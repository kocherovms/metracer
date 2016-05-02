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
import java.io.*;
import java.nio.charset.*;

public class WinLauncher extends AbstractLauncher {
	public static void main(String[] theArguments) {
		new WinLauncher().execute(theArguments);
	}

	@Override
	protected String getJavaExeName() {
		return "java.exe";
	}

	@Override
	protected String resolveUserNameOfTargetJvm(int thePid) {
		return resolveUserNameOfTargetJvm_impl(thePid);
	}

	@Override
	protected List<String> prepareArguments(String[] theOriginalArguments, Map<String, String> theEnvVariables) {
		assert(theOriginalArguments != null);
		assert(theEnvVariables != null);

		List<String> args = new ArrayList<String>();

		if(config.command.isImpersonationNeeded && !selfUserName.equals(userNameOfTargetJvm)) {
			args.add("runas");
			args.add("/env");
			args.add("/user:" + userNameOfTargetJvm);
			String toolsJarSubArgument = toolsJarPath != null
				? String.format("-Xbootclasspath/a:\\\"%s\\\"", toolsJarPath)
				: "";
			
			assert(selfJar != null);
			String argumentsStrippedCommand = String.format("\\\"%s\\\" %s -cp \\\"%s\\\" %s", javaExePath, toolsJarSubArgument, selfJar.getAbsolutePath(), Main.class.getName());
			args.add(argumentsStrippedCommand);

			int i = 0;

			for(String arg : theOriginalArguments)
				theEnvVariables.put("METRACER_ARGUMENT_" + (i++), arg);
		}
		else {
			args.add(javaExePath);
		
			if(toolsJarPath != null)
				args.add(String.format("-Xbootclasspath/a:%s", toolsJarPath));
		
			args.add("-cp");
			assert(selfJar != null);
			args.add(selfJar.getAbsolutePath());
			args.add(Main.class.getName());

			for(String arg : theOriginalArguments)
				args.add(arg);
		}

		return args;
	}

	static String resolveUserNameOfTargetJvm_impl(int thePid) {
		try {
			BufferedReader reader = getTaskList();
			return processTaskListOutput(reader, "" + thePid);
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to resolve user name of PID %d: %s", thePid, e.getMessage()), e);
		}
	}
	
 	static BufferedReader getTaskList() throws IOException {
		String charsetName = getCurrentCharsetName();
		String[] args = {
			"tasklist.exe",
			"/fo",
			"list", // not using a CSV, because tasklist produces incorrect CSV (e.g. when program caption contains " symbol)
			"/v"
		};
		Process p = java.lang.Runtime.getRuntime().exec(args);
		InputStream stdout = p.getInputStream();
		InputStreamReader inputStreamReader = charsetName != null
			? new InputStreamReader(stdout, charsetName)
			: new InputStreamReader(stdout);
		return new BufferedReader(inputStreamReader);
	}

	static String getCurrentCharsetName() {
		try {
			Process p = java.lang.Runtime.getRuntime().exec("chcp.com");
			String line = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
			Scanner scanner = new Scanner(line).useDelimiter(":");
			scanner.next();
			return scanner.next().trim();
		} catch(Throwable e) {
			System.err.format("Failed to resolve current charset name: %s\n", e.getMessage());
			return null;
		}
	}

	static String processTaskListOutput(BufferedReader theReader, String thePid) throws IOException {
		String line = null;
		int fieldIndex = 0;
		boolean pidFound = false;

		while((line = theReader.readLine()) != null) {
			line = line.trim();
			
			if(line.isEmpty()) {
				fieldIndex = 0;
				continue;
			}

			if(fieldIndex == 1 && !pidFound) {
				String currentPid = extractFieldValue(line);
				
				if(currentPid != null && currentPid.equals(thePid)) {
					pidFound = true;
				}
			}
			else if(fieldIndex == 6 && pidFound) {
				return extractFieldValue(line);
			}

			fieldIndex++;
		}

		return null;
	}

	static String extractFieldValue(String theField) {
		int index = theField.indexOf(':');
		
		if(index < 0 || (index == theField.length() - 1))
			return null;

		String rv = theField.substring(index + 1);
		return rv.trim();
	}
}
