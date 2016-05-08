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

public class LinuxLauncherTest {
	@Test
	public void testProcessStatusContent() throws IOException {
		String[] samples = {
			"Name:   bash\n" +
			"State:  S (sleeping)\n" +
			"Tgid:   9834\n" +
			"Pid:    9834\n" +
			"PPid:   9832\n" +
			"TracerPid:      0\n" +
			"Uid:    1000    1000    1000    1000\n" +
			"Gid:    1000    1000    1000    1000\n", "1000",

			"Name:   udisks-daemon\n" +
			"State:  S (sleeping)\n" +
			"Tgid:   2359\n" +
			"Pid:    2359\n" +
			"PPid:   1\n" +
			"TracerPid:      0\n" +
			"Uid:    0       0       0       0\n" +
			"Gid:    0       0       0       0\n", "0",

			"Name:   rsyslogd\n" +
			"State:  S (sleeping)\n" +
			"Tgid:   638\n" +
			"Pid:    638\n" +
			"PPid:   1\n" +
			"TracerPid:      0\n" +
			"Uid:    101     101     101     101\n" +
			"Gid:    103     103     103     103\n", "101",

			"Uid:\t1000\t1000\t1000\t1000", "1000",
			"UID:\t1000\t1000\t1000\t1000", "1000",
			"uid:\t1000\t1000\t1000\t1000", "1000",
			"Uid:   1000   1000   1000   1000", "1000",
			"UID:   1000   1000   1000   1000", "1000",
			"uid:   1000   1000   1000   1000", "1000",
		};

		for(int i = 0; i < samples.length; i += 2) {
			String statusContent = samples[i];
			String value = samples[i + 1];
			BufferedReader reader = new BufferedReader(new StringReader(statusContent));
			String resolvedValue = LinuxLauncher.processStatusContent(reader);
			System.out.format("%s => %s VS %s\n", statusContent, resolvedValue, value);
			Assert.assertEquals(value, resolvedValue);
		}
	}
}
