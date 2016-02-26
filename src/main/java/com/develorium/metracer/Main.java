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
import com.sun.tools.attach.*;
import javax.management.*;
import javax.management.remote.*;

public class Main {
	int pid = 0;
	String pattern = null;
	Object agent = null;

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
				throw new Exception(String.format("2 arguments are expected, but %d given", theArguments.length));

			parsePid(theArguments[0]);
			parsePattern(theArguments[1]);
		} catch(Exception e) {
			throw new Exception(String.format("%s\nUsage: metracer.jar <PID> <PATTERN>", e.getMessage()));
        }
	}

	private void parsePid(String thePid) throws Exception {
		try {
			pid = Integer.parseInt(thePid);
		} catch(NumberFormatException e) {
			throw new Exception(String.format("Value \"%s\" of argument #0 doesn't denote a PID", thePid));
		}
				
		if(pid <= 0)
			throw new Exception(String.format("Given PID %d is invalid", pid));
	}

	private void parsePattern(String thePattern) throws Exception {
		pattern = thePattern;

		if(pattern.isEmpty())
			throw new Exception("Pattern is empty");

		try {
			Pattern.compile(pattern);
		} catch(PatternSyntaxException e) {
			throw new Exception(String.format("Provided pattern \"%s\" is malformed: %s", pattern, e.toString()));
		}
	}

	private void loadAgent() throws Exception {
		VirtualMachine vm = null;

		try {
			vm = VirtualMachine.attach(Integer.toString(pid));
		} catch(Exception e) {
			throw new Exception(String.format("Failed to connect to JVM with PID %d: %s", pid, e.getMessage()));
		}

		String jmxLocalConnectAddress = ensureManagementAgentIsRunning(vm);
		agent = ensureMetracerAgentIsRunning(vm, jmxLocalConnectAddress);
		vm.detach(); 
	}

	private String ensureManagementAgentIsRunning(VirtualMachine theVm) throws Exception, IOException {
		String address = getJmxLocalConnectAddress(theVm);

		if(address != null)
			return address;

		String javaHome = theVm.getSystemProperties().getProperty("java.home");
		String managementAgentFileNames[] = { "/jre/lib/management-agent.jar", "/lib/management-agent.jar" };

		for(String managementAgentFileName: managementAgentFileNames) {
			String fullManagementAgentFileName = javaHome + managementAgentFileName.replace("/", File.separator);

			if(new File(fullManagementAgentFileName).exists()) {
				theVm.loadAgent(fullManagementAgentFileName, "com.sun.management.jmxremote");
				address = getJmxLocalConnectAddress(theVm);

				if(address == null)
					throw new Exception("Failed to start management agent");

				return address;
			}
		}

		throw new Exception("Management agent is not found");
	}

	private String getJmxLocalConnectAddress(VirtualMachine theVm) throws IOException {
		String JmxLocalConnectAddressPropertyName = "com.sun.management.jmxremote.localConnectorAddress";
		Properties properties = theVm.getAgentProperties();
		String address = (String)properties.get(JmxLocalConnectAddressPropertyName);
		return address;
	}

	private Object ensureMetracerAgentIsRunning(VirtualMachine theVm, String theJmxLocalConnectAddress) throws java.net.MalformedURLException, IOException {
		JMXServiceURL jmxUrl = new JMXServiceURL(theJmxLocalConnectAddress);
		JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl);
		agent = getMetracerAgentMxBean(jmxConnector);
		return agent;
	}

	private Object getMetracerAgentMxBean(JMXConnector theJmxConnector) {
		return null;
	}
}
