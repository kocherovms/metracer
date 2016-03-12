/*
 * Copyright 2015-2016 Michael Kocherov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.develorium.metracer.dynamic;

import java.io.*;
import java.lang.instrument.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import javax.management.*;

public class Agent extends NotificationBroadcasterSupport implements AgentMXBean, com.develorium.metracer.Runtime.LoggerInterface {
	public static final String MxBeanName = "com.develorium.metracer.dynamic:type=Agent";
	public static final String NotificationType = "com.develorium.metracer.traceevent";
	private Instrumentation instrumentation = null;
	private com.develorium.metracer.Runtime runtime = null;
	private AtomicInteger messageSerial = new AtomicInteger();
	// Two patterns are used because a single methodMatchingPattern can't be used to qualify classes. This pattern
	// may reference methods which we can't get during setPatterms method. This is because
	// theClass.getDeclaredMethods may lead to an overwhelming class loading
	// (e.g. of classes mentioned in return parameters and arguments). Beside huge I/O this could lead to
	// numerous LinkageError / NoClassDefFoundErrors due to class loaders isolation, different versioning or
	// multiple instances of the same class in different class loaders.
	// An approach could be to get bytecode of the class via a ClasseNode of ASM
	// but this again tends to be an overkill within some JavaEE application - too many classes, analysis would take significiant time
	// Hence the solution is to use a classMatchingPattern for filtering classes which require instrumentation and
	// an optional methodMatchingPattern for a fine-grained control of which methods must be instrumented
	public static class Patterns {
		public Pattern classMatchingPattern = null;
		public Pattern methodMatchingPattern = null;
	};
	Patterns patterns = null;

	public static void agentmain(String theArguments, Instrumentation theInstrumentation) {
		new Agent().bootstrap(theArguments, theInstrumentation);
	}

	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		MBeanNotificationInfo info = new MBeanNotificationInfo(
			new String[] { NotificationType }, 
			Notification.class.getName(), 
			"Notification about metracer event (entry / exit of methods)");
		return new MBeanNotificationInfo[] { info };
	}

	@Override
	public void printMessage(Class<?> theClass, String theMethodName, String theMessage) {
		Patterns p = patterns;

		if(p == null || p.classMatchingPattern == null) 
			return;
		else if(!p.classMatchingPattern.matcher(theClass.getName()).find())
			return;
		else if(p.methodMatchingPattern != null && !com.develorium.metracer.Runtime.isMethodPatternMatched(theClass.getName(), theMethodName, p.methodMatchingPattern)) 
			return;

		Notification notification = new Notification(NotificationType, this, messageSerial.incrementAndGet(), theMessage);
		sendNotification(notification);
	}

	@Override
	public void setIsVerbose(boolean theIsVerbose) {
		com.develorium.metracer.Runtime.isVerbose = theIsVerbose;
	}

	@Override
	public void setPatterns(String theClassMatchingPattern, String theMethodMatchingPattern) {
		if(theClassMatchingPattern == null)
			throw new NullPointerException("Class matching pattern is null");

		runtime.say(String.format("Going to set patterns: class matching pattern = %s%s", 
				theClassMatchingPattern, 
				theMethodMatchingPattern != null ? String.format(", method matching pattern = %s", theMethodMatchingPattern) : ""));
		// Reads and writes are atomic for reference variables (Java Language Specification), 
		// so it's not required to use syncrhonized when changing patterns
		patterns = null; // to avoid previous patterns to be effective in case of error
		Patterns newPatterns = createPatterns(theClassMatchingPattern, theMethodMatchingPattern);
		patterns = newPatterns;

		try {
			// newPatterns is used here to avoid possible races when setPatterns is called from two different sessions
			instrumentLoadedClasses(newPatterns);
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to instrument loaded classes: %s", e.getMessage()), e);
		}
	}

	public Patterns getPatterns() {
		return patterns;
	}

	private void bootstrap(String theArguments, Instrumentation theInstrumentation) {
		ClassFileTransformer transformer = null;

		try {
			instrumentation = theInstrumentation;
			createRuntime(theArguments);
			runtime.say("Runtime created");
			registerMxBean();
			runtime.say("MX bean registered");
			transformer = new MetracerClassFileTransformer(this);
			instrumentation.addTransformer(transformer, true);
			runtime.say("Class file transformer added");
		} catch(Throwable e) {
			if(transformer != null) {
				instrumentation.removeTransformer(transformer);
				runtime.say("Class file transformer removed");
			}
			
			unregisterMxBean();
			runtime.say("MX bean unregistered");
			throw new RuntimeException(String.format("Failed to bootstrap agent: %s", e.getMessage()), e);
		}
	}

	private void createRuntime(String theArguments) {
		runtime = new com.develorium.metracer.Runtime(this);
		runtime.isVerbose = theArguments != null && theArguments.contains("-v");
	}

	private void registerMxBean() throws Exception {
		MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName beanName = new ObjectName(MxBeanName);
		beanServer.registerMBean(this, beanName);
	}

	private void unregisterMxBean() {
		try {
			MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
			ObjectName beanName = new ObjectName(MxBeanName);
			beanServer.unregisterMBean(beanName);
		} catch(Throwable e) {
			runtime.say(String.format("Failed to unregister MX bean: %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	private static Patterns createPatterns(String theClassMatchingPattern, String theMethodMatchingPattern) {
		Patterns rv = new Patterns();
		rv.classMatchingPattern = createPattern(theClassMatchingPattern);
		rv.methodMatchingPattern = createPattern(theMethodMatchingPattern);
		return rv;
	}

	private static Pattern createPattern(String thePatternSource) {
		if(thePatternSource == null) 
			return null;

		try {
			return Pattern.compile(thePatternSource);
		} catch(PatternSyntaxException e) {
			throw new RuntimeException(String.format("Provided pattern \"%s\" is malformed: %s", thePatternSource, e.getMessage()), e);
		}
	}

	private void instrumentLoadedClasses(Patterns theNewPatterns) {
		final Pattern classMatchingPattern = theNewPatterns.classMatchingPattern;
		List<Class<?>> classesForInstrumentation = new ArrayList<Class<?>>();

		for(Class<?> c: instrumentation.getAllLoadedClasses()) {
			if(!instrumentation.isModifiableClass(c))
				continue;

			String className = c.getName();

			if(!className.startsWith("com.develorium.metracer.") && classMatchingPattern.matcher(className).find()) {
				runtime.say(String.format("Going to instrument %s", className));
				classesForInstrumentation.add(c);
			}
		}

		if(classesForInstrumentation.isEmpty())
			return;

		if(!restransformLoadedClassesBatch(classesForInstrumentation))
			restransformLoadedClassesByOne(classesForInstrumentation);
	}

	private boolean restransformLoadedClassesBatch(List<Class<?>> theClassesForInstrumentation) {
		try {
			Class<?>[] classesArray = theClassesForInstrumentation.toArray(new Class<?>[theClassesForInstrumentation.size()]);
			instrumentation.retransformClasses(classesArray);
			return true;
		} catch(Throwable e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			runtime.say(String.format("Failed to restransform classes in a batch mode: %s\n%s", e.toString(), sw.toString()));
		}

		return false;
	}

	private void restransformLoadedClassesByOne(List<Class<?>> theClassesForInstrumentation) {
		for(Class<?> c : theClassesForInstrumentation) {
			try {
				Class<?>[] classesArray = new Class<?>[] { c };
				instrumentation.retransformClasses(classesArray);
			} catch(Throwable e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				runtime.say(String.format("Failed to retransform class \"%s\": %s %s\n%s", c.getName(), e.toString(), e.getMessage(), sw.toString()));
			}
		}
	}
}
