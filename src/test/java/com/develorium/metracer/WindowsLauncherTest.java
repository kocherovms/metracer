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
import org.junit.Assert;
import org.junit.Test;
import org.junit.*;

public class WindowsLauncherTest {
	@Test
	public void testExtractFieldValue() {
		String[] samples = {
			"PID:\t123", "123",
			"PID:\t\t123\t", "123",
			"PID:  123  ", "123",
			"PID:\t  123  \t", "123",
			":123", "123",
			"PID:", null,
			"PID:123", "123",	
			"PID:1", "1",
			"User name: NT AUTHORITY\\SYSTEM", "NT AUTHORITY\\SYSTEM",
			"User name: VIRTUALPC\\вася", "VIRTUALPC\\вася",
			"Пользователь: NT AUTHORITY\\SYSTEM", "NT AUTHORITY\\SYSTEM",
			"Пользователь: VIRTUALPC\\вася", "VIRTUALPC\\вася",
		};

		for(int i = 0; i < samples.length; i += 2) {
			String field = samples[i];
			String value = samples[i + 1];
			String resolvedValue = WindowsLauncher.extractFieldValue(field);
			System.out.format("%s => %s VS %s\n", field, resolvedValue, value);
			Assert.assertEquals(value, resolvedValue);
		}
	}
	@Test
	public void testProcessTaskListOutput() throws IOException {
		String[] samples = {
			"Имя образа:     tasklist.exe\n" + 
			"PID:            1188\n" +
			"Имя сессии:     Console\n" +
			"№ сеанса:       0\n" +
			"Память:         3 464 КБ\n" +
			"Статус:         Работает\n" +
			"Пользователь:   VIRTUALPC\\вася\n" +
			"Время ЦП:       0:00:00\n" +
			"Заголовок окна: OleMainThreadWndName\n", "1188", "VIRTUALPC\\вася",

			"Имя образа:     cmd.exe\n" +
			"PID:            1528\n" +
			"Имя сессии:     Console\n" +
			"№ сеанса:       0\n" +
			"Память:         232 КБ\n" +
			"Статус:         Работает\n" +
			"Пользователь:   VIRTUALPC\\Администратор\n" +
			"Время ЦП:       0:00:00\n" +
			"Заголовок окна: cmd (запущено от имени VIRTUALPC\\Администратор) - java -cp target\test-classes com.develorium.metracertest.Main\n", "1528", "VIRTUALPC\\Администратор",

			"\n" +
			"Имя образа:     System\n" +
			"PID:            4\n" +
			"Имя сессии:     Console\n" +
			"№ сеанса:       0\n" +
			"Память:         212 КБ\n" +
			"Статус:         Работает\n" +
			"Пользователь:   NT AUTHORITY\\SYSTEM\n" +
			"Время ЦП:       0:00:36\n" +
			"Заголовок окна: Н/Д\n", "4", "NT AUTHORITY\\SYSTEM",

			"Image Name:   conhost.exe\n" +
			"PID:          4396\n" +
			"Session Name: Services\n" +
			"Session#:     0\n" +
			"Mem Usage:    2,856 K\n" +
			"Status:       Unknown\n" +
			"User Name:    NT AUTHORITY\\SYSTEM\n" +
			"CPU Time:     0:00:00\n" +
			"Window Title: N/A\n", "4396", "NT AUTHORITY\\SYSTEM",

			"Image Name:   mytask.exe\n" +
			"PID:          5555\n" +
			"Session Name: Services\n" +
			"Session#:     0\n" +
			"Mem Usage:    2,856 K\n" +
			"Status:       Unknown\n" +
			"User Name:    NT AUTHORITY\\SYSTEM\n" +
			"CPU Time:     0:00:00\n" +
			"Window Title: \"Hello, world: this is me!\"\n", "5555", "NT AUTHORITY\\SYSTEM",
		};
	
		for(int i = 0; i < samples.length; i += 3) {
			String fields = samples[i];
			String pid = samples[i + 1];
			String userName = samples[i + 2];
			BufferedReader reader = new BufferedReader(new StringReader(fields));
			String resolvedUserName = WindowsLauncher.processTaskListOutput(reader, pid);
			System.out.format("%s => %s => %s VS %s\n", fields, pid, resolvedUserName, userName);
			Assert.assertEquals(userName, resolvedUserName);
		}
	}
	@Test
	public void testResolveUserNameOfTargetJvm_impl() throws IOException {
		if(!isWindows()) {
			System.out.println("Skipping test testResolveUserNameOfTargetJvm_impl - must be executed under Windows only");
			return;
		}
		// Check 0 ("System Idle Process") - hope it always exists
		String userName = WindowsLauncher.resolveUserNameOfTargetJvm_impl(0);
		System.out.println(String.format("Resolved user name of PID 0: %s", userName));
		Assert.assertTrue(userName != null);
		Assert.assertTrue(userName.length() > 0);
	}
	
	private static boolean isWindows() {
		String osName = System.getProperty("os.name");
		return osName.startsWith("Windows");
	}
}
