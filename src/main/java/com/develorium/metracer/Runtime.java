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

public class Runtime {
	static public boolean isVerbose = false;
	
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

	public static void traceEntry(Class theClass, String theMethodName, String[] theArgumentNames, Object[] theArgumentValues) {
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
		String message = String.format("[metracer.%s]%s +++ [%d] %s.%s(%s)", getFormattedThreadId(), getIndent(callDepth), callDepth, theClass.getName(), theMethodName, arguments.toString());

		if(logger != null) 
			logger.printMessage(theClass, theMethodName, message);
	}

	public static void traceExit(Class theClass, String theMethodName, Object theReturnValue) {
		Integer callDepth = TracingStateThreadLocal.instance.get();
		TracingStateThreadLocal.instance.set(callDepth - 1);
		String returnValueInfo = analyzeReturnValueInfo(theReturnValue);
		String message = String.format("[metracer.%s]%s --- [%d] %s.%s%s", getFormattedThreadId(), getIndent(callDepth), callDepth, theClass.getName(), theMethodName, returnValueInfo);

		if(logger != null) 
			logger.printMessage(theClass, theMethodName, message);
	}

	private static String formatArgumentValue(Object theArgumentValue) {
		if(theArgumentValue == null)
			return "null";

		if(theArgumentValue.getClass().isArray()) {
			return formatArrayArgumentValue(getArrayAdapter(theArgumentValue));
		}
		else if(theArgumentValue instanceof java.util.Collection<?>) {
			return formatCollectionArgumentValue((Collection<?>)theArgumentValue);
		}

		return theArgumentValue.toString();
	}

	private static abstract class IterableArgumentAdapter {
		public abstract boolean hasNext();
		public abstract String getNext();
	}

	private static abstract class ArrayAdapter {
		ArrayAdapter(int theSize) {
			size = theSize;
		}
		public int size = 0;
		public abstract String toString(int theIndex);
	}

	private static ArrayAdapter getArrayAdapter(final Object theArray) {
		if(theArray instanceof boolean[]) {
			return new ArrayAdapter(((boolean[])theArray).length) {
				public String toString(int theIndex) { return "" + ((boolean[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof byte[]) {
			return new ArrayAdapter(((byte[])theArray).length) {
				public String toString(int theIndex) { return "" + ((byte[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof char[]) {
			return new ArrayAdapter(((char[])theArray).length) {
				public String toString(int theIndex) { return "" + ((char[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof short[]) {
			return new ArrayAdapter(((short[])theArray).length) {
				public String toString(int theIndex) { return "" + ((short[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof int[]) {
			return new ArrayAdapter(((int[])theArray).length) {
				public String toString(int theIndex) { return "" + ((int[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof long[]) {
			return new ArrayAdapter(((long[])theArray).length) {
				public String toString(int theIndex) { return "" + ((long[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof float[]) {
			return new ArrayAdapter(((float[])theArray).length) {
				public String toString(int theIndex) { return "" + ((float[])theArray)[theIndex]; }
			};
		}
		else if(theArray instanceof double[]) {
			return new ArrayAdapter(((double[])theArray).length) {
				public String toString(int theIndex) { return "" + ((double[])theArray)[theIndex]; }
			};
		}

		return null;
	}

	private static String formatArrayArgumentValue(ArrayAdapter theAdapter) {
		if(theAdapter == null)
			return "null";

		StringBuilder rv = new StringBuilder();
		int arraySize = theAdapter.size;
		int maxI = Math.min(10, arraySize);
		
		for(int i = 0; i < maxI; ++i) {
			if(i > 0)
				rv.append(", ");
		
			rv.append(theAdapter.toString(i));
		}
		
		if(maxI < arraySize) {
			rv.append(", ...");
		}
		
		return "[" + rv.toString() + "]";
	}

	private static String formatCollectionArgumentValue(Collection<?> theCollection) {
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
