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
	private static final String SampleProgramMainClassName = "com.develorium.metracer.sample.Main";

	private abstract class Scenario extends InputStream {
		public abstract String[] getLaunchArguments();
		public ByteArrayOutputStream capturingStream = null;

		public int process() {
			return 0;
		}

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
			p = launchSample();
			String capturedOutput = runMetracerScenario(new Scenario() {
				@Override	
				public String[] getLaunchArguments() {
					return new String[] { "-l" };
				}
			});

			System.out.format("Captured output is:\n%s\n", capturedOutput);
			String pid = null;

			for (String line: capturedOutput.split("\n", 1000)){
				if(line.contains(SampleProgramMainClassName)) {
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

			capturedOutput = runMetracerScenario(new Scenario() {
				@Override	
				public String[] getLaunchArguments() {
					return new String[] { "-v",  finalizedPid, "com.develorium.metracer.sample.Main" };
				}

				@Override	
				public int process() {
					if(capturingStream == null) 
						return 0;

					String capturedOutput = capturingStream.toString();
					System.err.println("kms@ capturedOutput = " + capturedOutput);

					if(capturedOutput.contains("classes instrumented")) {
						return 'q';
					}

					return 0;
				}
			});
			p.destroy();
		} catch(Exception e) {
			if(p != null)
				p.destroy();

			throw new RuntimeException(e);
		}
	}

	private static Process launchSample() throws IOException {
		String[] args = {
			"java",
			"-cp",
			String.format("%s/target/test-classes", System.getProperty("basedir")),
			SampleProgramMainClassName
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

	private String runMetracerScenario(Scenario theScenario) {
		PrintStream stdout = System.out;
		InputStream stdin = System.in;
		ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();

		try {
			System.setIn(theScenario);
			String[] launchArguments = theScenario.getLaunchArguments();
			StringBuilder launchArgumentsStringified = new StringBuilder();

			for(String launchArgument: launchArguments)
				launchArgumentsStringified.append(launchArgument + " ");

			System.out.format("Launching scenario with arguments: %s\n", launchArgumentsStringified.toString());

			System.setOut(new PrintStream(capturedOutput));
			theScenario.capturingStream = capturedOutput;
			new Main().main(launchArguments);
			return capturedOutput.toString();
		} finally {
			System.setIn(stdin);
			System.setOut(stdout);
		}
	}
}
