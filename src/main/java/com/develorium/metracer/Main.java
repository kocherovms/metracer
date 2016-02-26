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
import com.sun.tools.attach.*;

public class Main {
	int pid = 0;
	String pattern = null;

	public static void main(String[] theArguments) {
		new Main().execute(theArguments);
	}

	private void execute(String[] theArguments) {
		try {
			parseArguments(theArguments);
			loadAgent();
		} catch(Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	private void parseArguments(String[] theArguments) throws Exception {
		try {
			if(theArguments.length != 2) 
				throw new Exception(String.format("2 arguments are expected, but %1$s given", theArguments.length));

			parsePid(theArguments[0]);
			parsePattern(theArguments[1]);
		} catch(Exception e) {
			throw new Exception(String.format("%1$s\nUsage: metracer.jar <PID> <PATTERN>\n", e.getMessage()));
        }
	}

	private void parsePid(String thePid) throws Exception {
		try {
			pid = Integer.parseInt(thePid);
		} catch(NumberFormatException e) {
			throw new Exception(String.format("Value \"%1$s\" of argument #0 doesn't denote a PID", thePid));
		}
				
		if(pid <= 0)
			throw new Exception(String.format("Given PID %1$d is invalid", pid));
	}

	private void parsePattern(String thePattern) throws Exception {
		pattern = thePattern;

		if(pattern.isEmpty())
			throw new Exception("Pattern is empty");

		try {
			Pattern.compile(pattern);
		} catch(PatternSyntaxException e) {
			throw new Exception(String.format("Provided pattern \"%1$s\" is malformed: %2$s", pattern, e.toString()));
		}
	}

	private void loadAgent() throws Exception {
		try {
			VirtualMachine vm = VirtualMachine.attach(Integer.toString(pid));
			Properties properties = vm.getSystemProperties();
			properties.list(new java.io.PrintStream(System.out));
		} catch(Exception e) {
			throw new Exception(String.format("Failed to connect to JVM with PID %1$d: %2$s", pid, e.getMessage()));
		}
	}
}
