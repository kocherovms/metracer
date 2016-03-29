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

import java.lang.*;
import java.util.*;
import java.io.*;
import org.junit.Test;
import junit.framework.Assert;

public class MainIT {
	private static final String TestProgramMainClassName = "com.develorium.metracertest.Main";

	private abstract static class Scenario extends InputStream {
		public ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		public ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		public Date startTime = new Date();

		public abstract String[] getLaunchArguments();

		public int getTimeout() {
			return 10000;
		}

		public int process() {
			return 0;
		}

		public long getDuration() {
			Date currentTime = new Date();
			return currentTime.getTime() - startTime.getTime();
		}

		@Override
		public int read() {
			if(getDuration() > getTimeout())
				throw new RuntimeException("Scenario timed out");

			try {
				Thread.currentThread().sleep(1000);
			} catch(InterruptedException e) {
			}

			return process();
		}

		public void dump() {
			System.out.format("Captured stdout output:\n%s\n", stdout.toString());
			System.out.format("Captured stderr output:\n%s\n", stderr.toString());
		}
	}

	@Test
	public void test() throws Throwable {
		Process p = null;
		try {
			p = launchTestProgram();
			final String pid = testJvmListing();
			System.out.println("");

			testInstrumentAndQuitWithRemoval(pid);
			System.out.println("");

			testInstrumentAndQuitWithoutRemoval(pid);
			System.out.println("");

			testInstrumentAndQuitWithoutRemovalWithConsequentRemoval(pid);
			System.out.println("");

			testInstrumentationOutput(pid);
			System.out.println("");

			p.destroy();
			System.out.println("Sample program destroyed");
		} catch(Exception e) {
			if(p != null)
				p.destroy();

			throw new RuntimeException(e);
		}
	}

	private static Process launchTestProgram() throws IOException {
		String[] args = {
			"java",
			"-cp",
			String.format("%s/target/test-classes", System.getProperty("basedir")),
			TestProgramMainClassName
		};
		Process p =java.lang.Runtime.getRuntime().exec(args);
		// wait for program to start
		String magicMessage = "kms@ sample program started";
		InputStream stdout = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		String line = null;

		while((line = reader.readLine ()) != null) {
			if(line.startsWith(magicMessage)) {
				System.out.println("Sample program started");
				return p;
			}
		}

		throw new RuntimeException("Failed to wait for a magic message is printed: process failed to start?");
	}

	private static class JvmListingScenario extends Scenario {
		@Override
		public String[] getLaunchArguments() {
			return new String[] { "-l" };
		}
	}

	private String testJvmListing() {
		Scenario scenario = new JvmListingScenario(); 
		runMetracerScenario(scenario);
		String capturedOutput = scenario.stdout.toString();
		System.out.format("Captured output is:\n%s\n", capturedOutput);
		String pid = null;

		for (String line: capturedOutput.split("\n", 1000)){
			if(line.contains(TestProgramMainClassName)) {
				Scanner scanner = new Scanner(line);
				System.out.format("Searching for PID within \"%s\"\n", line);
				Assert.assertTrue(scanner.hasNextInt());
				pid = "" + scanner.nextInt();
				break;
			}
		}

		Assert.assertTrue(pid != null && !pid.isEmpty());
		System.out.format("Resolved PID is %s\n", pid);
		return pid;
	}
	
	private static class InstrumentAndQuitWithRemovalScenario extends Scenario {
		private String pid = null;

		public InstrumentAndQuitWithRemovalScenario(String thePid) {
			pid = thePid;
		}

		@Override
		public String[] getLaunchArguments() {
			return new String[] { "-v",  pid, "com.develorium.metracertest.Main" };
		}

		@Override	
		public int process() {
			String capturedStderr = stderr.toString();

			for(String line : capturedStderr.split("\n")) {
				if(line.contains("classes instrumented")) {
					System.out.format("Captured instrumentation results: %s\n", line);
					Assert.assertFalse(line.startsWith("0 methods"));
					return 'q';
				}
			}

			return 0;
		}
	}

	private void testInstrumentAndQuitWithRemoval(String thePid) {
		Scenario scenario = new InstrumentAndQuitWithRemovalScenario(thePid);
		runMetracerScenario(scenario);
		String capturedStderr = scenario.stderr.toString();
		Assert.assertTrue(capturedStderr.contains("Quitting with removal of instrumentation"));

		for(String line : capturedStderr.split("\n")) {
			if(line.contains("classes deinstrumented")) {
				System.out.format("Captured deinstrumentation results: %s\n", line);
				Assert.assertFalse(line.startsWith("0 methods"));
				return;
			}
		}

		Assert.assertTrue(false); // deinstrumentation must succeed
	}
	
