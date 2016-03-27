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
import java.io.*;
import org.junit.Test;
import junit.framework.Assert;

public class MainIT {
	private static final String SampleProgramMainClassName = "com.develorium.metracer.sample.Main";
	@Test
	public void testJvmListing() {
		Process p = null;
		try {
			p = launchSample();

			PrintStream stdout = System.out;
			ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
			try {
				System.setOut(new PrintStream(capturedOutput));
				new Main().main(new String[] { "-l" } );
			} finally {
				System.setOut(stdout);
			}

			System.out.format("Captured output is:\n%s\n", capturedOutput.toString());
			Assert.assertTrue(capturedOutput.toString().contains(SampleProgramMainClassName));
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
}
