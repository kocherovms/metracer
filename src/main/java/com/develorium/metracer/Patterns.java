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
public class Patterns {
	public static List<String> BlacklistedClassNamePrefixes = new ArrayList<String>();
	private Pattern classMatchingPattern = null;
	private Pattern methodMatchingPattern = null;
	private StackTraceMode stackTraceMode = StackTraceMode.DISABLED;
	private Set<String> instrumentedMethods = Collections.synchronizedSet(new HashSet<String>(1000));

	static {
		BlacklistedClassNamePrefixes.add("com.develorium.metracer.");
	}

	public Patterns(String theClassMatchingPattern, String theMethodMatchingPattern) {
		this(theClassMatchingPattern, theMethodMatchingPattern, StackTraceMode.DISABLED);
	}

	public Patterns(String theClassMatchingPattern, String theMethodMatchingPattern, StackTraceMode theStackTraceMode) {
		classMatchingPattern = createPattern(theClassMatchingPattern);

		if(classMatchingPattern == null)
			throw new NullPointerException("Class matching pattern is null");

		methodMatchingPattern = createPattern(theMethodMatchingPattern);
		stackTraceMode = theStackTraceMode;
	}

	public Pattern getClassMatchingPattern() {
		return classMatchingPattern;
	}

	public Pattern getMethodMatchingPattern() {
		return methodMatchingPattern;
	}

	public StackTraceMode getStackTraceMode() {
		return stackTraceMode;
	}

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
				stackTraceMode.equals(other.stackTraceMode);
		}

		return false;
	}

	public boolean isClassPatternMatched(String theClassName) {
		if(theClassName == null)
			return false;

		for(String blacklistedClassNamePrefix : BlacklistedClassNamePrefixes)
			if(theClassName.startsWith(blacklistedClassNamePrefix))
				return false;
		
		return classMatchingPattern.matcher(theClassName).find();
	}

	public boolean isPatternMatched(String theClassName, String theMethodName) {
		if(theClassName == null || theMethodName == null)
            return false;

		if(!isClassPatternMatched(theClassName))
			return false;
		else if(methodMatchingPattern != null) {
			String methodNameForPatternMatching = String.format("%s::%s", theClassName, theMethodName);
			return methodMatchingPattern.matcher(methodNameForPatternMatching).find();
		}

		return true;
	}

	public void registerInstrumentedMethod(ClassLoader theLoader, String theClassName, String theMethodName) {
		if(theClassName == null || theMethodName == null)
			return;
		else if(theClassName.length() == 0 || theMethodName.length() == 0)
			return;

		Key key = new Key();
		key.setClassId(formatClassId(theLoader, theClassName));
		key.setMethodName(theMethodName);
		instrumentedMethods.add(encodeKey(key));
	}
	
	public int getInstrumentedMethodsCount() {
		return instrumentedMethods.size();
	}

	public static int getDeinstrumentedMethodsCount(List<Patterns> theHistoryPatterns, List<Class<?>> theDeinstrumentedClasses) {
		if(theHistoryPatterns == null || theDeinstrumentedClasses == null)
			return 0;

		Set<String> mergedInstrumentedMethods = new HashSet<String>();

		for(Patterns p : theHistoryPatterns) 
			mergedInstrumentedMethods.addAll(p.instrumentedMethods);

		Set<String> deinstrumentedClassIds = new HashSet<String>(theDeinstrumentedClasses.size());

		for(Class<?> c : theDeinstrumentedClasses)
			deinstrumentedClassIds.add(formatClassId(c.getClassLoader(), c.getName()));

		int rv = 0;

		for(String encodedKey : mergedInstrumentedMethods) {
			Key key = decodeKey(encodedKey);

			if(deinstrumentedClassIds.contains(key.classId)) {
				// method is removed along with its class
				++rv; 
			}
		}

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

	private static boolean arePatternsEqual(Pattern theLeft, Pattern theRight) {
		if(theLeft != null && theRight != null)
			return theLeft.toString().equals(theRight.toString());
		else
			return (theLeft != null) == (theRight != null);
	}

	private static String formatClassId(ClassLoader theLoader, String theClassName) {
		return theClassName + (theLoader != null ? theLoader.toString() : "<bootstrap>");
	}

	static class Key {
		private String classId = "";
		private String methodName = "";

		public void setClassId(String theClassId) {
			if(theClassId == null)
				throw new NullPointerException("class id is null");

			classId = theClassId;
		}

		public String getClassId() {
			return classId;
		}

		public void setMethodName(String theMethodName) {
			if(theMethodName == null)
				throw new NullPointerException("method name is null");

			methodName = theMethodName;
		}

		public String getMethodName() {
			return methodName;
		}
	}

	static String encodeKey(Key theKey) {
		return String.format("%s::%s", theKey.getMethodName(), theKey.getClassId());
	}

	static Key decodeKey(String theEncodedKey) {
		int markerIndex = theEncodedKey.indexOf("::");

		if(markerIndex < 0) 
			return null;

		Key rv = new Key();
		rv.setClassId(theEncodedKey.substring(markerIndex + 2));
		rv.setMethodName(theEncodedKey.substring(0, markerIndex));
		return rv;
	}
}
