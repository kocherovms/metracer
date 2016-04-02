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
import org.junit.*;
import junit.framework.Assert;

public class MainIT {
	private static final String TestProgramMainClassName = "com.develorium.metracertest.Main";
	private static Process testProgramProcess = null;
	private static String pid = null;

	private abstract static class Scenario extends InputStream {
		public ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		public ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		public Date startTime = new Date();
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

	private static class JvmListingScenario extends Scenario {
		@Override
		public String[] getLaunchArguments() {
			return new String[] { "-l" };
		}
	}

	@BeforeClass 
	public static void launchTestProgram() throws IOException {
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
		int attempsCount = 3;

		while(attempsCount-- > 0 && (pid == null || pid.isEmpty())) {
			Scenario scenario = new JvmListingScenario(); 
			runMetracerScenario(scenario);
			String capturedOutput = scenario.stdout.toString();
			System.out.println(capturedOutput);

			for (String capturedOutputLine : capturedOutput.split("\n", 1000)){
				if(capturedOutputLine.contains(TestProgramMainClassName)) {
					Scanner scanner = new Scanner(capturedOutputLine);
					System.out.format("Searching for PID within \"%s\"\n", capturedOutputLine);
					Assert.assertTrue(scanner.hasNextInt());
					pid = "" + scanner.nextInt();
					break;
				}
			}

			try {
				Thread.currentThread().sleep(1000);
			} catch(InterruptedException e) {
			}
		}

		Assert.assertTrue(pid != null && !pid.isEmpty());
		System.out.format("Resolved PID is %s\n\n", pid);
	}

	@AfterClass
	public static void shutdownTestProgram() throws IOException {
		if(testProgramProcess != null) {
			testProgramProcess.destroy();
			testProgramProcess = null;
			System.out.println("Test program destroyed");
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
			printNewStderr();

			for(String line : stderr.toString().split("\n")) {
				if(line.contains("classes instrumented")) {
					System.out.format("Captured instrumentation results: %s\n", line);
					Assert.assertFalse(line.startsWith("0 methods"));
					Assert.assertFalse(line.contains(" failed for "));
					return 'q';
				}
			}

			return 0;
		}
	}

	@Test(timeout = 5000)
	public void testInstrumentAndQuitWithRemoval() {
		Scenario scenario = new InstrumentAndQuitWithRemovalScenario(pid);
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
			printNewStderr();

			for(String line : stderr.toString().split("\n")) {
				if(line.contains("classes instrumented")) {
					System.out.format("Captured instrumentation results: %s\n", line);
					return 'Q';
				}
			}

			return 0;
		}
	}

	@Test(timeout = 5000)
	public void testInstrumentAndQuitWithoutRemoval() {
		Scenario scenario = new InstrumentAndQuitWithoutRemovalScenario(pid);
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

	@Test(timeout = 5000)
	public void testInstrumentAndQuitWithoutRemovalWithConsequentRemoval() throws Throwable {
		Scenario cleanerScenario = new RemoveInstrumentationScenario(pid);
		Scenario instrumentationWithRetentionScenario = new InstrumentAndQuitWithoutRemovalScenario(pid);
		runMetracerScenario(cleanerScenario); // to get rid of possible leftovers from previous scenarios
		runMetracerScenario(instrumentationWithRetentionScenario);
		cleanerScenario = new RemoveInstrumentationScenario(pid); // recreated to have a clean stdout / stderr
		runMetracerScenario(cleanerScenario);

		try {
			String capturedStderr = cleanerScenario.stderr.toString();
			System.out.println(capturedStderr);

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
		private Pattern pattern = Pattern.compile(
			"\\+\\+\\+ \\[0\\] com.develorium.metracertest.Main.testBundle.*" +
			"\\+\\+\\+ \\[1\\] com.develorium.metracertest.Main.testIntRetVal.*" + 
			"\\-\\-\\- \\[1\\] com.develorium.metracertest.Main.testIntRetVal.*" +
			"\\-\\-\\- \\[0\\] com.develorium.metracertest.Main.testBundle.*", Pattern.DOTALL);

		public InstrumentationOutputScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  pid, "com.develorium.metracertest.Main", "testBundle|testIntRetVal" };
		}

		@Override	
		public int process() {
			printNewStdout();

			if(pattern.matcher(stdout.toString()).find()) 
				return 'q';

			return 0;
		}
	}

