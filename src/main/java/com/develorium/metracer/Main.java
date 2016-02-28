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
import com.develorium.metracer.dynamic.*;

public class Main {
	int pid = 0;
	String classMatchingPattern = null;
	String methodMatchingPattern = null;
	MBeanServerConnection connection = null;
	ObjectName agentMxBeanName = null;
	AgentMXBean agent = null;

	public static void main(String[] theArguments) {
		new Main().execute(theArguments);
	}

	private void execute(String[] theArguments) {
		try {
			parseArguments(theArguments);
			loadAgent();
			configureAgent();
			startListeningToAgentEvents();
			waitForever();
		} catch(Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void parseArguments(String[] theArguments) {
		if(theArguments.length != 2 && theArguments.length != 3)
			throw new RuntimeException(String.format("2 or 3 arguments are expected, but %d given", theArguments.length));

		pid = parsePid(theArguments[0]);
		classMatchingPattern = parsePattern(theArguments[1], "class matching pattern");
		methodMatchingPattern = parsePattern(theArguments.length > 2 ? theArguments[2] : null, "method matching pattern");
	}

	private void loadAgent() {
		String exceptionMessage = "";

		try {
			exceptionMessage = String.format("Failed to connect to JVM with PID %d", pid);
			VirtualMachine vm = VirtualMachine.attach(Integer.toString(pid));
			say(String.format("Attached to JVM (PID %d)", pid));

			exceptionMessage = "Failed to ensure management agent is running";
			String jmxLocalConnectAddress = ensureManagementAgentIsRunning(vm);
			say("Management agent is running");

			exceptionMessage = "Failed to connect to MBean server";
			connection = connectToMbeanServer(jmxLocalConnectAddress);
			say("Connected to MBean server");

			exceptionMessage = "Failed to ensure metracer agent is running";
			agent = ensureMetracerAgentIsRunning(vm);
			say("metracer agent is running");

			vm.detach(); 
			say(String.format("Detached from JVM (PID %d)", pid));
		} catch(Throwable e) {
			throw new RuntimeException(String.format("%s: %s", exceptionMessage, e.getMessage()), e);
		}
	}

	private void configureAgent() {
		agent.setPatterns(classMatchingPattern, methodMatchingPattern);
		say(String.format("Class matching pattern set to \"%s\"", classMatchingPattern));

		if(methodMatchingPattern != null)
			say(String.format("Method matching pattern set to \"%s\"", methodMatchingPattern));
	}

	private void startListeningToAgentEvents() {
		try {
			connection.addNotificationListener(agentMxBeanName, new NotificationListener() {
					@Override
						public void handleNotification(Notification theNotification, Object theHandback) {
						System.out.println(theNotification.getMessage());
					}
				}, 
				null, null);
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to start listening to agent events: %s", e.getMessage()), e);
		}
	}

	private void waitForever() {
		while(true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			} catch (InterruptedException e) {
			}
		}
	}

	private static int parsePid(String thePid) {
		int rv = 0;

		try {
			rv = Integer.parseInt(thePid);
		} catch(NumberFormatException e) {
			throw new RuntimeException(String.format("Value \"%s\" of argument #0 doesn't denote a PID", thePid), e);
		}
				
		if(rv <= 0)
			throw new RuntimeException(String.format("Given PID %d is invalid", rv));

		return rv;
	}

	private static String parsePattern(String thePattern, String thePatternName) {
		if(thePattern == null)
			return thePattern;
		else if(thePattern.isEmpty())
			throw new RuntimeException(String.format("Pattern %s is empty", thePatternName));

		try {
			Pattern.compile(thePattern);
			return thePattern;
		} catch(PatternSyntaxException e) {
			throw new RuntimeException(String.format("Provided pattern \"%s\" is malformed: %s", thePattern, e.getMessage()), e);
		}
	}

	private String ensureManagementAgentIsRunning(VirtualMachine theVm) throws Exception {
		String address = getJmxLocalConnectAddress(theVm);

		if(address != null)
			return address;

		say("Management agent is not running");
		String managementAgentFileName = locateManagementAgentJar(theVm);

		if(managementAgentFileName == null)
			throw new Exception("Management agent JAR is not found");

		say(String.format("Loading management agent from \"%s\"", managementAgentFileName));
		theVm.loadAgent(managementAgentFileName, "com.sun.management.jmxremote");
		say("Management agent loaded");
		address = getJmxLocalConnectAddress(theVm);

		if(address == null)
			throw new Exception("Failed to start management agent");

		return address;
	}

	private String locateManagementAgentJar(VirtualMachine theVm) throws IOException {
		String javaHome = theVm.getSystemProperties().getProperty("java.home");
		String managementAgentFileNames[] = { "/jre/lib/management-agent.jar", "/lib/management-agent.jar" };

		for(String managementAgentFileName: managementAgentFileNames) {
			String fullManagementAgentFileName = javaHome + managementAgentFileName.replace("/", File.separator);

			if(new File(fullManagementAgentFileName).exists()) 
				return fullManagementAgentFileName;
		}

		return null;
	}

	private String getJmxLocalConnectAddress(VirtualMachine theVm) throws IOException {
		String JmxLocalConnectAddressPropertyName = "com.sun.management.jmxremote.localConnectorAddress";
		Properties properties = theVm.getAgentProperties();
		String address = (String)properties.get(JmxLocalConnectAddressPropertyName);
		return address;
	}

	private MBeanServerConnection connectToMbeanServer(String theJmxLocalConnectAddress) throws java.net.MalformedURLException, IOException, Exception {
		JMXServiceURL jmxUrl = new JMXServiceURL(theJmxLocalConnectAddress);
		JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl);
		return jmxConnector.getMBeanServerConnection();
	}

	private AgentMXBean ensureMetracerAgentIsRunning(VirtualMachine theVm) throws Exception {
		agent = getMetracerAgentMxBean();

		if(agent != null) 
			return agent;

		say("metracer agent is not loaded");
		String metracerAgentFileName = resolveMetracerAgentJar();
			
		if(!new File(metracerAgentFileName).exists())
			throw new Exception(String.format("Resolved metracer jar \"%s\" doesn't exist", metracerAgentFileName));

		say(String.format("Loading metracer agent from \"%s\"", metracerAgentFileName));
		theVm.loadAgent(metracerAgentFileName, null);
		say("metracer agent loaded");

		agent = getMetracerAgentMxBean();

		if(agent == null)
			throw new Exception("Failed to run metracer agent");

		return agent;
	}

	private AgentMXBean getMetracerAgentMxBean() throws IOException, MalformedObjectNameException {
		agentMxBeanName = new ObjectName(com.develorium.metracer.dynamic.Agent.MxBeanName);

		if(!connection.isRegistered(agentMxBeanName))
			return null;

		return JMX.newMXBeanProxy(connection, agentMxBeanName, AgentMXBean.class, true);
	}

	private String resolveMetracerAgentJar() throws UnsupportedEncodingException {
		String sourcePath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		return java.net.URLDecoder.decode(sourcePath, "UTF-8");
	}

	private static void say(String theMessage) {
		System.out.println(theMessage);
	}
}
