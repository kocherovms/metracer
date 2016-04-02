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

package com.develorium.metracer.statik;

import java.util.*;
import java.lang.ref.*;
import java.lang.reflect.*;

class RuntimeLogger implements com.develorium.metracer.Runtime.LoggerInterface {
	private final Object NullLogger = new Object();
	private Map<String, WeakReference<ClassLoader>> classesWithLoggers = Collections.
		synchronizedMap(new HashMap<String, WeakReference<ClassLoader>>(1000));
	private Map<String, WeakReference<Object>> loggers = 
		Collections.synchronizedMap(new HashMap<String, WeakReference<Object>>(1000));

	@Override
	public void printMessage(Class<?> theClass, String theMethodName, String theMessage, List<StackTraceElement> theStackTraceElements) {
		Object logger = getAndResolveLogger(theClass);

		if(!isSlf4jLogger(logger)) 
			logger = findNearestSlf4jLogger();

		printMessageViaLogger(logger, theMessage);
	}

	public void registerClassWithSlf4jLogger(String theClassName, ClassLoader theClassLoader) {
		classesWithLoggers.put(theClassName, new WeakReference(theClassLoader));
		loggers.put(theClassName, null);
	}

	private Object getAndResolveLogger(Class theClass) {
		Object logger = getLogger(theClass.getName());

		if(logger == null) {
			// logger is not yet resolved, need to resolve it
			logger = resolveLogger(theClass);
			loggers.put(theClass.getName(), new WeakReference(logger));
		}

		return logger;
	}

	private Object getLogger(String theClassName) {
		WeakReference<Object> ref = loggers.get(theClassName);
		
		if(ref != null) 
			return ref.get();

		return null;
	}

	private Object resolveLogger(Class theClass) {
		Object logger = null;
		Field[] fields = theClass.getDeclaredFields();

		for(Field field: fields) {
			Class c = field.getType();
			
			if(c != null && Modifier.isStatic(field.getModifiers()) && c.getName().equals("org.slf4j.Logger")) {
				boolean isLoggerAccessible = field.isAccessible();
				try {
					field.setAccessible(true);
					try {
						logger = field.get(null);
					} catch(Exception e) {
						System.err.format("Failed to get value of a static field (logger) %1$s in class %2$s: %3$s\n", field.getName(), theClass.getName(), e.toString());
					}
				} finally { 
					field.setAccessible(isLoggerAccessible);
				}
			}
		}

		if(logger != null) 
			return logger;

		return NullLogger;
	}

	private boolean isSlf4jLogger(Object theLogger) {
		return theLogger != null && theLogger != NullLogger;
	}

	private Object findNearestSlf4jLogger() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		Object logger = null;

		for(StackTraceElement element : elements) {
			String className = element.getClassName();
			logger = getLogger(className);

			if(isSlf4jLogger(logger))
				return logger;

			WeakReference<ClassLoader> classLoaderRef = classesWithLoggers.get(className);

			if(classLoaderRef == null)
				continue;

			ClassLoader classLoader = classLoaderRef.get();

			if(classLoader == null) // class loader was GC'ed already
				continue;

			try {
				Field f = ClassLoader.class.getDeclaredField("classes");
				f.setAccessible(true);
				Vector<Class> classes = (Vector<Class>)f.get(classLoader);

				for(Class c : classes) {
					if(c == null || !c.getName().equals(className)) 
						continue;

					logger = resolveLogger(c);
					loggers.put(className, new WeakReference(logger)); // TODO: duplication

					if(isSlf4jLogger(logger)) {
						return logger;
					}

					break;
				}
			} catch(Exception e) {
				System.err.format("Failed to search for class %1$s within class loader %2$s: %3$s\n", className, classLoader.toString(), e.toString());
			}
		}
				
		return null;
	}
			
	private void printMessageViaLogger(Object theLogger, String theMessage) {
		if(isSlf4jLogger(theLogger)) {
			Method[] methods = theLogger.getClass().getDeclaredMethods();

			for(Method method: methods) {
				if(!method.getName().equals("info")) 
					continue;

				Class[] argumentTypes = method.getParameterTypes();

				if(argumentTypes == null || argumentTypes.length != 1)
					continue;

				if(!argumentTypes[0].equals(String.class))
					continue;

				try {
					method.invoke(theLogger, theMessage);
					return;
				} catch(Exception e) {
					System.err.format("Failed to invoke method %1$s over %2$s: %3$s\n", method.getName(), theLogger.toString(), e.toString());
				}
			}
		}

		System.out.println(theMessage);
	}
}