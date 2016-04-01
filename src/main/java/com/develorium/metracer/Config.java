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

public class Config {
	public static class BadConfig extends Exception {
		public BadConfig(String theMessage) {
			super(theMessage);
		}
	};

	enum COMMAND {
		HELP, LIST, INSTRUMENT, DEINSTRUMENT
	};

	public COMMAND command = null;
	public boolean isVerbose = false;
	public boolean isWithStackTrace = false;
	public String stackTraceFileName = null;
	public int pid = 0;
	public String classMatchingPattern = null;
	public String methodMatchingPattern = null;
	private LinkedList<String> argumentList = null;

	public Config(String[] theArguments) throws BadConfig {
		if(theArguments == null)
			throw new BadConfig("arguments are not specified");

		argumentList = new LinkedList<String>(Arrays.asList(theArguments));
		isVerbose = argumentList.remove("-v");
		command = findCommand();

		switch(command) {
		case HELP:
		case LIST:
			checkThereAreNoMoreArguments(command.toString());
			return;
		case INSTRUMENT:
			parseStackTraceOption();
		case DEINSTRUMENT:
			parsePositionalArguments();
			checkThereAreNoMoreArguments(command.toString());
			return;
		default:
			throw new BadConfig("Failed to recognize any known command");
		}
	}

	private COMMAND findCommand() {
		if(argumentList.remove("-h")) {
			return COMMAND.HELP;
		}
		else if(argumentList.remove("-l")) {
			return COMMAND.LIST;
		}
		else if(argumentList.remove("-r")) {
			return COMMAND.DEINSTRUMENT;
		}
		else {
			return COMMAND.INSTRUMENT;
		}
	}

	private void checkThereAreNoMoreArguments(String theCommand) throws BadConfig {
		if(!argumentList.isEmpty()) {
			StringBuilder b = new StringBuilder();

			for(String arg : argumentList)
				b.append(arg + " ");

			throw new BadConfig(String.format("Unexpected arguments for command %s: %s", theCommand, b.toString()));
		}
	}

	private void parseStackTraceOption() throws BadConfig {
		ListIterator<String> it = argumentList.listIterator();

		while(it.hasNext()) {
			String option = it.next();

			if(option.equals("-s") || option.equals("-S")) {
				isWithStackTrace = true;
				it.remove();

				if(option.equals("-S")) {
					if(!it.hasNext()) 
						throw new BadConfig("-S requires an accompanying file name (for stack traces)");

					stackTraceFileName = it.next();
					it.remove();
					break;
				}
			}
		}
	}

	private void parsePositionalArguments() throws BadConfig {
		if(argumentList.isEmpty())
			throw new BadConfig("mandatory 'PID' parameter is not specified");
	
		pid = parsePid(argumentList.removeFirst());
	
		if(command == COMMAND.DEINSTRUMENT) {
			// patterns are not needed for removal
			return;
		}
	
		classMatchingPattern = parsePattern(argumentList.isEmpty() ? null : argumentList.removeFirst(), "CLASS-MATCHING-PARAMETER");
		methodMatchingPattern = parsePattern(argumentList.isEmpty() ? null : argumentList.removeFirst(), "METHOD-MATCHING-PATTERN");
	}

	private static int parsePid(String thePid) throws BadConfig {
		int rv = 0;

		try {
			rv = Integer.parseInt(thePid);
		} catch(NumberFormatException e) {
			throw new BadConfig(String.format("Value \"%s\" of a PID argument is not an integer", thePid));
		}
				
		if(rv <= 0)
			throw new BadConfig(String.format("Given PID %d is invalid", rv));

		return rv;
	}

	private static String parsePattern(String thePattern, String thePatternName) throws BadConfig {
		if(thePattern == null)
			return thePattern;
		else if(thePattern.isEmpty())
			throw new BadConfig(String.format("Pattern %s is empty", thePatternName));

		try {
			Pattern.compile(thePattern);
			return thePattern;
		} catch(PatternSyntaxException e) {
			throw new BadConfig(String.format("Provided pattern \"%s\" is malformed: %s", thePattern, e.getMessage()));
		}
	}


}