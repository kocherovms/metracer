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

import java.util.Map;
import java.util.HashMap;
import org.junit.Test;
import org.junit.Assert;

public class ConfigTest {
	@Test(expected = Config.BadConfig.class)
	public void testNullIsNotTolerated() {
		Config config = new Config(null);
	}

	@Test
	public void testReadArgumentsFromEnvironmentVariablesIfTheyAreEmpty() {
		Map<String, String> env = new HashMap<String, String>();
		env.put("METRACER_ARGUMENT_0", "A");
		env.put("METRACER_ARGUMENT_1", "B");
		env.put("METRACER_ARGUMENT_2", "C");
		String[] args = Config.readArgumentsFromEnvironmentVariablesIfTheyAreEmpty(null, env);
		Assert.assertEquals(3, args.length);
		Assert.assertEquals("A", args[0]);
		Assert.assertEquals("B", args[1]);
		Assert.assertEquals("C", args[2]);

		args = Config.readArgumentsFromEnvironmentVariablesIfTheyAreEmpty(new String[] { "-v", "2016", "test" }, env);
		Assert.assertEquals(3, args.length);
		Assert.assertEquals("-v", args[0]);
		Assert.assertEquals("2016", args[1]);
		Assert.assertEquals("test", args[2]);

		env.clear();
		env.put("METRACER_ARGUMENT_0", "A");
		env.put("METRACER_ARGUMENT_1", "B");
		env.put("METRACER_ARGUMENT_9", "C");
		env.put("METRACER_ARGUMENT_10", "D");
		env.put("METRACER_ARGUMENT_11", "E");
		args = Config.readArgumentsFromEnvironmentVariablesIfTheyAreEmpty(null, env);
		Assert.assertEquals(5, args.length);
		Assert.assertEquals("A", args[0]);
		Assert.assertEquals("B", args[1]);
		Assert.assertEquals("C", args[2]);
		Assert.assertEquals("D", args[3]);
		Assert.assertEquals("E", args[4]);

		env.clear();
		env.put("JAVA_HOME", "c:\\Program Files\\Java\\bin");
		env.put("METRACER_ARGUMENT_0", "A");
		env.put("PATH", "c:\\test");
		args = Config.readArgumentsFromEnvironmentVariablesIfTheyAreEmpty(null, env);
		Assert.assertEquals(1, args.length);
		Assert.assertEquals("A", args[0]);

		env.clear();
		env.put("METRACER_ARGUMENT_zzz", "A");
		args = Config.readArgumentsFromEnvironmentVariablesIfTheyAreEmpty(null, env);
		Assert.assertEquals(null, args);
	}

	@Test
	public void testHelpCommand() {
		Config config = new Config(new String[]{ "-h" });
		Assert.assertTrue(config.command ==  Config.COMMAND.HELP);
	}

	@Test
	public void testVerboseHelpCommand() {
		Config config = new Config(new String[]{ "-h", "-v" });
		Assert.assertTrue(config.command == Config.COMMAND.HELP);
		Assert.assertTrue(config.isVerbose);
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadHelpCommand() {
		Config config = new Config(new String[]{ "-h", "bla-bla-bla" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBad2HelpCommand() {
		Config config = new Config(new String[]{ "-h", "bla-bla-bla", "-v" });
	}

	@Test
	public void testListCommand() {
		Config config = new Config(new String[]{ "-l" });
		Assert.assertTrue(config.command == Config.COMMAND.LIST);
	}

	@Test
	public void testVerboseListCommand() {
		Config config = new Config(new String[]{ "-l", "-v" });
		Assert.assertTrue(config.command == Config.COMMAND.LIST);
		Assert.assertTrue(config.isVerbose);
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadListCommand() {
		Config config = new Config(new String[]{ "-l", "bla-bla-bla" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBad2ListCommand() {
		Config config = new Config(new String[]{ "-l", "bla-bla-bla", "-v" });
	}

	@Test
	public void testInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.methodMatchingPattern == null);
		Assert.assertFalse(config.isWithStackTrace);
	}

	@Test
	public void testVerboseIntrumentCommand() {
		Config config = new Config(new String[]{ "123", "-v", "Class" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.isVerbose);
	}

	@Test
	public void testLongInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class", "Method" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.methodMatchingPattern.equals("Method"));
		Assert.assertFalse(config.isWithStackTrace);
	}

	@Test
	public void testWithStackTraceInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class", "-s"});
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.isWithStackTrace);
		Assert.assertTrue(config.stackTraceFileName == null);
	}

	@Test
	public void testWithStackTraceInstrumentCommand2() {
		Config config = new Config(new String[]{ "-s", "123", "Class"});
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.isWithStackTrace);
		Assert.assertTrue(config.stackTraceFileName == null);
	}

	@Test
	public void testWithStackTraceBigInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class", "-S", "/tmp/st.txt" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.isWithStackTrace);
		Assert.assertTrue(config.stackTraceFileName.equals("/tmp/st.txt"));
	}

	@Test
	public void testWithStackTraceBigInstrumentCommand2() {
		Config config = new Config(new String[]{ "-S", "/tmp/st.txt", "123", "Class"});
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.isWithStackTrace);
		Assert.assertTrue(config.stackTraceFileName.equals("/tmp/st.txt"));
	}

	@Test
	public void testWithStackTraceLongInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class", "Method", "-s" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.methodMatchingPattern.equals("Method"));
		Assert.assertTrue(config.isWithStackTrace);
		Assert.assertTrue(config.stackTraceFileName == null);
	}

