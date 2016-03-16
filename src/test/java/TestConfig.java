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

import java.util.*;
import junit.framework.Assert;
import org.junit.Test;

public class TestConfig {
	@Test(expected = Config.BadConfig.class)
	public void testNullIsNotTolerated() throws Config.BadConfig {
		Config config = new Config(null);
	}

	@Test
	public void testHelpCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-h" });
		Assert.assertEquals(config.command, Config.COMMAND.HELP);
	}

	@Test
	public void testVerboseHelpCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-h", "-v" });
		Assert.assertEquals(config.command, Config.COMMAND.HELP);
		Assert.assertEquals(config.isVerbose, true);
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadHelpCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-h", "bla-bla-bla" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBad2HelpCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-h", "bla-bla-bla", "-v" });
	}

	@Test
	public void testListCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-l" });
		Assert.assertEquals(config.command, Config.COMMAND.LIST);
	}

	@Test
	public void testVerboseListCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-l", "-v" });
		Assert.assertEquals(config.command, Config.COMMAND.LIST);
		Assert.assertEquals(config.isVerbose, true);
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadListCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-l", "bla-bla-bla" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBad2ListCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-l", "bla-bla-bla", "-v" });
	}

	@Test
	public void testInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123", "Class" });
		Assert.assertEquals(config.command, Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.classMatchingPattern, "Class");
		Assert.assertEquals(config.methodMatchingPattern, null);
		Assert.assertEquals(config.isWithStackTrace, false);
	}

	@Test
	public void testVerboseIntrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123", "-v", "Class" });
		Assert.assertEquals(config.command, Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.classMatchingPattern, "Class");
		Assert.assertEquals(config.isVerbose, true);
	}

	@Test
	public void testLongInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123", "Class", "Method" });
		Assert.assertEquals(config.command, Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.classMatchingPattern, "Class");
		Assert.assertEquals(config.methodMatchingPattern, "Method");
		Assert.assertEquals(config.isWithStackTrace, false);
	}

	@Test
	public void testWithStackTraceInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123", "Class", "-s"});
		Assert.assertEquals(config.command, Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.classMatchingPattern, "Class");
		Assert.assertEquals(config.isWithStackTrace, true);
	}

	@Test
	public void testWithStackTraceLongInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123", "Class", "Method", "-s" });
		Assert.assertEquals(config.command, Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.classMatchingPattern, "Class");
		Assert.assertEquals(config.methodMatchingPattern, "Method");
		Assert.assertEquals(config.isWithStackTrace, true);
	}

	@Test(expected = Config.BadConfig.class)
	public void testNoExtraArgumentsForInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123", "Class", "Method", "Extra" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadPidInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "habr", "Class" });
	}

	@Test
	public void testMinimalInstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "123" });
		Assert.assertEquals(config.command, Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.classMatchingPattern, null);
		Assert.assertEquals(config.methodMatchingPattern, null);
		Assert.assertEquals(config.isWithStackTrace, false);
	}

	@Test
	public void testDesintrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-r", "123" });
		Assert.assertEquals(config.command, Config.COMMAND.DEINSTRUMENT);
		Assert.assertEquals(config.pid, 123);
	}

	@Test
	public void testVerboseDesintrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-r", "123", "-v" });
		Assert.assertEquals(config.command, Config.COMMAND.DEINSTRUMENT);
		Assert.assertEquals(config.pid, 123);
		Assert.assertEquals(config.isVerbose, true);
	}

	@Test(expected = Config.BadConfig.class)
	public void testNoExtraArgumentsForDeinstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-r", "123", "Class" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testNoWithStackTraceForDeinstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-r", "123", "-s" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadPidDeinstrumentCommand() throws Config.BadConfig {
		Config config = new Config(new String[]{ "-r", "habr" });
	}
}

