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

package com.develorium.metracer;

import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

public class Runtime {
	private static Map<Class, Object> loggers = Collections.synchronizedMap(new WeakHashMap<Class, Object>(1000));
	private static final Object NullLogger = new Object();

	public Runtime() {
		
	}

	private static class TracingStateThreadLocal extends ThreadLocal<Integer> {
		@Override 
		protected Integer initialValue() {
			return new Integer(-1);
		}
		public static final TracingStateThreadLocal instance = new TracingStateThreadLocal();
	}

	public static void traceEntry(Class theClass, String theMethodName, String[] theArgumentNames, Object[] theArgumentValues) {
		StringBuilder arguments = new StringBuilder();

		if(theArgumentValues != null) {
			for(int i = 0; i < theArgumentValues.length; ++i) {
				String argumentName = theArgumentNames != null && i < theArgumentNames.length && theArgumentNames[i] != null ? theArgumentNames[i] : "<unk>";
				String argumentValue = theArgumentValues[i] != null ? theArgumentValues[i].toString() : "null";
				
				if(arguments.length() > 0)
					arguments.append(", ");

				arguments.append(String.format("%1$s = %2$s", argumentName, argumentValue));
			}
		}

		Integer callDepth = TracingStateThreadLocal.instance.get() + 1;
		TracingStateThreadLocal.instance.set(callDepth);
		String message = String.format("%1$s +++ [%2$d] %3$s.%4$s(%5$s)", getIndent(callDepth), callDepth, theClass.getName(), theMethodName, arguments.toString());
		printMessage(theClass, message);
	}

	public static void traceExit(Class theClass, String theMethodName, boolean theIsFinishedOk) {
		Integer callDepth = TracingStateThreadLocal.instance.get();
		TracingStateThreadLocal.instance.set(callDepth - 1);
		String exceptionInfo = theIsFinishedOk ? "" : " (by exception)";
		String message = String.format("%1$s --- [%2$d] %3$s.%4$s%5$s", getIndent(callDepth), callDepth, theClass.getName(), theMethodName, exceptionInfo);
		printMessage(theClass, message);
	}

	private static String getIndent(int theCallDepth) {
		return theCallDepth > 0 ? String.format("%" + Math.min(32, theCallDepth) + "s", "") : "";
	}

	private static void printMessage(Class theClass, String theMessage) {
		Object logger = getLogger(theClass);

		if(logger != null && logger != NullLogger) {
			Method[] methods = logger.getClass().getDeclaredMethods();

			for(Method method: methods) {
				if(method.getName().equals("info")) {
					try {
						method.invoke(logger, theMessage);
						return;
					} catch(Exception e) {
						System.err.format("Failed to invoke method %1$s over %2$s: %3$s\n", method.getName(), logger.toString(), e.toString());
					}
				}
			}
		}

		System.out.println(theMessage);
	}

	private static Object getLogger(Class theClass) {
		Object rv = loggers.get(theClass);
		
		if(rv == null) {
			// need to identify logger
			Field[] fields = theClass.getDeclaredFields();

			for(Field field: fields) {
				Class c = field.getType();
			
				if(c != null && Modifier.isStatic(field.getModifiers()) && c.getName().equals("org.slf4j.Logger")) {
					boolean isLoggerAccessible = field.isAccessible();
					try {
						field.setAccessible(true);
						try {
							rv = field.get(null);
						} catch(Exception e) {
							System.err.format("Failed to get value of a static field (logger) %1$s in class %2$s: %3$s\n", field.getName(), theClass.getName(), e.toString());
						}
					} finally { 
						field.setAccessible(isLoggerAccessible);
					}
				}
			}

			if(rv == null) 
				rv = NullLogger;

			loggers.put(theClass, rv);
		}

		return rv;
	}
}
