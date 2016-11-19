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
import java.net.*;
import com.sun.tools.attach.*;
import java.lang.management.*;

public class Helper {
	public static class JvmAutoDiscoverFailure extends Exception {
		public boolean isAmbigious = false;

		public JvmAutoDiscoverFailure(String theMessage, boolean theIsAmbigious) {
			super(theMessage);
			isAmbigious = theIsAmbigious;
		}
	}

	public static void printUsage(PrintStream theOutput, String theLaunchString) {
		try {
			theOutput.println(loadAndProcessTextResource("usage.txt", theLaunchString));
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to print usage: %s", t.getMessage()), t);
		}
	}

	public static boolean executeAuxCommands(Config.COMMAND theCommand, PrintStream theOutput) {
		switch(theCommand) {
		case HELP:
			printHelp(theOutput);
			return true;
		case LIST:
			printJvmList(theOutput);
			return true;
		}

		return false;
	}

	public static int autoDiscoverOnlyJvm() throws JvmAutoDiscoverFailure {
		List<VirtualMachineDescriptor> availableJvms = VirtualMachine.list();

		if(availableJvms.isEmpty())
			throw new JvmAutoDiscoverFailure("No JVM found for connection", false);
		else if(availableJvms.size() > 1)
			throw new JvmAutoDiscoverFailure("More than one JVM for connection detected, please, specify desired one manually", true);

		try {
			return Integer.parseInt(availableJvms.get(0).id());
		} catch(NumberFormatException e) {
			throw new JvmAutoDiscoverFailure("Failed to determine PID of JVM for connection, please, specify desired one manually", true);
		}
	}
	
	public static boolean waitForQuit(InputStream theStdin, PrintStream theStdout) {
		while(true) {
			try {
				int symbol = theStdin.read();

				if(symbol == 'q')
					return true;
				else if(symbol == 'Q')
					return false;
				else if(symbol == 10) { // return / enter
					if(System.getenv().get(Constants.METRACER_IS_CBREAK_DISABLED) != null)
						theStdout.println("");
				}
			} catch(IOException e) {
			}
		}
	}

	public static File getSelfJarFile() throws URISyntaxException {
		return new File(Helper.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
	}

	public static File getSelfJarFileSafe() {
		try {
			return getSelfJarFile();
		} catch(Throwable e) {
			return null;
		}
	}

	public static String getSelfPid() {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.substring(0, jvmName.indexOf('@'));
	}

	public static String loadTextResource(String theResourceId) {
		try {
			ClassLoader loader = Helper.class.getClassLoader();
			InputStream stream = loader.getResourceAsStream(theResourceId);

			if(stream == null)
				throw new RuntimeException("Failed to locate a resource " + theResourceId);

			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(streamReader);
			StringBuilder rv = new StringBuilder();
			String line;

			while((line = reader.readLine()) != null) {
				if(rv.length() > 0)
					rv.append("\n");

				rv.append(line);
			}

			return rv.toString();
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to load text resource %s: %s", theResourceId, t.getMessage()), t);
		}
	}

	private static void printHelp(PrintStream theOutput) {
		try {
			String usage = loadAndProcessTextResource("usage.txt", null);
			String help = loadAndProcessTextResource("help.txt", null);
			String processedHelp = help.replace("${usage}", usage);
			theOutput.println(processedHelp);
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to print help: %s", t.getMessage()), t);
		}
	}

	private static List<VirtualMachineDescriptor> listAvailableJvms() {
		List<VirtualMachineDescriptor> result = new ArrayList<VirtualMachineDescriptor>();
		Set<String> blacklistedPids = new HashSet<String>();
		blacklistedPids.add(getSelfPid());
		String launcherPid = System.getenv().get(Constants.METRACER_LAUNCHER_PID);

		if(launcherPid != null && !launcherPid.isEmpty())
			blacklistedPids.add(launcherPid);

		List<VirtualMachineDescriptor> jvmList = VirtualMachine.list();

		for(VirtualMachineDescriptor jvm: jvmList) {
			if(blacklistedPids.contains(jvm.id()))
				continue;

			result.add(jvm);
		}

		return result;
	}

	private static void printJvmList(PrintStream theOutput) {
		List<VirtualMachineDescriptor> availableJvms = listAvailableJvms();
		List<String> lines = new ArrayList<String>();

		for(VirtualMachineDescriptor jvm : availableJvms) {
			lines.add(String.format("%s\t%s", jvm.id(), jvm.displayName()));
		}

		if(lines.isEmpty()) {
			theOutput.println("<No JVM for connection found>");
			return;
		}

		theOutput.println("PID\tNAME");

		for(String line : lines)
			theOutput.println(line);
	}

	private static String loadAndProcessTextResource(String theResourceId, String theLaunchString) {
		String textResourceContent = loadTextResource(theResourceId);
		String launchString = theLaunchString;

		if(launchString == null) {
			Map<String, String> env = System.getenv();
			launchString = env.get(Constants.METRACER_LAUNCH_STRING);

			if(launchString == null)
				launchString = "java -Xbootclasspath/a:<path-to-tools.jar> -jar metracer.jar";
		}

		return textResourceContent.replace("${launchstring}", launchString);
	}
}