	@Test
	public void testWithStackTraceBigLongInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class", "Method", "-S", "/tmp/123.txt" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.methodMatchingPattern.equals("Method"));
		Assert.assertTrue(config.isWithStackTrace);
		Assert.assertTrue(config.stackTraceFileName.equals("/tmp/123.txt"));
	}

	@Test
	public void testInstrumentCommandFromFileName() {
		Config config = new Config(new String[]{ "123", "-f", "/tmp/123.txt" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.patternsFileName.equals("/tmp/123.txt"));

		config = new Config(new String[]{ "-f", "/tmp/123.txt", "123" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.patternsFileName.equals("/tmp/123.txt"));

		config = new Config(new String[]{ "-f", "/tmp/123.txt", "123", "Class" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.patternsFileName.equals("/tmp/123.txt"));

		config = new Config(new String[]{ "-f", "/tmp/123.txt", "123", "Class", "Method" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertTrue(config.pid == 123);
		Assert.assertTrue(config.classMatchingPattern.equals("Class"));
		Assert.assertTrue(config.methodMatchingPattern.equals("Method"));
		Assert.assertTrue(config.patternsFileName.equals("/tmp/123.txt"));
	}

	@Test
	public void testSoleWithStackTraceIsWrong() {
		Config config = new Config(new String[]{ "-s" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testSoleWithStackTraceBigIsWrong() {
		Config config = new Config(new String[]{ "-S" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testInstrumentCommandWithStackTraceBigRequiresFileName() {
		Config config = new Config(new String[]{ "123", "Class", "Method", "-S" });
		Assert.assertEquals(123, config.pid);
	}

	@Test
	public void testInstrumentCommandWithStackTraceBigConsumesFileName() {
		Config config = new Config(new String[]{ "-S", "123", "Class", "Method" });
		Assert.assertEquals(0, config.pid); // autodiscover mode
	}

	@Test(expected = Config.BadConfig.class)
	public void testInstrumentCommandWithStackTraceBigConsumesFileName2() {
		Config config = new Config(new String[]{ "-S", "123", "badpid", "Class", "Method" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testNoExtraArgumentsForInstrumentCommand() {
		Config config = new Config(new String[]{ "123", "Class", "Method", "Extra" });
	}

	@Test
	public void testAutodiscoverPidInstrumentCommand() {
		Config config = new Config(new String[]{ "habrClass" });
		Assert.assertEquals(0, config.pid); // autodiscover mode
	}

	@Test
	public void testAutodiscoverPidInstrumentCommand2() {
		Config config = new Config(new String[]{ "habrClass", "fooMethod" });
		Assert.assertEquals(0, config.pid); // autodiscover mode
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadPidInstrumentCommand() {
		Config config = new Config(new String[]{ "bogus", "habrClass", "fooMethod" });
	}

	@Test
	public void testInstrumentWithFileNameConsumesPid() {
		Config config = new Config(new String[]{ "-f", "123", "Class", "Method" });
		Assert.assertEquals(0, config.pid); // autodiscover mode
	}

	@Test(expected = Config.BadConfig.class)
	public void testInstrumentWithFileNameConsumesPid2() {
		Config config = new Config(new String[]{ "-f", "123", "badpid", "Class", "Method" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testInstrumentCommandFromFileNameRequiredFileName() {
		Config config = new Config(new String[]{ "123", "-f" });
	}

	@Test
	public void testMinimalInstrumentCommand() {
		Config config = new Config(new String[]{ "123" });
		Assert.assertTrue(config.command == Config.COMMAND.INSTRUMENT);
		Assert.assertEquals(123, config.pid);
		Assert.assertTrue(config.classMatchingPattern == null);
		Assert.assertTrue(config.methodMatchingPattern == null);
		Assert.assertFalse(config.isWithStackTrace);
	}

	@Test
	public void testDesintrumentCommand() {
		Config config = new Config(new String[]{ "-r", "123" });
		Assert.assertTrue(config.command == Config.COMMAND.DEINSTRUMENT);
		Assert.assertTrue(config.pid == 123);
	}

	@Test
	public void testVerboseDesintrumentCommand() {
		Config config = new Config(new String[]{ "-r", "123", "-v" });
		Assert.assertTrue(config.command == Config.COMMAND.DEINSTRUMENT);
		Assert.assertEquals(123, config.pid);
		Assert.assertTrue(config.isVerbose);
	}

	@Test
	public void testAutodiscoverPidDeinstrumentCommand() {
		Config config = new Config(new String[]{ "-r" });
		Assert.assertEquals(0, config.pid);
	}

	@Test(expected = Config.BadConfig.class)
	public void testNoExtraArgumentsForDeinstrumentCommand() {
		Config config = new Config(new String[]{ "-r", "123", "Class" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testNoWithStackTraceForDeinstrumentCommand() {
		Config config = new Config(new String[]{ "-r", "123", "-s" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testBadPidDeinstrumentCommand() {
		Config config = new Config(new String[]{ "-r", "habr" });
	}

	@Test
	public void testMethodArgumentDumpLimit() {
		Config config = new Config(new String[]{ "-m", "128", "15" });
		Assert.assertEquals(128, config.methodArgumentDumpLimit);
	}

	@Test(expected = Config.BadConfig.class)
	public void testEmptyMethodArgumentDumpLimit() {
		Config config = new Config(new String[]{ "15", "-m" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testNonIntegerMethodArgumentDumpLimit() {
		Config config = new Config(new String[]{ "-m", "zzz", "15" });
	}

	@Test(expected = Config.BadConfig.class)
	public void testZeroMethodArgumentDumpLimit() {
		Config config = new Config(new String[]{ "-m", "0", "15" });
	}
}

