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

public class MainIT extends BaseClass {
	private static class InstrumentAndQuitWithRemovalScenario extends Scenario {
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
	public void testInstrumentAndQuitWithRemoval() throws Throwable {
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
		public InstrumentAndQuitWithoutRemovalScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { "-v",  pid, "com.develorium.metracertest.Main", "doSomething" };
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
	public void testInstrumentAndQuitWithoutRemoval() throws Throwable {
		Scenario scenario = new InstrumentAndQuitWithoutRemovalScenario(pid);
		runMetracerScenario(scenario);
		String capturedStderr = scenario.stderr.toString();
		Assert.assertTrue(capturedStderr.contains("Quitting with retention of instrumentation"));
		Assert.assertFalse(capturedStderr.contains("classes deinstrumented"));
	}

	public static class RemoveInstrumentationScenario extends Scenario {
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
	public void testInstrumentationOutput() throws Throwable {
		Scenario scenario = new InstrumentationOutputScenario(pid);
		runMetracerScenario(scenario);
	}

	public static class InstrumentationOutputWithStackTracesScenario extends Scenario {
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
	public void testInstrumentationOutputWithStackTraces() throws Throwable {
		Scenario scenario = new InstrumentationOutputWithStackTracesScenario(pid);
		runMetracerScenario(scenario);
	}

	public static class InstrumentationWithStackTracesSavingScenario extends Scenario {
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
		private String fileName = null;
		private Pattern pattern = Pattern.compile(
			"\\+\\+\\+ \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace0.*" +
			"\\+\\+\\+ \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace1.*" + 
			"\\+\\+\\+ \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace2.*" + 
			"\\+\\+\\+ \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace3.*" + 
			"\\+\\+\\+ \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace4.*" +
			"\\-\\-\\- \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace4.*" +
			"\\-\\-\\- \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace3.*" + 
			"\\-\\-\\- \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace2.*" + 
			"\\-\\-\\- \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace1.*" + 
			"\\-\\-\\- \\[\\d+\\] com.develorium.metracertest.Main.testStackTrace0.*", Pattern.DOTALL);

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
	public void testVerticalInstrumentation() throws Throwable {
		String fileName = String.format("%s/target/stacktrace.txt", System.getProperty("basedir"));
		Scenario saveStackTracesScenario = new InstrumentationWithStackTracesSavingScenario(pid, fileName);
		runMetracerScenario(saveStackTracesScenario);
		Scenario instrumentWithPatternsFromFileScenario = new InstrumentWithPatternsFromFileScenario(pid, fileName);
		runMetracerScenario(instrumentWithPatternsFromFileScenario);
	}

	public static class ReturnValuePrintingScenario extends Scenario {
		public ReturnValuePrintingScenario(String thePid) {
			pid = thePid;
		}

		@Override	
		public String[] getLaunchArguments() {
			return new String[] { pid, TestProgramMainClassName, "testReturnsMap" };
		}

		@Override	
		public int process() {
			printNewStdout();

			for(String line : stdout.toString().split("\n")) {
				if(line.contains("--- [0] com.develorium.metracertest.Main.testReturnsMap")) {
					Assert.assertTrue(line.contains("return:"));
					Assert.assertTrue(line.contains("2*2=>4"));
					Assert.assertTrue(line.contains("lorem=>ipsum"));
					Assert.assertTrue(line.contains("hello=>world"));
					return 'q';
				}
			}

			return 0;
		}
	}

	@Test(timeout = 5000)
	public void testReturnValuePrinting() throws Throwable {
		Scenario scenario = new ReturnValuePrintingScenario(pid);
		runMetracerScenario(scenario);
	}
}
