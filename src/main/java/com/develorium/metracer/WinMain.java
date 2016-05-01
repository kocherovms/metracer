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

public class WinMain {
	public static void main(String[] theArguments) {
		File selfJar = Helper.getSelfJarFileSafe();
		String launchString = String.format("java -jar %s", selfJar != null ? selfJar.getName() : "metracer_win.jar");

		try {
			Config config = new Config(theArguments);
			String[] javaLocation = resolveJavaLocation();
			String javaExe = javaLocation[0];
			String javaExeFolder = javaLocation[1];

			if(!new File(javaExe).exists())
				throw new RuntimeException(String.format("Resolved 'java.exe' executable (%s) doesn't exist", javaExe));

			String toolsJarFileName = null;

			if(config.command.isToolsJarNeeded) {
				toolsJarFileName = resolveToolsJarFileName(javaExeFolder);
				System.out.println(toolsJarFileName);
				
				if(!new File(toolsJarFileName).exists())
					throw new RuntimeException("Failed to resolve tools.jar from a JDK. Please, make sure that JDK is installed and JAVA_HOME environment variable is properly set");
			}

			String userName = null;

			if(config.command.isImpersonationNeeded) {
				userName = resolveUserName(theArguments);
			}
			
			List<String> args = new ArrayList<String>();
			args.add(javaExe);

			if(toolsJarFileName != null)
				args.add(String.format("-Xbootclasspath/a:%s", toolsJarFileName));

			args.add("-cp");
			// TODO: handle null
			args.add(selfJar.getAbsolutePath());
			args.add("com.develorium.metracer.Main");
				
			for(String arg : theArguments)
				args.add(arg);

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.inheritIO();
			pb.environment().put(Constants.METRACER_LAUNCH_STRING, launchString);
			pb.environment().put(Constants.METRACER_LAUNCHER_PID, Helper.getSelfPid());
			Process p = pb.start();

			while(true) {
				try {
					System.exit(p.waitFor());
				} catch(InterruptedException e) {
				}
			}
		} catch(Config.BadConfig e) {
			System.err.println(e.getMessage());
			Helper.printUsage(System.err, launchString);
			System.exit(1);
		//} catch(IOException e) {
		//	System.err.println(e.getMessage());
		//	e.printStackTrace(System.err);
		//	System.exit(2);
		} catch(Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(3);
		}
	}

	static String[] resolveJavaLocation() {
		Map<String, String> env = System.getenv();
		String home = env.containsKey("JAVA_HOME")
			? env.get("JAVA_HOME")
			: System.getProperty("java.home");

		if(home == null || home.isEmpty())
			throw new RuntimeException("Failed to resolve path to java.exe - neither JAVA_HOME env variable or 'java.home' system prop is set ");

		String sep = System.getProperty("file.separator");
		String[] rv = {
			home + sep + "bin" + sep + "java",
			home + sep + "bin",
		};
		return rv;
	}

	static String resolveToolsJarFileName(String theJavaExeFolder) {
		String sep = System.getProperty("file.separator");
		return theJavaExeFolder + sep + ".." + sep + "lib" + sep + "tools.jar";
	}

	static String resolveUserName(String[] theArguments) throws Config.BadConfig, IOException {
		Config config = new Config(theArguments);
		BufferedReader reader = getTaskList();
		return resolveUserNameOfPid(reader, "" + config.pid);
	}
	
 	static BufferedReader getTaskList() throws IOException {
		String[] args = {
			"tasklist.exe",
			"/fo",
			"list", // not using a CSV, because tasklist produces incorrect CSV (e.g. when program caption contains " symbol)
			"/v"
		};
		Process p = java.lang.Runtime.getRuntime().exec(args);
		InputStream stdout = p.getInputStream();
		return new BufferedReader(new InputStreamReader(stdout));
	}
	
	static String resolveUserNameOfPid(BufferedReader theReader, String thePid) throws IOException {
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
