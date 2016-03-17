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
	public Pattern classMatchingPattern = null;
	public Pattern methodMatchingPattern = null;
	public boolean isWithStackTraces = false;
	private Set<String> instrumentedMethods = Collections.synchronizedSet(new HashSet<String>(1000));

	public Patterns(String theClassMatchingPattern, String theMethodMatchingPattern, boolean theIsWithStackTraces) {
		classMatchingPattern = createPattern(theClassMatchingPattern);
		methodMatchingPattern = createPattern(theMethodMatchingPattern);
		isWithStackTraces = theIsWithStackTraces;
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
				isWithStackTraces == other.isWithStackTraces;
		}

		return false;
	}

	public void registerInstrumentedMethod(ClassLoader theLoader, String theClassName, String theMethodName) {
		if(theClassName == null || theMethodName == null)
			return;
		else if(theClassName.length() == 0 || theMethodName.length() == 0)
			return;

		Key key = new Key();
		key.setClassId(theClassName + (theLoader != null ? theLoader.toString() : "<bootstrap>"));
		key.setMethodName(theMethodName);
		instrumentedMethods.add(encodeKey(key));
	}
	
	public static class Counters {
		public int classesCount = 0;
		public int methodsCount = 0;
	}

	public Counters getCounters() {
		Counters rv = new Counters();
		Set<String> classNames = new HashSet<String>();

		synchronized(instrumentedMethods) {
			Iterator<String> i = instrumentedMethods.iterator();

			while(i.hasNext()) {
				String encodedKey = i.next();
				Key key = encodedKey != null ? decodeKey(encodedKey) : null;

				if(key == null)
					continue;

				classNames.add(key.getClassId());
				++rv.methodsCount;
			}
		}

		rv.classesCount = classNames.size();
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
