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

import java.io.*;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.*;
import org.apache.commons.csv.*;

public class WinMainTest {
	@Before
	public void runOnWindowsOnly() {
		String osName = System.getProperty("os.name");
		org.junit.Assume.assumeTrue(osName.startsWith("Windows"));
	}
	@Test
	public void testResolveUserNameOfPid() throws IOException {
		String[] samples = {
			"\"csrss.exe\",\"364\",\"Services\",\"0\",\"3,480 K\",\"Unknown\",\"NT AUTHORITY\\SYSTEM\",\"0:00:22\",\"N/A\"", "364", "NT AUTHORITY\\SYSTEM",
			"\"System\",\"4\",\"Console\",\"0\",\"212 КБ\",\"Работает\",\"NT AUTHORITY\\SYSTEM\",\"0:00:31\",\"Н/Д\"", "4", "NT AUTHORITY\\SYSTEM",
			"\"explorer.exe\",\"1584\",\"Console\",\"0\",\"18 432 КБ\",\"Работает\",\"VIRTUALPC\\вася\",\"0:01:43\",\"Н/Д\"", "1584", "VIRTUALPC\\вася",
			"\"some\", \"strange\", \"csv\"", "100", null,
			"\"csrss.exe\",\"364\",\"Services\",\"0\",\"3,480 K\",\"Unknown\",\"NT AUTHORITY\\SYSTEM\",\"0:00:22\",\"N/A\"", "zzzz", null,
		};

		for(int i = 0; i < samples.length; i += 3) {
			String line = samples[i];
			String pid = samples[i + 1];
			String userName = samples[i + 2];
			CSVParser parser = CSVParser.parse(line, CSVFormat.DEFAULT);
			String resolvedUserName = WinMain.resolveUserNameOfPid(parser, pid);
			System.out.format("%s => %s => %s VS %s\n", line, pid, resolvedUserName, userName);
			Assert.assertEquals(userName, resolvedUserName);
		}
	}
	@Test
	public void testResolveUserName() throws Config.BadConfig, IOException {
		// Check 0 ("System Idle Process") - hope it always exists
		String userName = WinMain.resolveUserName(new String[] { "0" });
		System.out.println(String.format("Resolved user name of PID 0: %s", userName));
		Assert.assertTrue(userName != null);
		Assert.assertTrue(userName.length() > 0);
	}
}
