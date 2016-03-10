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
				String argumentValue = theArgumentValues[i] != null ? theArgumentValues[i].toString() : "null";
				
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

	public static void traceExit(Object theReturnValue, Class theClass, String theMethodName) {
		Integer callDepth = TracingStateThreadLocal.instance.get();
		TracingStateThreadLocal.instance.set(callDepth - 1);
		String returnValueInfo = analyzeReturnValueInfo(theReturnValue);
		String message = String.format("[metracer.%s]%s --- [%d] %s.%s%s", getFormattedThreadId(), getIndent(callDepth), callDepth, theClass.getName(), theMethodName, returnValueInfo);

		if(logger != null) 
			logger.printMessage(theClass, theMethodName, message);
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
