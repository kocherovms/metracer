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
import com.develorium.metracer.*;

public class Agent extends NotificationBroadcasterSupport implements AgentMXBean, com.develorium.metracer.Runtime.LoggerInterface {
	public static final String MxBeanName = "com.develorium.metracer.dynamic:type=Agent";
	public static final String NotificationType = "com.develorium.metracer.traceevent";
	private Instrumentation instrumentation = null;
	private com.develorium.metracer.Runtime runtime = null;
	private AtomicInteger messageSerial = new AtomicInteger();
	private volatile Patterns patterns = null;
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

		if(p == null || !p.isPatternMatched(theClass.getName(), theMethodName)) 
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
	synchronized public byte[] setPatterns(String theClassMatchingPattern, String theMethodMatchingPattern, boolean theIsWithStackTraces) {
		if(theClassMatchingPattern == null)
			throw new NullPointerException("Class matching pattern is null");

		Patterns newPatterns = new Patterns(theClassMatchingPattern, theMethodMatchingPattern, theIsWithStackTraces);

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
			RestransformLoadedClassesResult retransformResult = restransformLoadedClasses(Arrays.asList(patterns), "instrument");
			Counters counters = new Counters();
			counters.methodsCount = patterns.getInstrumentedMethodsCount();
			counters.classesCount = retransformResult.retransformedClasses.size();
			counters.failedClassesCount = retransformResult.notRetransformedClasses.size();
			sayCounters(counters, "instrument");
			return counters.serialize();
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to instrument loaded classes: %s", e.getMessage()), e);
		}
	}

	@Override
	synchronized public byte[] removePatterns() {
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
				RestransformLoadedClassesResult retransformResult = restransformLoadedClasses(historyPatterns, "deinstrument");
				Counters counters = new Counters();
				counters.methodsCount = Patterns.getDeinstrumentedMethodsCount(historyPatterns, retransformResult.retransformedClasses);
				counters.classesCount = retransformResult.retransformedClasses.size();
				counters.failedClassesCount = retransformResult.notRetransformedClasses.size();
				sayCounters(counters, "deinstrument");
				return counters.serialize();
			} catch(Throwable e) {
				throw new RuntimeException(String.format("Failed to deinstrument loaded classes: %s", e.getMessage()), e);
			}
		} finally {
			historyPatterns.clear();
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

	private static class RestransformLoadedClassesResult {
		List<Class<?>> retransformedClasses = new ArrayList<Class<?>>();
		List<Class<?>> notRetransformedClasses = new ArrayList<Class<?>>();
	}

	private RestransformLoadedClassesResult restransformLoadedClasses(List<Patterns> thePatternsList, String theVerb) {
		List<Class<?>> classesForInstrumentation = new ArrayList<Class<?>>();

		for(Class<?> c: instrumentation.getAllLoadedClasses()) {
			if(!instrumentation.isModifiableClass(c))
				continue;

			String className = c.getName();

			for(Patterns p: thePatternsList) {
				if(p.isClassPatternMatched(className)) {
					runtime.say(String.format("Going to %s %s", theVerb, className));
					classesForInstrumentation.add(c);
					break;
				}
			}
		}

		if(classesForInstrumentation.isEmpty()) {
			return new RestransformLoadedClassesResult();
		}

		try {
			return restransformLoadedClassesInBatch(classesForInstrumentation);
		} catch(Throwable e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			runtime.say(String.format("Failed to restransform classes in a batch mode: %s\n%s", e.toString(), sw.toString()));
		}

		return restransformLoadedClassesByOne(classesForInstrumentation);
	}

	private RestransformLoadedClassesResult restransformLoadedClassesInBatch(List<Class<?>> theClassesForInstrumentation) throws UnmodifiableClassException {
		RestransformLoadedClassesResult rv = new RestransformLoadedClassesResult();
		Class<?>[] classesArray = theClassesForInstrumentation.toArray(new Class<?>[theClassesForInstrumentation.size()]);
		instrumentation.retransformClasses(classesArray);
		rv.retransformedClasses = theClassesForInstrumentation;
		return rv;
	}

	private RestransformLoadedClassesResult restransformLoadedClassesByOne(List<Class<?>> theClassesForInstrumentation) {
		RestransformLoadedClassesResult rv = new RestransformLoadedClassesResult();

		for(Class<?> c : theClassesForInstrumentation) {
			try {
				Class<?>[] classesArray = new Class<?>[] { c };
				instrumentation.retransformClasses(classesArray);
				rv.retransformedClasses.add(c);
			} catch(Throwable e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				runtime.say(String.format("Failed to retransform class %s: %s %s\n%s", c.getName(), e.toString(), e.getMessage(), sw.toString()));
				rv.notRetransformedClasses.add(c);
			}
		}

		return rv;
	}

	private void sayCounters(Counters theCounters, String theVerb) {
		String failMessage = theCounters.failedClassesCount > 0 
			? String.format(", %sation failed for %d classes", theVerb, theCounters.failedClassesCount)
			: "";
		runtime.say(String.format("%d methods in %d classes %sed%s", 
				theCounters.methodsCount,
				theCounters.classesCount,
				theVerb,
				failMessage));
	}
}