	@Test(timeout = 5000)
	public void testInstrumentationOutput() {
		Scenario scenario = new InstrumentationOutputScenario(pid);
		runMetracerScenario(scenario);
	}

	public static class InstrumentationOutputWithStackTracesScenario extends Scenario {
		private String pid = null;
		private Pattern pattern = Pattern.compile(
			"\\[0\\] com.develorium.metracertest.Main.testStackTrace4.*" +
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace4.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace3.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace2.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace1.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace0.*", Pattern.DOTALL);

		public InstrumentationOutputWithStackTracesScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  "-s", pid, "com.develorium.metracertest.Main", "testStackTrace4" };
		}

		@Override	
		public int process() {
			printNewStdout();

			if(pattern.matcher(stdout.toString()).find()) 
				return 'q';

			return 0;
		}
	}

	@Test(timeout = 5000)
	public void testInstrumentationOutputWithStackTraces() {
		Scenario scenario = new InstrumentationOutputWithStackTracesScenario(pid);
		runMetracerScenario(scenario);
	}

	public static class InstrumentationWithStackTracesSavingScenario extends Scenario {
		private String pid = null;
		private String fileName = null;
		private Pattern pattern = Pattern.compile(
			"\\[0\\] com.develorium.metracertest.Main.testStackTrace4.*" +
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace4.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace3.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace2.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace1.*" + 
			"\\s+at\\s+com.develorium.metracertest.Main.testStackTrace0.*", Pattern.DOTALL);

		public InstrumentationWithStackTracesSavingScenario(String thePid, String theFileName) {
			pid = thePid;
			fileName = theFileName;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  "-S", fileName, pid, "com.develorium.metracertest.Main", "testStackTrace4" };
		}

		@Override	
		public int process() {
			printNewStdout();

			if(pattern.matcher(stdout.toString()).find())
				return 'q';

			return 0;
		}
	}

	public static class InstrumentWithPatternsFromFileScenario extends Scenario {
		private String pid = null;
		private String fileName = null;
		private Pattern pattern = Pattern.compile(
			"\\[0\\] com.develorium.metracertest.Main.testStackTrace0.*" +
			"\\[1\\] com.develorium.metracertest.Main.testStackTrace1.*" + 
			"\\[2\\] com.develorium.metracertest.Main.testStackTrace2.*" + 
			"\\[3\\] com.develorium.metracertest.Main.testStackTrace3.*" + 
			"\\[4\\] com.develorium.metracertest.Main.testStackTrace4.*", Pattern.DOTALL);

		public InstrumentWithPatternsFromFileScenario(String thePid, String theFileName) {
			pid = thePid;
			fileName = theFileName;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  "-f", fileName, pid };
		}

		@Override	
		public int process() {
			printNewStdout();

			if(pattern.matcher(stdout.toString()).find())
				return 'q';

			return 0;
		}
	}

	@Test(timeout = 5000)
	public void testVerticalInstrumentation() {
		String fileName = String.format("%s/target/stacktrace.txt", System.getProperty("basedir"));
		Scenario saveStackTracesScenario = new InstrumentationWithStackTracesSavingScenario(pid, fileName);
		runMetracerScenario(saveStackTracesScenario);
		Scenario instrumentWithPatternsFromFileScenario = new InstrumentWithPatternsFromFileScenario(pid, fileName);
		runMetracerScenario(instrumentWithPatternsFromFileScenario);
	}

	private static void runMetracerScenario(Scenario theScenario) {
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
