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
import com.sun.tools.attach.*;
import java.lang.management.*;

public class Aux {
	public static void printUsage(PrintStream theOutput) {
		try {
			theOutput.println(loadInfoResource("usage.txt"));
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

	public static boolean waitForQuit() {
		Console console = System.console();

		if(console != null)
			return waitForQuitFromConsole(console);
		else 
			return waitForQuitFromStdin();
	}

	private static void printHelp(PrintStream theOutput) {
		try {
			String usage = loadInfoResource("usage.txt");
			String help = loadInfoResource("help.txt");
			String processedHelp = help.replace("${usage}", usage);
			theOutput.println(processedHelp);
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to print help: %s", t.getMessage()), t);
		}
	}

	private static void printJvmList(PrintStream theOutput) {
		String selfPid = getSelfPid();
		List<VirtualMachineDescriptor> jvmList = VirtualMachine.list();
		List<String> jvms = new ArrayList<String>();

		for(VirtualMachineDescriptor jvm: jvmList) {
			if(selfPid.equals(jvm.id()))
				continue;

			jvms.add(String.format("%s\t%s", jvm.id(), jvm.displayName()));
		}

		if(jvms.isEmpty()) {
			theOutput.println("<No JVM for connection found>");
			return;
		}

		theOutput.println("PID\tNAME");

		for(String jvm : jvms)
			theOutput.println(jvm);
	}

	private static String loadInfoResource(String theResourceId) {
		try {
			ClassLoader loader = Aux.class.getClassLoader();
			InputStream stream = loader.getResourceAsStream(theResourceId);

			if(stream == null)
				throw new RuntimeException("Failed to locate a resource " + theResourceId);

			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(streamReader);
			Map<String, String> env = System.getenv();
			String launchString = env.get("METRACER_LAUNCH_STRING");

			if(launchString == null)
				launchString = "java -Xbootclasspath/a:<path-to-tools.jar> -jar metracer.jar";

			StringBuilder rv = new StringBuilder();
			String line;

			while((line = reader.readLine()) != null) {
				String processedLine = line.replace("${launchstring}", launchString);

				if(rv.length() > 0)
					rv.append("\n");

				rv.append(processedLine);
			}

			return rv.toString();
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to load info resource %s: %s", theResourceId, t.getMessage()), t);
		}
	}

	private static String getSelfPid() {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.substring(0, jvmName.indexOf('@'));
	}

	private static boolean waitForQuitFromConsole(Console theConsole) {
		while(true) {
			try {
				int symbol = theConsole.reader().read();
			
				if(symbol == 113) // 'q'
					return true;
				else if(symbol == 81) // 'Q'
					return false;
			} catch(IOException e) {
			}
		}
	}

	private static boolean waitForQuitFromStdin() {
		Scanner scanner = new Scanner(System.in);

		while(true) {
			String input = scanner.next();			
			
			if(input.equals("q"))
				return true;
			else if(input.equals("Q"))
				return false;
		}
	}
}
