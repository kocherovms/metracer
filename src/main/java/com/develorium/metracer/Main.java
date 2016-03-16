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
	Config config = null;
	MBeanServerConnection connection = null;
	ObjectName agentMxBeanName = null;
	AgentMXBean agent = null;

	public static void main(String[] theArguments) {
		new Main().execute(theArguments);
	}

	private void execute(String[] theArguments) {
		try {
			config = new Config(theArguments);
			
			if(executeAuxCommands())
				return;

			if(loadAgent())
				configureAgent();

			if(config.command == Config.COMMAND.INSTRUMENT) {
				startListeningToAgentEvents();
				waitForever();
			}
		} catch(Config.BadConfig e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(1);
		} catch(Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private boolean executeAuxCommands() {
		switch(config.command) {
		case HELP:
			printHelp();
			return true;
		case LIST:
			printJvmList();
			return true;
		}

		return false;
	}

	private void printHelp() {
		try {
			String usage = loadInfoResource("usage.txt");
			String help = loadInfoResource("help.txt");
			String processedHelp = help.replace("${usage}", usage);
			System.out.println(processedHelp);
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to print help: %s", t.getMessage()), t);
		}
	}

	private void printJvmList() {
		List<VirtualMachineDescriptor> jvmList = VirtualMachine.list();

		for(VirtualMachineDescriptor jvm: jvmList) {
			System.out.println(String.format("%s\t%s", jvm.id(), jvm.displayName()));
		}
	}

	private void printUsage() {
		try {
			System.out.println(loadInfoResource("usage.txt"));
		} catch(Throwable t) {
			throw new RuntimeException(String.format("Failed to print usage: %s", t.getMessage()), t);
		}
	}

	private String loadInfoResource(String theResourceId) {
		try {
			ClassLoader loader = getClass().getClassLoader();
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

	private boolean loadAgent() {
		String exceptionMessage = "";

		try {
			exceptionMessage = String.format("Failed to connect to JVM with PID %d", config.pid);
			VirtualMachine vm = VirtualMachine.attach(Integer.toString(config.pid));

			try {
				say(String.format("Attached to JVM (PID %d)", config.pid));

				exceptionMessage = "Failed to ensure management agent is running";
				String jmxLocalConnectAddress = ensureManagementAgentIsRunning(vm);

				if(jmxLocalConnectAddress == null && config.command == Config.COMMAND.DEINSTRUMENT) {
					say("Management agent is not running but this is ok for removal, nothing to do");
					return false;
				}

				say("Management agent is running");

				exceptionMessage = "Failed to connect to MBean server";
				connection = connectToMbeanServer(jmxLocalConnectAddress);
				say("Connected to MBean server");

				exceptionMessage = "Failed to ensure metracer agent is running";
				agent = ensureMetracerAgentIsRunning(vm);

				if(agent == null && config.command == Config.COMMAND.DEINSTRUMENT) {
					say("metracer agent is not running but this is ok for removal, nothing to do");
					return false;
				}

				say("metracer agent is running");
				return true;
			} finally {
				vm.detach(); 
				say(String.format("Detached from JVM (PID %d)", config.pid));
			}
		} catch(Throwable e) {
			throw new RuntimeException(String.format("%s: %s", exceptionMessage, e.getMessage()), e);
		}
	}

	private void configureAgent() {
		agent.setIsVerbose(config.isVerbose);

		if(config.command == Config.COMMAND.DEINSTRUMENT) {
			int[] counters = agent.removePatterns();
			say("Patterns removed");

			if(counters != null && counters.length == 2)
				say(String.format("%d classes deinstrumented ok, %d failed", counters[0], counters[1]));
		}
		else {
			int[] counters = agent.setPatterns(config.classMatchingPattern, config.methodMatchingPattern, config.isWithStackTrace);
			say(String.format("Class matching pattern set to \"%s\"", config.classMatchingPattern));

			if(config.methodMatchingPattern != null)
				say(String.format("Method matching pattern set to \"%s\"", config.methodMatchingPattern));

			if(config.isWithStackTrace)
				say("Stack traces enabled");

			if(counters != null && counters.length == 2)
				say(String.format("%d classes instrumented ok, %d failed", counters[0], counters[1]));
		}
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

	private String ensureManagementAgentIsRunning(VirtualMachine theVm) throws Exception {
		String address = getJmxLocalConnectAddress(theVm);

		if(address == null && config.command == Config.COMMAND.DEINSTRUMENT)
			return null;
		else if(address != null)
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

		if(agent == null && config.command == Config.COMMAND.DEINSTRUMENT)
			return null;
		else if(agent != null) 
			return agent;

		say("metracer agent is not loaded");
		String metracerAgentFileName = resolveMetracerAgentJar();
			
		if(!new File(metracerAgentFileName).exists())
			throw new Exception(String.format("Resolved metracer jar \"%s\" doesn't exist", metracerAgentFileName));

		say(String.format("Loading metracer agent from \"%s\"", metracerAgentFileName));
		theVm.loadAgent(metracerAgentFileName, config.isVerbose ? "-v" : null);
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

	private void say(String theMessage) {
		if(!config.isVerbose)
			return;

		System.out.println(theMessage);
	}
}
