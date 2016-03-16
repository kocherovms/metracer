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
import java.text.*;
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
		public boolean isWithStackTraces = false;

		@Override
		public boolean equals(Object theOther) {
			if(theOther == null)
				return false;
			else if(this == theOther)
				return true;
			else if(theOther instanceof Patterns) {
				Patterns other = (Patterns)theOther;
				return 
					arePatternsEqual(classMatchingPattern, other.classMatchingPattern) && 
					arePatternsEqual(methodMatchingPattern, other.methodMatchingPattern) &&
					isWithStackTraces == other.isWithStackTraces;
			}

			return false;
		}

		private static boolean arePatternsEqual(Pattern theLeft, Pattern theRight) {
			if(theLeft != null && theRight != null)
				return theLeft.toString().equals(theRight.toString());
			else
				return (theLeft != null) == (theRight != null);
		}
	};
	private Patterns patterns = null;
	private List<Patterns> historyPatterns = new LinkedList<Patterns>();
	private SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss.SSS");

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

		String messageWithTimestamp = timestampFormat.format(new Date()) + " " + theMessage;
		Notification notification = new Notification(NotificationType, this, messageSerial.incrementAndGet(), messageWithTimestamp);
		sendNotification(notification);
	}

	@Override
	public void setIsVerbose(boolean theIsVerbose) {
		com.develorium.metracer.Runtime.isVerbose = theIsVerbose;
	}

	@Override
	synchronized public int[] setPatterns(String theClassMatchingPattern, String theMethodMatchingPattern, boolean theIsWithStackTraces) {
		if(theClassMatchingPattern == null)
			throw new NullPointerException("Class matching pattern is null");

		Patterns newPatterns = createPatterns(theClassMatchingPattern, theMethodMatchingPattern, theIsWithStackTraces);

		if(patterns != null && patterns.equals(newPatterns)) {
			runtime.say(String.format("Patterns already set to: class matching pattern = %s%s, stack traces = %s", 
					theClassMatchingPattern, 
					theMethodMatchingPattern != null ? String.format(", method matching pattern = %s", theMethodMatchingPattern) : "",
					"" + theIsWithStackTraces));
			return null;
		}

		runtime.say(String.format("Setting patterns: class matching pattern = %s%s, stack traces = %s",
				theClassMatchingPattern, 
				theMethodMatchingPattern != null ? String.format(", method matching pattern = %s", theMethodMatchingPattern) : "",
				"" + theIsWithStackTraces));

		if(patterns != null)
			historyPatterns.add(patterns);

		patterns = newPatterns;

		try {
			int[] counters = instrumentLoadedClasses(Arrays.asList(patterns), "instrument");
			runtime.say(String.format("Loaded classes instrumented: %d ok, %d failed", counters[0], counters[1]));
			return counters;
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to instrument loaded classes: %s", e.getMessage()), e);
		}
	}

	@Override
	synchronized public int[] removePatterns() {
		runtime.say("Removing patterns");

		if(patterns == null && historyPatterns.isEmpty()) {
			runtime.say("There are no active patterns, nothing to remove");
			return null;
		}
		else if(patterns != null) {
			historyPatterns.add(patterns);
			patterns = null;
		}
		
		try {
			try {
				int[] counters = instrumentLoadedClasses(historyPatterns, "deinstrument");
				runtime.say(String.format("Loaded classes deinstrumented: %d ok, %d failed", counters[0], counters[1]));
				return counters;
			} catch(Throwable e) {
				throw new RuntimeException(String.format("Failed to deinstrument loaded classes: %s", e.getMessage()), e);
			}
		} finally {
			historyPatterns.clear();
		}
	}

	synchronized public Patterns getPatterns() {
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

	static Patterns createPatterns(String theClassMatchingPattern, String theMethodMatchingPattern, boolean theIsWithStackTraces) {
		Patterns rv = new Patterns();
		rv.classMatchingPattern = createPattern(theClassMatchingPattern);
		rv.methodMatchingPattern = createPattern(theMethodMatchingPattern);
		rv.isWithStackTraces = theIsWithStackTraces;
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

	private int[] instrumentLoadedClasses(List<Patterns> thePatternsList, String theVerb) {
		List<Class<?>> classesForInstrumentation = new ArrayList<Class<?>>();

		for(Class<?> c: instrumentation.getAllLoadedClasses()) {
			if(!instrumentation.isModifiableClass(c))
				continue;

			String className = c.getName();

			if(className.startsWith("com.develorium.metracer."))
				continue;

			for(Patterns p: thePatternsList) {
				if(p.classMatchingPattern.matcher(className).find()) {
					runtime.say(String.format("Going to %s %s", theVerb, className));
					classesForInstrumentation.add(c);
					break;
				}
			}
		}

		int[] rv = new int[2];

		if(classesForInstrumentation.isEmpty())
			return rv;

		if(!restransformLoadedClassesBatch(classesForInstrumentation, rv))
			restransformLoadedClassesByOne(classesForInstrumentation, rv);

		return rv;
	}

	private boolean restransformLoadedClassesBatch(List<Class<?>> theClassesForInstrumentation, int[] theCounters) {
		try {
			Class<?>[] classesArray = theClassesForInstrumentation.toArray(new Class<?>[theClassesForInstrumentation.size()]);
			instrumentation.retransformClasses(classesArray);
			theCounters[0] = theClassesForInstrumentation.size();
			theCounters[1] = 0;
			return true;
		} catch(Throwable e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			runtime.say(String.format("Failed to restransform classes in a batch mode: %s\n%s", e.toString(), sw.toString()));
		}

		theCounters[0] = 0;
		theCounters[1] = theClassesForInstrumentation.size();
		return false;
	}

	private void restransformLoadedClassesByOne(List<Class<?>> theClassesForInstrumentation, int[] theCounters) {
		for(Class<?> c : theClassesForInstrumentation) {
			try {
				Class<?>[] classesArray = new Class<?>[] { c };
				instrumentation.retransformClasses(classesArray);
				theCounters[0]++;
			} catch(Throwable e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				runtime.say(String.format("Failed to retransform class \"%s\": %s %s\n%s", c.getName(), e.toString(), e.getMessage(), sw.toString()));
			}
		}

		theCounters[1] = theClassesForInstrumentation.size() - theCounters[0];
	}
}