	public static class InstrumentAndQuitWithoutRemovalScenario extends Scenario {
		private String pid = null;

		public InstrumentAndQuitWithoutRemovalScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  pid, "com.develorium.metracertest.Main" };
		}

		@Override	
		public int process() {
			String capturedStderr = stderr.toString();

			for(String line : capturedStderr.split("\n")) {
				if(line.contains("classes instrumented")) {
					System.out.format("Captured instrumentation results: %s\n", line);
					return 'Q';
				}
			}

			return 0;
		}
	}

	private void testInstrumentAndQuitWithoutRemoval(String thePid) {
		Scenario scenario = new InstrumentAndQuitWithoutRemovalScenario(thePid);
		runMetracerScenario(scenario);
		String capturedStderr = scenario.stderr.toString();
		Assert.assertTrue(capturedStderr.contains("Quitting with retention of instrumentation"));
		Assert.assertFalse(capturedStderr.contains("classes deinstrumented"));
	}

	public static class RemoveInstrumentationScenario extends Scenario {
		private String pid = null;

		public RemoveInstrumentationScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-r",  "-v", pid };
		}
	}

	private void testInstrumentAndQuitWithoutRemovalWithConsequentRemoval(String thePid) throws Throwable {
		Scenario cleanerScenario = new RemoveInstrumentationScenario(thePid);
		Scenario instrumentationWithRetentionScenario = new InstrumentAndQuitWithoutRemovalScenario(thePid);
		runMetracerScenario(cleanerScenario); // to get rid of possible leftovers from previous scenarios
		runMetracerScenario(instrumentationWithRetentionScenario);
		cleanerScenario = new RemoveInstrumentationScenario(thePid); // recreated to have a clean stdout / stderr
		runMetracerScenario(cleanerScenario);

		try {
			String capturedStderr = cleanerScenario.stderr.toString();

			for(String line : capturedStderr.split("\n")) {
				if(line.contains("classes deinstrumented")) {
					System.out.format("Captured deinstrumentation results: %s\n", line);
					Assert.assertFalse(line.startsWith("0 methods"));
					return;
				}
			}

			Assert.assertTrue(false); // deinstrumentation must succeed
		} catch(Throwable e) {
			cleanerScenario.dump();
			throw e;
		}
	}

	public static class InstrumentationOutputScenario extends Scenario {
		private String pid = null;
		private boolean isEntryTag = false;	

		public InstrumentationOutputScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  pid, "com.develorium.metracertest.Main", "doSomething" };
		}

		@Override	
		public int process() {
			String capturedStdout = stdout.toString();

			for(String line : capturedStdout.split("\n")) {
				if(!isEntryTag) {
					if(line.contains("+++ [0] com.develorium.metracertest.Main.doSomething")) {
						System.out.println("Entry tag met: " + line);
						isEntryTag = true;
					}
				}
				else {
					if(line.contains("--- [0] com.develorium.metracertest.Main.doSomething")) {
						System.out.println("Exit tag met: " + line);
						return 'q';
					}
				}
			}

			return 0;
		}
	}


	private void testInstrumentationOutput(final String thePid) {
		Scenario scenario = new InstrumentationOutputScenario(thePid);
		runMetracerScenario(scenario);
	}

	private void runMetracerScenario(Scenario theScenario) {
		String[] launchArguments = theScenario.getLaunchArguments();
		StringBuilder launchArgumentsStringified = new StringBuilder();

		for(String launchArgument: launchArguments)
			launchArgumentsStringified.append(launchArgument + " ");

		System.out.format(">>> Launching scenario %s with arguments: %s\n", theScenario.getClass().getSimpleName(), launchArgumentsStringified.toString());
		try {
			try {
				Main.main(launchArguments, theScenario, new PrintStream(theScenario.stdout), new PrintStream(theScenario.stderr));
			} catch(Throwable e) {
				theScenario.dump();
				throw new RuntimeException(e);
			}
		} finally {
			System.out.format("<<< Scenario %s finished in %d ms\n", theScenario.getClass().getSimpleName(), theScenario.getDuration());
		}
	}
}
