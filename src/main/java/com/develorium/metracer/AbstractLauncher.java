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
	protected String selfPid = null;
	protected String selfUserName = null;
	protected File selfJar = null;
	protected Config config = null;
	protected String launchString = null;
	protected String javaExePath = null;
	protected String javaExeFolderPath = null;
	protected String toolsJarPath = null;
	protected String userNameOfTargetJvm = null;

	public AbstractLauncher() {
		selfPid = Helper.getSelfPid();
		selfUserName = resolveUserNameOfTargetJvm(Integer.parseInt(selfPid));
		selfJar = Helper.getSelfJarFileSafe();

		if(selfJar == null)
			throw new RuntimeException("Failed to resolve self jar file");

		launchString = String.format("%s -jar %s", getJavaExeName(), selfJar.getName());
	}

	public void execute(String[] theArguments) {
		int rv = 0;

		try {
			config = new Config(theArguments);

			if(config.pid == 0) {
				try {
					config.pid = Helper.autoDiscoverOnlyJvm();
					System.out.format("Autodiscovered JVM PID %d\n", config.pid);
				} catch(Helper.JvmAutoDiscoverFailure e) {
					System.err.println(e.getMessage());
			
					if(!e.isAmbigious) 
						throw e;

					config.command = Config.COMMAND.LIST;
				}
			}

			String[] javaLocation = resolveJavaLocation();
			javaExePath = javaLocation[0];
			javaExeFolderPath = javaLocation[1];

			if(!new File(javaExePath).exists())
				throw new RuntimeException(String.format("Resolved %s executable (%s) doesn't exist", getJavaExeName(), javaExePath));

			if(config.command.isToolsJarNeeded) {
				toolsJarPath = resolveToolsJarPath(javaExeFolderPath);
				
				if(toolsJarPath == null)
					throw new RuntimeException("Failed to resolve tools.jar from a JDK. Please, make sure that JDK is installed and JAVA_HOME environment variable is properly set");
			}

			if(config.command.isImpersonationNeeded) {
				userNameOfTargetJvm = resolveUserNameOfTargetJvm(config.pid);
			}

			try {
				Map<String, String> customEnvVariables = new HashMap<String, String>();
				customEnvVariables.put(Constants.METRACER_LAUNCH_STRING, launchString);
				customEnvVariables.put(Constants.METRACER_LAUNCHER_PID, selfPid);
				prepareExecutionEnvironment(customEnvVariables);

				java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						cleanupExecutionEnvironment();
					}
				});

				List<String> args = prepareArguments(theArguments, customEnvVariables);
				ProcessBuilder pb = new ProcessBuilder(args);
				pb.inheritIO();
				pb.environment().putAll(customEnvVariables);
				Process p = pb.start();

				while(true) {
					try {
						rv = p.waitFor();
						break;
					} catch(InterruptedException e) {
					}
				}
			} finally {
				postProcessCommand();
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

		System.exit(rv);
	}

	protected abstract String getJavaExeName();
	protected abstract String resolveUserNameOfTargetJvm(int thePid);
	protected void prepareExecutionEnvironment(Map<String, String> theEnvVariables) {
	}
	protected void cleanupExecutionEnvironment() {
	}
	protected abstract List<String> prepareArguments(String[] theOriginalArguments, Map<String, String> theEnvVariables);
	protected void postProcessCommand() {
	}

	protected String[] resolveJavaLocation() {
		Map<String, String> env = System.getenv();
		String home = env.containsKey("JAVA_HOME")
			? env.get("JAVA_HOME")
			: System.getProperty("java.home");

		if(home == null || home.isEmpty()) {
			String message = String.format("Failed to resolve path to %s", getJavaExeName());
			message += " - neither JAVA_HOME env variable or 'java.home' system prop is set";
			throw new RuntimeException(message);
		}

		String sep = File.separator;
		assert(sep != null);
		String[] rv = {
			home + sep + "bin" + sep + getJavaExeName(),
			home + sep + "bin",
		};
		return rv;
	}

	static String resolveToolsJarPath(String theJavaExeFolderPath) {
		String sep = System.getProperty("file.separator");
		File file = new File(theJavaExeFolderPath + sep + ".." + sep + "lib" + sep + "tools.jar");

		if(file.exists())
			return file.getAbsolutePath();

		file = new File(theJavaExeFolderPath + sep + ".." + sep + ".." + sep + "lib" + sep + "tools.jar");

		if(file.exists())
			return file.getAbsolutePath();

		return null;
	}
}
