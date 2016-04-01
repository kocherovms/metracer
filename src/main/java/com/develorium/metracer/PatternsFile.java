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

public class PatternsFile {
	private String classMatchingPattern = null;
	private String methodMatchingPattern = null;

	public PatternsFile(InputStream theInputStream) throws IOException {
		Objects.requireNonNull(theInputStream);
		loadPatterns(theInputStream);
	}

	public PatternsFile(OutputStream theOutputStream) {
		Objects.requireNonNull(theOutputStream);
	}

	public String getClassMatchingPattern() {
		return classMatchingPattern;
	}

	public String getMethodMatchingPattern() {
		return methodMatchingPattern;
	}

	public void consumePatterns(String thePatterns, String theContext) {
		//
	}

	private void loadPatterns(InputStream theInputStream) throws IOException {
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(theInputStream));
		String rawLine = null;
		StringBuilder classMatchingPatternBuilder = new StringBuilder();
		StringBuilder methodMatchingPatternBuilder = new StringBuilder();
		Set<String> classNames = new HashSet<String>();
		Set<String> classNamePlusMethodNames = new HashSet<String>();
		Pattern anySpacePattern = Pattern.compile("\\s");
		
		while((rawLine = inputReader.readLine()) != null) {
			String line = rawLine.trim();

			if(line.startsWith("#") || line.isEmpty())
				continue;

			int separatorIndex = line.indexOf("::");

			if(separatorIndex == -1 || separatorIndex == 0 || (separatorIndex + 2) >= line.length())
				throw new RuntimeException(String.format("Cannot extract class and method names from line \"%s\"", line));

			if(anySpacePattern.matcher(line).find())
				throw new RuntimeException(String.format("Line \"%s\" contains unallowed spaces, tabs or other separators", line));
				
			String className = line.substring(0, separatorIndex);
			String methodName = line.substring(separatorIndex + 2);
			String classNamePlusMethodName = String.format("%s::%s", className, methodName);

			if(!classNames.contains(className)) {
				classNames.add(className);

				if(classMatchingPatternBuilder.length() > 0)
					classMatchingPatternBuilder.append("|");

				classMatchingPatternBuilder.append(getRegexFriendlyString(className));
			}

			methodName = getRegexFriendlyString(methodName);

			if(!classNamePlusMethodNames.contains(classNamePlusMethodName)) {
				classNamePlusMethodNames.add(classNamePlusMethodName);

				if(methodMatchingPatternBuilder.length() > 0)
					methodMatchingPatternBuilder.append("|");

				methodMatchingPatternBuilder.append(getRegexFriendlyString(classNamePlusMethodName));
			}
		}

		if(classMatchingPatternBuilder.length() > 0) {
			classMatchingPattern = "(" + classMatchingPatternBuilder.toString() + ")";

			if(methodMatchingPatternBuilder.length() > 0) 
				methodMatchingPattern = "(" + methodMatchingPatternBuilder.toString() + ")";
		}
	}

	private String getRegexFriendlyString(String theUnfriendlyString) {
		StringBuilder friendlyString = new StringBuilder();

		for(int i = 0; i < theUnfriendlyString.length(); ++i) {
			int codePoint = theUnfriendlyString.codePointAt(i);
			
			if(Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == ':') {
				friendlyString.appendCodePoint(codePoint);
				continue;
			}

			if(codePoint == '$' || codePoint == '.') {
				friendlyString.append("\\");
				friendlyString.appendCodePoint(codePoint);
				continue;
			}

			friendlyString.append('.');
		}

		return friendlyString.toString();
	}
}