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
import java.util.regex.*;
import org.junit.*;
import junit.framework.Assert;

public class BaseClass {
	protected static final String TestProgramMainClassName = "com.develorium.metracertest.Main";
	protected static Process testProgramProcess = null;
	protected static String pid = null;

	protected abstract static class Scenario extends InputStream implements Environment {
		public String pid = null;
		public ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		public ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		public Date startTime = new Date();
		public Integer exitCode = null;
		private PrintStream stdoutStream = new PrintStream(stdout);
		private PrintStream stderrStream = new PrintStream(stderr);
		private int[] extractedStdoutSize = { 0 };
		private int[] extractedStderrSize = { 0 };

		public abstract String[] getLaunchArguments();

		public int process() {
			return 0;
		}

		public long getDuration() {
			Date currentTime = new Date();
			return currentTime.getTime() - startTime.getTime();
		}

		@Override
		public int read() {
			try {
				Thread.currentThread().sleep(50);
			} catch(InterruptedException e) {
			}

			return process();
		}

		@Override
		public InputStream getStdin() {
			return this;
		}

		@Override
		public PrintStream getStdout() {
			return stdoutStream;
		}

		@Override
		public PrintStream getStderr() {
			return stderrStream;
		}

		@Override
		public void exit(int theExitCode) {
			exitCode = theExitCode;
		}

		public void printNewStdout() {
			String newStdout = getNewPortionOfBuffer(stdout, extractedStdoutSize);

			if(!newStdout.isEmpty())
				System.out.println(newStdout);
		}

		public void printNewStderr() {
			String newStderr = getNewPortionOfBuffer(stderr, extractedStderrSize);

			if(!newStderr.isEmpty())
				System.out.println(newStderr);
		}

		public void dump() {
			System.out.format("Captured stdout output:\n%s\n", stdout.toString());
			System.out.format("Captured stderr output:\n%s\n", stderr.toString());
		}

		private String getNewPortionOfBuffer(ByteArrayOutputStream theBuffer, int[] theExtractedSize) {
			String text = theBuffer.toString();
			
			if(text.length() <= theExtractedSize[0])
				return "";

			String rv = text.substring(extractedStdoutSize[0]);
			theExtractedSize[0] = text.length();
			return rv;
		}
	}

	protected static class JvmListingScenario extends Scenario {
		@Override
		public String[] getLaunchArguments() {
			return new String[] { "-l" };
		}
	}

	@BeforeClass 
	public static void launchTestProgram() throws Throwable {
		String[] args = {
			"java",
			"-cp",
			String.format("%s/target/test-classes", System.getProperty("basedir")),
			TestProgramMainClassName
		};
		Process p = java.lang.Runtime.getRuntime().exec(args);
		// wait for program to start
		String magicMessage = "kms@ sample program started";
		InputStream stdout = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		String line = null;

		while((line = reader.readLine()) != null) {
			if(line.startsWith(magicMessage)) {
				System.out.println("Test program started");
				testProgramProcess = p;
				break;
			}
		}

		Assert.assertTrue(testProgramProcess != null);
		Scenario scenario = new JvmListingScenario(); 
		runMetracerScenario(scenario);
		String capturedOutput = scenario.stdout.toString();
		System.out.println(capturedOutput);

		for (String capturedOutputLine : capturedOutput.split("\n", 1000)){
			if(capturedOutputLine.contains(TestProgramMainClassName)) {
				if(pid != null) 
					throw new RuntimeException(String.format("More that one running test program detected (%s): can't decide to which to connect to", TestProgramMainClassName));

				Scanner scanner = new Scanner(capturedOutputLine);
				System.out.format("Searching for PID within \"%s\"\n", capturedOutputLine);
				Assert.assertTrue(scanner.hasNextInt());
				pid = "" + scanner.nextInt();
			}
		}

		Assert.assertTrue(pid != null && !pid.isEmpty());
		System.out.format("Resolved PID is %s\n\n", pid);
	}

	@AfterClass
	public static void shutdownTestProgram() throws Throwable {
		if(testProgramProcess != null) {
			testProgramProcess.destroy();
			testProgramProcess = null;
			System.out.println("Test program destroyed");
		}

		if(pid != null && !pid.isEmpty())
			System.out.format("Waiting for PID %s is gone\n", pid);

		int attemptsCount = 20; // 250 * 20 -> 5 seconds for a termination

		while(pid != null && !pid.isEmpty()) {
			Scenario scenario = new JvmListingScenario(); 
			runMetracerScenario(scenario);
			String capturedOutput = scenario.stdout.toString();
			System.out.println(capturedOutput);
			boolean isPidStillPresent = false;

			for (String capturedOutputLine : capturedOutput.split("\n", 1000)) {
				if(capturedOutputLine.contains(TestProgramMainClassName)) {
					Scanner scanner = new Scanner(capturedOutputLine);
					System.out.format("Searching for PID within \"%s\"\n", capturedOutputLine);
					Assert.assertTrue(scanner.hasNextInt());
					String localPid = "" + scanner.nextInt();
					isPidStillPresent = isPidStillPresent || localPid.equals(pid);
				}
			}

			if(!isPidStillPresent) {
				System.out.format("PID %s is gone, finishing test suite!\n", pid);
				pid = null;
			}
			else {
				Assert.assertTrue(--attemptsCount > 0);
				System.out.format("PID %s is still present, waiting\n", pid);
				try {
					Thread.currentThread().sleep(250);
				} catch(InterruptedException e) {
				}
			}
		}
	}

	@Before
	public void printStartSeparator() {
		System.out.println("------TEST STARTED------");
	}

	@After
	public void printEndSeparator() {
		System.out.println("------TEST FINISHED------\n");
	}

	protected static void runMetracerScenario(Scenario theScenario) throws Throwable {
		String[] launchArguments = theScenario.getLaunchArguments();
		StringBuilder launchArgumentsStringified = new StringBuilder();

		for(String launchArgument: launchArguments)
			launchArgumentsStringified.append(launchArgument + " ");

		System.out.format(">>> Launching scenario %s with arguments: %s\n", theScenario.getClass().getSimpleName(), launchArgumentsStringified.toString());
		boolean isFinishedOk = false;

		try {
			Main.main(launchArguments, theScenario);
			isFinishedOk = true;
		} finally {
			if(!isFinishedOk)
				theScenario.dump();

			System.out.format("<<< Scenario %s finished in %d ms\n", theScenario.getClass().getSimpleName(), theScenario.getDuration());
		}
	}
}
