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
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import com.develorium.metracer.asm.*;

public class Agent extends NotificationBroadcasterSupport implements AgentMXBean, com.develorium.metracer.Runtime.LoggerInterface {
	public static final String MxBeanName = "com.develorium.metracer.dynamic:type=Agent";
	public static final String NotificationType = "com.develorium.metracer.traceevent";
	private Instrumentation instrumentation = null;
	private com.develorium.metracer.Runtime runtime = null;
	private AtomicInteger messageSerial = new AtomicInteger();
	private Pattern pattern = null;

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
		System.out.println("kms@ " + theClass + " " + theMethodName + " " + theMessage + " vs " + pattern);

		if(!com.develorium.metracer.Runtime.isMethodPatternMatched(theClass.getName(), theMethodName, pattern)) 
			return;

		Notification notification = new Notification(NotificationType, this, messageSerial.incrementAndGet(), theMessage);
		sendNotification(notification);
	}

	@Override
	public void setPattern(String thePattern) {
		System.out.println("kms@ pattern = " + thePattern);
		try {
            Pattern newPattern = Pattern.compile(thePattern);
			pattern = newPattern; // Reads and writes are atomic for reference variables (Java Language Specification)
        } catch(PatternSyntaxException e) {
            throw new RuntimeException(String.format("Provided pattern \"%s\" is malformed: %s", thePattern, e.toString()));
		}

		try {
			instrumentLoadedClasses();
		} catch(Exception e) {
			throw new RuntimeException(String.format("Failed to instrument loaded classes: %s", e.toString()));
		}
	}

	public Pattern getPattern() {
		return pattern;
	}

	private void bootstrap(String theArguments, Instrumentation theInstrumentation) {
		try {
			instrumentation = theInstrumentation;
			createRuntime();
			registerMxBean();
			instrumentation.addTransformer(new MetracerClassFileTransformer(this), true);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void createRuntime() {
		runtime = new com.develorium.metracer.Runtime(this);
	}

	private void registerMxBean() throws Exception {
		MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName beanName = new ObjectName(MxBeanName);
		beanServer.registerMBean(this, beanName);
	}

	synchronized private void instrumentLoadedClasses() throws UnmodifiableClassException {
		List<Class<?>> classesForInstrumentation = new ArrayList<Class<?>>();

		for(Class<?> c: instrumentation.getAllLoadedClasses()) {
			if(!instrumentation.isModifiableClass(c))
				continue;

			if(doesClassNeedInstrumentation(c))
				classesForInstrumentation.add(c);
		}

		if(classesForInstrumentation.isEmpty())
			return;

		Class<?>[] classesArray = classesForInstrumentation.toArray(new Class<?>[classesForInstrumentation.size()]);
		instrumentation.retransformClasses(classesArray);
	}

	private boolean doesClassNeedInstrumentation(Class<?> theClass) {
		Method[] methods = theClass.getDeclaredMethods();
			
		for(Method method: methods) {
			if(runtime.isMethodPatternMatched(theClass.getName(), method.getName(), pattern)) {
				return true;
			}
		}

		return false;
	}
}
