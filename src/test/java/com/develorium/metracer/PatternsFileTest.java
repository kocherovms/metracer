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
import java.util.regex.*;
import java.io.*;
import org.junit.Assert;
import org.junit.Test;

public class PatternsFileTest {
	@Test
	public void testLoading() throws IOException {
		String content = 
			"com.develorium.metracer.TestA::method1\n" +
			"com.develorium.metracer.TestA::method2\n" +
			"com.develorium.metracer.TestA::method3\n" +
			"com.develorium.metracer.TestB::method0\n" +
			"com.develorium.metracer.TestC$1::method2\n" +
			"com.develorium.metracer.TestD_DD::m_ethod\n";
		ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
		PatternsFile file = new PatternsFile(inputStream);

		Pattern classMatchingPattern = Pattern.compile(file.getClassMatchingPattern());
		System.out.println("Class matching pattern = " + classMatchingPattern);
		Assert.assertTrue(classMatchingPattern.matcher("com.develorium.metracer.TestA").find());
		Assert.assertTrue(classMatchingPattern.matcher("com.develorium.metracer.TestB").find());
		Assert.assertTrue(classMatchingPattern.matcher("zzz.com.develorium.metracer.TestA.zzz").find());
		Assert.assertTrue(classMatchingPattern.matcher("com.develorium.metracer.TestC$1").find());
		Assert.assertTrue(classMatchingPattern.matcher("com.develorium.metracer.TestD_DD").find());
		Assert.assertFalse(classMatchingPattern.matcher("com.develorium.").find());
		Assert.assertFalse(classMatchingPattern.matcher("com.develorium.metracer.TestC").find());
		Assert.assertFalse(classMatchingPattern.matcher("java.lang.String").find());

		Pattern methodMatchingPattern = Pattern.compile(file.getMethodMatchingPattern());
		System.out.println("Method matching pattern = " + methodMatchingPattern);
		Assert.assertTrue(methodMatchingPattern.matcher("com.develorium.metracer.TestA::method1").find());
		Assert.assertTrue(methodMatchingPattern.matcher("com.develorium.metracer.TestA::method2").find());
		Assert.assertTrue(methodMatchingPattern.matcher("com.develorium.metracer.TestA::method3").find());
		Assert.assertTrue(methodMatchingPattern.matcher("com.develorium.metracer.TestB::method0").find());
		Assert.assertTrue(methodMatchingPattern.matcher("zzz.com.develorium.metracer.TestB::method0___").find());
		Assert.assertTrue(methodMatchingPattern.matcher("com.develorium.metracer.TestC$1::method2").find());
		Assert.assertTrue(methodMatchingPattern.matcher("com.develorium.metracer.TestD_DD::m_ethod").find());
		Assert.assertFalse(methodMatchingPattern.matcher("com.develorium.metracer.TestA::methodX").find());
		Assert.assertFalse(methodMatchingPattern.matcher("com.develorium.metracer.TestB::method2").find());
		Assert.assertFalse(methodMatchingPattern.matcher("java.lang.String::substring").find());
	}

	@Test
	public void testLoadingOfDups() throws IOException {
		String content = 
			"com.develorium.metracer.TestA::method1\n" +
			"com.develorium.metracer.TestA::method1\n" +
			"com.develorium.metracer.TestA::method1\n";
		ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
		PatternsFile file = new PatternsFile(inputStream);

		String classMatchingPattern = file.getClassMatchingPattern();
		System.out.println("Class matching pattern = " + classMatchingPattern);
		Assert.assertFalse(classMatchingPattern.contains("|"));

		String methodMatchingPattern = file.getMethodMatchingPattern();
		System.out.println("Method matching pattern = " + methodMatchingPattern);
		Assert.assertFalse(methodMatchingPattern.contains("|"));
	}

	@Test
	public void testEmptyLoading() throws IOException {
		String content = 
			"# Ignored comment\n" +
			"# Another ignored comment\n" +
			"\n";
		ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
		PatternsFile file = new PatternsFile(inputStream);
		Assert.assertTrue(file.getClassMatchingPattern() == null);
		Assert.assertTrue(file.getMethodMatchingPattern() == null);
	}

	@Test
	public void testLoadingOfBadPatterns() throws IOException {
		String content = 
			"com.develorium.metracer.TestA\n" +
			"methodName\n" +
			"java.lang.String:unknown\n" +
			"::methodName\n" +
			"ClassName::\n" +
			"::\n" + 
			"com.develorium.metracer.TestA : : methodName\n" +
			"com.develorium.metracer.TestA: :methodName\n" +
			"com.develorium.metracer.TestA :: methodName\n";

		for(String contentLine : content.split("\n")) {
			try {
				ByteArrayInputStream inputStream = new ByteArrayInputStream(contentLine.getBytes());
				PatternsFile file = new PatternsFile(inputStream);
				Assert.fail();
			} catch(RuntimeException e) {
			}
		}
	}

	@Test 
	public void testPatternsConsumption() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PatternsFile file = new PatternsFile(outputStream);
		file.consumePatterns("com.develorium.metracer.Test::method");
		Assert.assertTrue(outputStream.toString().contains("com.develorium.metracer.Test::method"));
		file.consumePatterns("com.acme.www.Servlet::doPerform\ncom.acme.www.Servlet::doBuild");
		Assert.assertTrue(outputStream.toString().contains("com.acme.www.Servlet::doPerform"));
		Assert.assertTrue(outputStream.toString().contains("com.acme.www.Servlet::doBuild"));
	}

	@Test
	public void testDuplicationProtectionDuringPatternsConsumption() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PatternsFile file = new PatternsFile(outputStream);
		file.consumePatterns("x.y.z.MyClass::method1");
		int size = outputStream.size();

		file.consumePatterns("x.y.z.MyClass::method1");
		Assert.assertTrue(size == outputStream.size());
	}

	@Test
	public void testPatternsRecovery() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PatternsFile file = new PatternsFile(outputStream);
		String stackTrace1 = 
			"x.y.z.MyClass::method1\n" + 
			"x.y.z.MyClass::method2\n" +
			"x.y.z.MyClass::method3\n";

		String stackTrace2 = 
			"a.b.c.HisClass::buildx\n" + 
			"a.b.c.HisClass::buildy\n" + 
			"a.b.c.HisClass::buildz\n";

		file.consumePatterns("# Comment 1\n" + stackTrace1);
		file.consumePatterns("# Comment 2\n" + stackTrace2);

		String content = outputStream.toString();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
		file = new PatternsFile(inputStream);
		Pattern classMatchingPattern = Pattern.compile(file.getClassMatchingPattern());
		System.out.println("Class matching pattern = " + classMatchingPattern);
		Assert.assertTrue(classMatchingPattern.matcher("x.y.z.MyClass").find());
		Assert.assertTrue(classMatchingPattern.matcher("a.b.c.HisClass").find());
		Pattern methodMatchingPattern = Pattern.compile(file.getMethodMatchingPattern());
		System.out.println("Method matching pattern = " + methodMatchingPattern);

		for(String line : stackTrace1.split("\n"))
			Assert.assertTrue(methodMatchingPattern.matcher(line).find());

		for(String line : stackTrace2.split("\n"))
			Assert.assertTrue(methodMatchingPattern.matcher(line).find());
	}
}