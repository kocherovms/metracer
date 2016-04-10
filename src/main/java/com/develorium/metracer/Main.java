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
	Environment env = new SystemEnvironment();
	Config config = null;
	MBeanServerConnection connection = null;
	ObjectName agentMxBeanName = null;
	AgentMXBean agent = null;
	PatternsFile outputPatternsFile = null;
	boolean isFinished = false;

	public static void main(String[] theArguments) {
		try {
			new Main().execute(theArguments);
		} catch(Throwable e) {
			System.exit(1);
		}
	}

	public static void main(String[] theArguments, Environment theEnvironment) throws Throwable {
		new Main(theEnvironment).execute(theArguments);
	}

	public Main() {
	}

	public Main(Environment theEnvironment) {
		env = theEnvironment;
	}

	private void execute(String[] theArguments) throws Throwable {
		try {
			config = new Config(theArguments);

			if(Aux.executeAuxCommands(config.command, env.getStdout()))
				return;

			executeCommands();
			isFinished = true;
		} catch(Config.BadConfig e) {
			env.getStderr().println(e.getMessage());
			Aux.printUsage(env.getStderr());
			throw e;
		} catch(Throwable e) {
			env.getStderr().println(e.getMessage());
			e.printStackTrace(env.getStderr());
			throw e;
		}
	}

	private void executeCommands() {
		switch(config.command) {
		case INSTRUMENT:
			executeInstrument();
			break;
		case DEINSTRUMENT:
			executeDeinstrument();
			break;
		}
	}

	private void executeInstrument() {
		loadAgent(true);
		agent.setIsVerbose(config.isVerbose);
		String effectiveClassMatchingPattern = config.classMatchingPattern;
		String effectiveMethodMatchingPattern = config.methodMatchingPattern;

		if(config.patternsFileName != null) {
			say(String.format("Loading patterns from \"%s\"", config.patternsFileName));
			try {
				FileInputStream inputStream = new FileInputStream(config.patternsFileName);
				PatternsFile inputStreamFile = new PatternsFile(inputStream);
				effectiveClassMatchingPattern = inputStreamFile.getClassMatchingPattern();
				effectiveMethodMatchingPattern = inputStreamFile.getMethodMatchingPattern();
			} catch(FileNotFoundException e) {
				throw new RuntimeException(String.format("Failed to open \"%s\" for reading: %s", config.patternsFileName, e.getMessage()), e);
			} catch(IOException e) {
				throw new RuntimeException(String.format("Failed to read patterns from \"%s\": %s", config.patternsFileName, e.getMessage()), e);
			}
		}

		if(effectiveClassMatchingPattern == null) {
			say("Not setting any patterns, using ones from a previous session");
		}
		else {
			if(config.isWithStackTrace && config.stackTraceFileName != null) {
				try {
					FileOutputStream outputStream = new FileOutputStream(config.stackTraceFileName);
					outputPatternsFile = new PatternsFile(outputStream);
				} catch(FileNotFoundException e) {
					throw new RuntimeException(String.format("Failed to open \"%s\" for writing: %s", config.stackTraceFileName, e.getMessage()), e);
				}
			}

			StackTraceMode stackTraceMode = config.isWithStackTrace 
				? (config.stackTraceFileName != null 
				   ? StackTraceMode.PRINT_AND_REPORT
				   : StackTraceMode.PRINT)
				: StackTraceMode.DISABLED;
			byte[] encodedCounters = agent.setPatterns(effectiveClassMatchingPattern, effectiveMethodMatchingPattern, stackTraceMode);
			say(String.format("Class matching pattern set to \"%s\"", effectiveClassMatchingPattern));
			
			if(effectiveMethodMatchingPattern != null)
				say(String.format("Method matching pattern set to \"%s\"", effectiveMethodMatchingPattern));
			
			if(config.isWithStackTrace)
				say("Stack traces enabled");

			sayCounters(encodedCounters, "instrument");
		}

		say("Press 'q' to quit with removal of instrumentation, 'Q' - to quit with retention of instrumentation in target JVM");
		startListeningToAgentEvents();

		if(!Aux.waitForQuit(env.getStdin(), env.getStderr())) {
			say("Quitting with retention of instrumentation in target JVM");
			return;
		}

		say("Quitting with removal of instrumentation in target JVM");
		deinstrument();
	}

	private void executeDeinstrument() {
		if(!loadAgent(false))
			return;

		agent.setIsVerbose(config.isVerbose);
		deinstrument();
	}

	private boolean loadAgent(boolean theIsAgentRequired) {
		String exceptionMessage = "";

		try {
			exceptionMessage = String.format("Failed to connect to JVM with PID %d", config.pid);
			VirtualMachine vm = VirtualMachine.attach(Integer.toString(config.pid));

			try {
				say(String.format("Attached to JVM (PID %d)", config.pid));

				exceptionMessage = "Failed to ensure management agent is running";
				String jmxLocalConnectAddress = ensureManagementAgentIsRunning(vm, theIsAgentRequired);

				if(jmxLocalConnectAddress == null && !theIsAgentRequired) {
					say("Management agent is not running but this is ok for removal, nothing to do");
					return false;
				}

				say("Management agent is running");

				exceptionMessage = "Failed to connect to MBean server";
				connection = connectToMbeanServer(jmxLocalConnectAddress);
				say("Connected to MBean server");

				exceptionMessage = "Failed to ensure metracer agent is running";
				agent = ensureMetracerAgentIsRunning(vm, theIsAgentRequired);

				if(agent == null && !theIsAgentRequired) {
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

	private void deinstrument() {
		byte[] encodedCounters = agent.removePatterns();
		say("Patterns removed");
		sayCounters(encodedCounters, "deinstrument");
	}

	private void startListeningToAgentEvents() {
		try {
			connection.addNotificationListener(agentMxBeanName, new NotificationListener() {
					@Override
					public void handleNotification(Notification theNotification, Object theHandback) {
						String notificationType = theNotification.getType();

						if(notificationType.equals(JmxNotificationTypes.EntryExitNotificationType)) 
							env.getStdout().println(theNotification.getMessage());
						else if(notificationType.equals(JmxNotificationTypes.StackTraceNotificationType) && outputPatternsFile != null)
							try {
								outputPatternsFile.consumePatterns(theNotification.getMessage());
							} catch(IOException e) {
								env.getStderr().format("Failed to write new patterns: %s\n", e.getMessage());
							}
					}
				}, 
				null, null);
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to start listening to agent events: %s", e.getMessage()), e);
		}
	}

	private String ensureManagementAgentIsRunning(VirtualMachine theVm, boolean theIsAgentRequired) throws Exception {
		String address = getJmxLocalConnectAddress(theVm);

		if(address == null && !theIsAgentRequired)
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
		NotificationListener connectionListener = new NotificationListener() {
			@Override
			public void handleNotification(Notification theNotification, Object theHandback) {
				if(isFinished) 
					return;

				if(theNotification.getType().equals("jmx.remote.connection.failed")) {
					say("Aborting: connection failed");
					env.exit(2);
				}
				else if(theNotification.getType().equals("jmx.remote.connection.closed")) {
					say("Aborting: connection closed");
					env.exit(3);
				}
			}
		};
		jmxConnector.addConnectionNotificationListener(connectionListener, null, null);
		return jmxConnector.getMBeanServerConnection();
	}

	private AgentMXBean ensureMetracerAgentIsRunning(VirtualMachine theVm, boolean theIsAgentRequired) throws Exception {
		agent = getMetracerAgentMxBean();

		if(agent == null && !theIsAgentRequired)
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


	private void sayCounters(byte[] theEncodedCounters, String theVerb) {
		if(!config.isVerbose) 
			return;

		try {
			if(theEncodedCounters == null) 
				say(String.format("No methods were %sed", theVerb));
			else {
				AgentMXBean.Counters counters = AgentMXBean.Counters.deserialize(theEncodedCounters);
				String failMessage = counters.failedClassesCount > 0 
					? String.format(", %sation failed for %d classes", theVerb, counters.failedClassesCount) 
					: "";
				say(String.format("%d methods in %d classes %sed%s", 
						counters.methodsCount, 
						counters.classesCount, 
						theVerb,
						failMessage));
			}
		} catch(Throwable e) {
			say(String.format("Failed to get counters: %s", e.getMessage()));
		}
	}

	private void say(String theMessage) {
		if(!config.isVerbose)
			return;

		env.getStderr().println(theMessage);
	}
}
