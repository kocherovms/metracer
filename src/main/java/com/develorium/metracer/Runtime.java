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
import java.util.regex.*;
import java.io.*;

public class Runtime {
	static public boolean isVerbose = false;
	static public int MaxElementsForPrinting = 10;
	
	public interface LoggerInterface {
		public void printMessage(Class<?> theClass, String theMethodName, String theMessage);
	}

	private static LoggerInterface logger = null;

	public Runtime(LoggerInterface theLogger) {
		logger = theLogger;
		new TracingStateThreadLocal(); // to trigger static initializer
	}

	public static boolean isClassPatternMatched(String theClassName, Pattern thePattern) {
		return !theClassName.startsWith("com.develorium.metracer.") && thePattern.matcher(theClassName).find();
	}

	public static boolean isMethodPatternMatched(String theClassName, String theMethodName, Pattern thePattern) {
		String methodNameForPatternMatching = String.format("%s::%s", theClassName, theMethodName);
		return thePattern.matcher(methodNameForPatternMatching).find();
	}

	public static void say(String theMessage) {
		if(!isVerbose)
			return;

		System.out.println("[metracer] " + theMessage);
	}

	private static class TracingStateThreadLocal extends ThreadLocal<Integer> {
		@Override 
		protected Integer initialValue() {
			return new Integer(-1);
		}
		public static final TracingStateThreadLocal instance = new TracingStateThreadLocal();
	}

	public static void traceEntry(Class theClass, String theMethodName, String[] theArgumentNames, Object[] theArgumentValues, boolean theIsWithStackTraces) {
		StringBuilder arguments = new StringBuilder();

		if(theArgumentValues != null) {
			for(int i = 0; i < theArgumentValues.length; ++i) {
				String argumentName = theArgumentNames != null && i < theArgumentNames.length && theArgumentNames[i] != null ? theArgumentNames[i] : "<unk>";
				String argumentValue = formatArgumentValue(theArgumentValues[i]);
				
				if(arguments.length() > 0)
					arguments.append(", ");

				arguments.append(String.format("%s = %s", argumentName, argumentValue));
			}
		}

		Integer callDepth = TracingStateThreadLocal.instance.get() + 1;
		TracingStateThreadLocal.instance.set(callDepth);
		String messagePrefix = String.format("[metracer.%s]%s +++ [%d] ", getFormattedThreadId(), getIndent(callDepth), callDepth);
		String message = String.format("%s%s.%s(%s)", messagePrefix, theClass.getName(), theMethodName, arguments.toString());

		if(logger != null) {
			logger.printMessage(theClass, theMethodName, message);

			if(theIsWithStackTraces) {
				StringWriter sw = new StringWriter();
				new Throwable().printStackTrace(new PrintWriter(sw));
				String lines[] = sw.toString().split("\\r?\\n");

				// skip first and last lines
				for(int i = 1; i < lines.length - 1; ++i) {
					logger.printMessage(theClass, theMethodName, messagePrefix + lines[i]);
				}
			}
		}
	}

	public static void traceExit(Class theClass, String theMethodName, Object theReturnValue) {
		Integer callDepth = TracingStateThreadLocal.instance.get();
		TracingStateThreadLocal.instance.set(callDepth - 1);
		String returnValueInfo = analyzeReturnValueInfo(theReturnValue);
		String message = String.format("[metracer.%s]%s --- [%d] %s.%s%s", getFormattedThreadId(), getIndent(callDepth), callDepth, theClass.getName(), theMethodName, returnValueInfo);

		if(logger != null) 
			logger.printMessage(theClass, theMethodName, message);
	}

	public static String formatArgumentValue(Object theArgumentValue) {
		if(theArgumentValue == null)
			return "null";

		if(theArgumentValue.getClass().isArray()) {
			return formatIterableArgumentValue(getArrayArgumentAdapter(theArgumentValue));
		}
		else if(theArgumentValue instanceof java.util.Collection<?>) {
			return formatIterableArgumentValue(getCollectionArgumentAdapter((Collection<?>)theArgumentValue));
		}
		else if(theArgumentValue instanceof java.util.Map<?, ?>) {
			return formatIterableArgumentValue(getMapArgumentAdapter((Map<?, ?>)theArgumentValue));
		}

		return theArgumentValue.toString();
	}

