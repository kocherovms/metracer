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

public abstract class AbstractLauncher {
	public void execute(String[] theArguments) {
		File selfJar = Helper.getSelfJarFileSafe();
		String launchString = String.format("%s -jar %s", getJavaExeName(), selfJar != null ? selfJar.getName() : getDefaultSelfJarName());

		try {
			Config config = new Config(theArguments);
			String[] javaLocation = resolveJavaLocation();
			String javaExePath = javaLocation[0];
			String javaExeFolderPath = javaLocation[1];

			if(!new File(javaExePath).exists())
				throw new RuntimeException(String.format("Resolved %s executable (%s) doesn't exist", getJavaExeName(), javaExePath));

			String toolsJarFileName = null;

			if(config.command.isToolsJarNeeded) {
				toolsJarFileName = resolveToolsJarFileName(javaExeFolderPath);
				System.out.println(toolsJarFileName);
				
				if(!new File(toolsJarFileName).exists())
					throw new RuntimeException("Failed to resolve tools.jar from a JDK. Please, make sure that JDK is installed and JAVA_HOME environment variable is properly set");
			}

			String userName = null;

			if(config.command.isImpersonationNeeded) {
				userName = resolveUserNameOfTargetJvm(config.pid);
			}

			try {
				Map<String, String> customEnvVariables = new HashMap<String, String>();
				customEnvVariables.put(Constants.METRACER_LAUNCH_STRING, launchString);
				customEnvVariables.put(Constants.METRACER_LAUNCHER_PID, Helper.getSelfPid());
				prepareExecutionEnvironment(customEnvVariables);

				List<String> args = new ArrayList<String>();
				args.add(javaExePath);

				if(toolsJarFileName != null)
					args.add(String.format("-Xbootclasspath/a:%s", toolsJarFileName));

				args.add("-cp");
				// TODO: handle null
				args.add(selfJar.getAbsolutePath());
				args.add("com.develorium.metracer.Main");
				
				for(String arg : theArguments)
					args.add(arg);

				if(config.command.isImpersonationNeeded)
					prepareImpersonation(args, customEnvVariables, userName);

				for(String arg : args)
					System.out.println("kms@ " + arg);

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.inheritIO();
				pb.environment().putAll(customEnvVariables);
				Process p = pb.start();

				while(true) {
					try {
						System.exit(p.waitFor());
					} catch(InterruptedException e) {
					}
				}
			} finally {
				cleanupExecutionEnvironment();
			}
		} catch(Config.BadConfig e) {
			System.err.println(e.getMessage());
			Helper.printUsage(System.err, launchString);
			System.exit(1);
		} catch(Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(2);
		}
	}

	protected abstract String getJavaExeName();
	protected abstract String getDefaultSelfJarName();
	protected abstract String resolveUserNameOfTargetJvm(int thePid);
	protected void prepareExecutionEnvironment(Map<String, String> theEnvVariables) {
	}
	protected void cleanupExecutionEnvironment() {
	}
	protected abstract void prepareImpersonation(List<String> theArguments, Map<String, String> theEnvVariables, String theUserName);

	protected String[] resolveJavaLocation() {
		Map<String, String> env = System.getenv();
		String home = env.containsKey("JAVA_HOME")
			? env.get("JAVA_HOME")
			: System.getProperty("java.home");

		if(home == null || home.isEmpty())
			throw new RuntimeException(String.format("Failed to resolve path to %s - neither JAVA_HOME env variable or 'java.home' system prop is set", getJavaExeName()));

		String sep = File.separator;
		assert(sep != null);
		String[] rv = {
			home + sep + "bin" + sep + getJavaExeName(),
			home + sep + "bin",
		};
		return rv;
	}

	static String resolveToolsJarFileName(String theJavaExeFolderPath) {
		String sep = System.getProperty("file.separator");
		return theJavaExeFolderPath + sep + ".." + sep + "lib" + sep + "tools.jar";
	}
}
