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

public class HelperTest {
	@Test
	public void testUsageIsWorking() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Helper.printUsage(new PrintStream(output), null);
		Assert.assertTrue(output.size() > 0);
		Assert.assertFalse(output.toString().contains("${launchstring}"));
	}

	@Test
	public void testHelpIsWorking() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Helper.executeAuxCommands(Config.COMMAND.HELP, new PrintStream(output));
		Assert.assertTrue(output.size() > 0);
		Assert.assertFalse(output.toString().contains("${launchstring}"));
	}

	@Test
	public void testJvmListIsWorking() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Helper.executeAuxCommands(Config.COMMAND.LIST, new PrintStream(output));
		Assert.assertTrue(output.size() > 0);
	}
}