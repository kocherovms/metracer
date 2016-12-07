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

public class LinuxLauncher extends AbstractLauncher {
	private String sttyState = null;

	@Override
	protected String getJavaExeName() {
		return "java";
	}

	@Override
	protected String resolveUserNameOfTargetJvm(int thePid) {
		try {
			InputStream stream = new FileInputStream(String.format("/proc/%d/status", thePid));
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			return processStatusContent(reader);
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to resolve user ID for PID %d: %s", thePid, e.getMessage()), e);
		}
	}

	@Override
	protected void prepareExecutionEnvironment(Map<String, String> theEnvVariables) {
		try {
			sttyState = execStty("-g", true);
			execStty("-echo", false);
			execStty("cbreak", false);
		} catch(Throwable e) {
			System.err.format("Failed to enable non-buffered input: %s\n", e);
			// we don't need a half-working terminal - better to restore it to an original state
			cleanupExecutionEnvironment();
			return;
		}

		theEnvVariables.put(Constants.METRACER_IS_CBREAK_DISABLED, "1");
	}

	@Override
	protected void cleanupExecutionEnvironment() {
		if(sttyState == null) 
			return;

		try {
			execStty(sttyState, false);
		} catch(Throwable e) {
			System.err.format("Failed to restore stty state to \"%s\": %s\n", sttyState, e);
		}

		sttyState = null;
	}

	@Override
	protected List<String> prepareArguments(String[] theOriginalArguments, Map<String, String> theEnvVariables) {
		assert(theOriginalArguments != null);
		assert(theEnvVariables != null);

		List<String> args = new ArrayList<String>();

		if(config.command.isImpersonationNeeded && !selfUserName.equals(userNameOfTargetJvm)) {
			args.add("sudo");
			args.add("-u");
			args.add("#" + userNameOfTargetJvm);

			for(Map.Entry<String, String> kv : theEnvVariables.entrySet())
				args.add(String.format("%s=%s", kv.getKey(), kv.getValue()));
		}

		args.add(javaExePath);
		
		if(toolsJarPath != null)
			args.add(String.format("-Xbootclasspath/a:%s", toolsJarPath));
		
		args.add("-cp");
		assert(selfJar != null);
		args.add(selfJar.getAbsolutePath());
		args.add(Main.class.getName());
		
		for(String arg : theOriginalArguments)
			args.add(arg);
		
		return args;
	}

	@Override
	protected void postProcessCommand() {
		if(config.command != Config.COMMAND.LIST)
			return;
		
		if(selfUserName.equals("0"))
			return;

		System.out.format("\nWARNING: JVM list may be not full (missing root privileges). Use `sudo %s` to get a full list\n", launchString);
	}

	@Override
	protected boolean isFileAvailableForReading(String theFileName, String theUserId) {
		List<String> args = Arrays.asList("sudo", "-n", "-u", "#" + theUserId, "head", "-c1b", theFileName);
		ProcessBuilder pb = new ProcessBuilder(args);

		try {
			Process p = pb.start();

			while(true) {
				try {
					int rv = p.waitFor();
					return rv == 0;
				} catch(InterruptedException e) {
				}
			}
		} catch(IOException e) {
			return false;
		}
	}

	static String processStatusContent(BufferedReader theReader) throws IOException {
		String line = null;

		while((line = theReader.readLine()) != null) {
			line = line.trim().toLowerCase();

			if(!line.startsWith("uid:"))
				continue;

			// IDs go in following order (http://man7.org/linux/man-pages/man5/proc.5.html): 
			// Real, effective, saved set, and filesystem UIDs. 
			// We need a real UID
			Scanner scanner = new Scanner(line);
			scanner.next(); // skip Uid:
			return "" + scanner.nextInt();
		}

		return null;
	}

	static String execStty(String theArgument, boolean theIsOutputNeeded) throws IOException {
		assert(theArgument != null);
		String[] args = { "stty", theArgument };
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.inheritIO();

		if(theIsOutputNeeded)
			pb.redirectOutput(ProcessBuilder.Redirect.PIPE);

		Process p = pb.start();
		String output = null;

		if(theIsOutputNeeded) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;

			while((line = reader.readLine()) != null) {
				if(output == null)
					output = line;
				else
					output += "\n" + line;
			}
		}

		while(true) {
			try {
				p.waitFor();
				break;
			} catch(InterruptedException e) {
			}
		}

		return output;
	}
}