	private static abstract class IterableArgumentAdapter {
		public abstract boolean hasNext();
		public abstract String getNext();
	}

	private static abstract class ArrayArgumentAdapter extends IterableArgumentAdapter {
		ArrayArgumentAdapter(int theSize) {
			size = theSize;
		}
		public int i = 0;
		public int size = 0;
		public boolean hasNext() {  return i < size; }
		public String getNext() { return toString(i++); }
		public abstract String toString(int theIndex);
	}

	private static IterableArgumentAdapter getArrayArgumentAdapter(final Object theArray) {
		if(theArray instanceof boolean[]) {
			return new ArrayArgumentAdapter(((boolean[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((boolean[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof byte[]) {
			return new ArrayArgumentAdapter(((byte[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((byte[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof char[]) {
			return new ArrayArgumentAdapter(((char[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((char[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof short[]) {
			return new ArrayArgumentAdapter(((short[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((short[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof int[]) {
			return new ArrayArgumentAdapter(((int[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((int[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof long[]) {
			return new ArrayArgumentAdapter(((long[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((long[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof float[]) {
			return new ArrayArgumentAdapter(((float[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((float[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof double[]) {
			return new ArrayArgumentAdapter(((double[])theArray).length) {
				public String toString(int theIndex) { 
					return "" + ((double[])theArray)[theIndex]; 
				}
			};
		}
		else if(theArray instanceof Object[]) {
			return new ArrayArgumentAdapter(((Object[])theArray).length) {
				public String toString(int theIndex) { 
					Object o = ((Object[])theArray)[theIndex];
					return o != null ? o.toString() : "null";
				}
			};
		}

		return null;
	}

	private static IterableArgumentAdapter getCollectionArgumentAdapter(final Collection<?> theCollection) {
		return new IterableArgumentAdapter() {
			Iterator<?> it = theCollection.iterator();
			public boolean hasNext() { 
				return it.hasNext(); 
			}
			public String getNext() { 
				Object x = it.next();
				return x != null ? x.toString() : "null"; 
			}
		};
	}

	private static IterableArgumentAdapter getMapArgumentAdapter(final Map<?, ?> theMap) {
		return new IterableArgumentAdapter() {
			Iterator<? extends Map.Entry<?, ?>> it = theMap.entrySet().iterator();
			public boolean hasNext() { 
				return it.hasNext(); 
			}
			public String getNext() { 
				Map.Entry<?, ?> entry = it.next();
				Object k = entry.getKey();
				Object v = entry.getValue();
				return String.format("%s => %s", k != null ? k.toString() : "null", v != null ? v.toString() : "null" );
			}
		};
	}

	static String formatIterableArgumentValue(IterableArgumentAdapter theAdapter) {
		if(theAdapter == null)
			return "null";

		StringBuilder rv = new StringBuilder();
		
		for(int i = 0; i < MaxElementsForPrinting && theAdapter.hasNext(); ++i) {
			if(i > 0)
				rv.append(", ");
		
			rv.append(theAdapter.getNext());
		}
		
		if(theAdapter.hasNext()) {
			rv.append(", ...");
		}
		
		return "[" + rv.toString() + "]";
	}

	private static String analyzeReturnValueInfo(Object theReturnValue) {
		if(theReturnValue == null)
			return " => void";
		else if(theReturnValue instanceof Throwable)
			return String.format(" => exception: %s", theReturnValue.toString());
		else 
			return String.format(" => return: %s", theReturnValue.toString());
	}

	private static String getFormattedThreadId() {
		long threadId = Thread.currentThread().getId();
		return threadId <= Integer.MAX_VALUE && threadId >= Integer.MIN_VALUE
			? String.format("%08X", threadId)
			: String.format("%016X", threadId);
	}
	
	private static String getIndent(int theCallDepth) {
		return theCallDepth > 0 ? String.format("%" + Math.min(32, theCallDepth) + "s", "") : "";
	}
}
