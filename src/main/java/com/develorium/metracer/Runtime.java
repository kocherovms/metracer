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
	
	public interface LoggerInterface {
		public void printMessage(Class<?> theClass, String theMethodName, String theMessage, List<StackTraceElement> theStackTraceElements);
	}

	private static LoggerInterface logger = null;

	public Runtime(LoggerInterface theLogger) {
		logger = theLogger;
		new TracingStateThreadLocal(); // to trigger static initializer
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
			StackTraceElement[] stackTraceElements = theIsWithStackTraces ? Thread.currentThread().getStackTrace() : null;
			List<StackTraceElement> prunedStackTraceElements = null;
			
			if(stackTraceElements != null) {
				for(StackTraceElement element : stackTraceElements) {
					if(element.getClassName().equals(theClass.getName()) && element.getMethodName().equals(theMethodName))
						prunedStackTraceElements = new LinkedList<StackTraceElement>();

					if(prunedStackTraceElements != null)
						prunedStackTraceElements.add(element);
				}
			}

			logger.printMessage(theClass, theMethodName, message, prunedStackTraceElements);

			if(prunedStackTraceElements != null) {
				for(StackTraceElement element : prunedStackTraceElements) {
					logger.printMessage(theClass, theMethodName, String.format("%s    at %s", messagePrefix, element.toString()), null);
				}
			}
		}
	}

	public static void traceExit(Class theClass, String theMethodName, boolean theIsVoid, Object theReturnValue) {
		Integer callDepth = TracingStateThreadLocal.instance.get();
		TracingStateThreadLocal.instance.set(callDepth - 1);
		String returnValueInfo = formatReturnValue(theIsVoid, theReturnValue);
		String message = String.format("[metracer.%s]%s --- [%d] %s.%s%s", getFormattedThreadId(), getIndent(callDepth), callDepth, theClass.getName(), theMethodName, returnValueInfo);

		if(logger != null) 
			logger.printMessage(theClass, theMethodName, message, null);
	}

	public static String formatArgumentValue(Object theArgumentValue) {
		if(theArgumentValue == null)
			return "null";

		return new ObjectDumper().dumpObject(theArgumentValue);
	}

	static String formatReturnValue(boolean theIsVoid, Object theReturnValue) {
		if(theIsVoid)
			return " => void";
		else if(theReturnValue instanceof Throwable)
			return String.format(" => exception: %s", theReturnValue.toString());
		else 
			return String.format(" => return: %s", formatArgumentValue(theReturnValue));
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
