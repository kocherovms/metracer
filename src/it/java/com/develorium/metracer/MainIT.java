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

	private abstract class Scenario extends InputStream {
		public ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		public ByteArrayOutputStream stderr = new ByteArrayOutputStream();

		public abstract String[] getLaunchArguments();

		public int process() {
			return 0;
		}

		@Override
		public int read() {
			try {
				Thread.currentThread().sleep(1000);
			} catch(InterruptedException e) {
			}

			return process();
		}
	}

	@Test
	public void testClassMatchingPattern() {
		Process p = null;
		try {
			p = launchTestProgram();
			Scenario jvmListingScenario = new Scenario() {
				@Override
				public String[] getLaunchArguments() {
					return new String[] { "-l" };
				}
			};
			runMetracerScenario(jvmListingScenario);
			String capturedOutput = jvmListingScenario.stdout.toString();
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
			final String finalizedPid = pid;
			Scenario instrumentScenario = new Scenario() {
				@Override	
				public String[] getLaunchArguments() {
					return new String[] { "-v",  finalizedPid, "com.develorium.metracertest.Main" };
				}

				@Override	
				public int process() {
					String capturedStderr = stderr.toString();
					String[] lines = capturedStderr.split("\n");

					for(String line : lines) {
						if(line.contains("classes instrumented")) {
							System.out.format("Captured instrumentation results: %s\n", line);
							Assert.assertFalse(line.startsWith("0 classes"));
							return 'q';
						}
					}

					return 0;
				}
			};

			runMetracerScenario(instrumentScenario);
			p.destroy();
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

	private void runMetracerScenario(Scenario theScenario) {
		String[] launchArguments = theScenario.getLaunchArguments();
		StringBuilder launchArgumentsStringified = new StringBuilder();

		for(String launchArgument: launchArguments)
			launchArgumentsStringified.append(launchArgument + " ");

		System.out.format("Launching scenario with arguments: %s\n", launchArgumentsStringified.toString());
		Main.main(launchArguments, theScenario, new PrintStream(theScenario.stdout), new PrintStream(theScenario.stderr));
	}
}
