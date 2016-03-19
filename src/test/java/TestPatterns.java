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
import junit.framework.Assert;
import org.junit.Test;

public class TestPatterns {
	Patterns testNullFalsePattern = new Patterns("test", null, false);
	Patterns testTestFalsePattern = new Patterns("test", "test", false);
	Patterns testNullTruePattern = new Patterns("test", null, true);
	Patterns testTestTruePattern = new Patterns("test", "test", true);
	Patterns testNullFalsePattern2 = new Patterns("test", null, false);
	Patterns testTestFalsePattern2 = new Patterns("test", "test", false);
	Patterns testNullTruePattern2 = new Patterns("test", null, true);
	Patterns testTestTruePattern2 = new Patterns("test", "test", true);

	@Test(expected = NullPointerException.class)
	public void testNotNullClassMatchingPattern() {
		new Patterns(null, "test", false);
	}

	@Test
	public void testIsClassPatternMatched() {
		String[] goodPatterns = {
			"com.",
			"com.develorium",
			"com.develorium.metracertest",
			"com.develorium.metracertest.Test",
			"Test",
			"develorium",
			"rium.metr",
			"T",
			"com.*Test",
			"com\\.develorium\\..*Test",
			"com.[xyd].*Test",
			".*",
			"qwe.zxc|com.develorium"
		};

		String[] badPatterns = {
			"tom&jerry",
			"sherry",
			"roastbeef"
		};

		for(String goodPattern : goodPatterns) {
			Assert.assertTrue(new Patterns(goodPattern, null, false).isClassPatternMatched("com.develorium.metracertest.Test"));
		}

		for(String badPattern : badPatterns) {
			Assert.assertFalse(new Patterns(badPattern, null, false).isClassPatternMatched("com.develorium.metracertest.Test"));
		}
	}

	@Test
	public void testIsPatternMatched() {
		String[] goodPatterns = {
			"com.", "doSomething",
			"com.develorium", "doSomething",
			"com.develorium.metracertest", "doSomething",
			"com.develorium.metracertest.Test", "doSomething",
			"Test", "doSomething",
			"develorium", "doSomething",
			"rium.metr", "doSomething",
			"T", "doSomething",
			"Test", "Test::doSomething",
			"Test", null,
			"Test", "t::d",
			"Test", "Test",
		};

		String[] badPatterns = {
			"Test", "doAnother",
			"Test", "ther",
			"Test", "Test:$",
			"Test", "Test::beGood"
		};

		for(int i = 0; i < goodPatterns.length;) {
			String classMatchingPattern = goodPatterns[i++];
			String methodMatchingPattern = goodPatterns[i++];
			Assert.assertTrue(new Patterns(classMatchingPattern, methodMatchingPattern, false).isPatternMatched("com.develorium.metracertest.Test", "doSomething"));
		}

		for(int i = 0; i < badPatterns.length;) {
			String classMatchingPattern = badPatterns[i++];
			String methodMatchingPattern = badPatterns[i++];
			Assert.assertFalse(new Patterns(classMatchingPattern, methodMatchingPattern, false).isPatternMatched("com.develorium.metracertest.Test", "doSomething"));
		}
	}

	@Test
	public void testBlacklisting() {
		for(String prefix : Patterns.BlacklistedClassNamePrefixes) {
			Assert.assertFalse(new Patterns(".*", null, false).isClassPatternMatched(prefix));
			Assert.assertFalse(new Patterns(".*", null, false).isPatternMatched(prefix, "doSomething"));
		}
	}

	@Test
	public void testEquality() {
		Assert.assertEquals(testNullFalsePattern, testNullFalsePattern2);
		Assert.assertEquals(testTestFalsePattern, testTestFalsePattern2);
		Assert.assertEquals(testNullTruePattern, testNullTruePattern2);
		Assert.assertEquals(testTestTruePattern, testTestTruePattern2);
	}

	@Test
	public void testNonEquality() {
		Assert.assertFalse(testNullFalsePattern.equals(testTestFalsePattern));
		Assert.assertFalse(testNullFalsePattern.equals(testNullTruePattern));
		Assert.assertFalse(testNullFalsePattern.equals(testTestTruePattern));
		Assert.assertFalse(testNullFalsePattern.equals(testTestFalsePattern2));
		Assert.assertFalse(testNullFalsePattern.equals(testNullTruePattern2));
		Assert.assertFalse(testNullFalsePattern.equals(testTestTruePattern2));
	}

	@Test
	public void testKeyGoodEncodingDecoding() {
		String[] data = {
			"test1", TestPatterns.class.getName(),
			"test3" , TestPatterns.class.getName() + TestPatterns.class.getClassLoader().toString(),
			"", TestPatterns.class.getName(),
			"test4", "",
			"t", "t",
			"t", "x",
			"test5(Ljava.lang.Object", "music",
			"test6([Ljava.lang.Object", "music",
			"test7", "somerandomdata::somerandomdata"
		};

		for(int i = 0; i < data.length; ) {
			String methodName = data[i++];
			String classId = data[i++];
			Patterns.Key key = new Patterns.Key();
			key.setClassId(classId);
			key.setMethodName(methodName);
			String encodedKey =  Patterns.encodeKey(key);
			Patterns.Key decodedKey = Patterns.decodeKey(encodedKey);
			Assert.assertEquals(key.getClassId(), decodedKey.getClassId());
			Assert.assertEquals(key.getMethodName(), decodedKey.getMethodName());
		}
	}

	@Test
	public void testKeyBadEncodingDecoding() {
		Patterns.Key key = Patterns.decodeKey("");
		Assert.assertNull(key);
	}

	@Test(expected = NullPointerException.class) 
	public void testKeyDoesntAcceptNullClassId() {
		Patterns.Key key = new Patterns.Key();
		key.setClassId(null);
	}

	@Test(expected = NullPointerException.class) 
	public void testKeyDoesntAcceptNullMethodName() {
		Patterns.Key key = new Patterns.Key();
		key.setMethodName(null);
	}

	@Test
	public void testGetCounters() {
		Patterns p = new Patterns("test", null, false);
		Patterns.Counters c = p.getCounters();
		Assert.assertEquals(c.classesCount, 0);
		Assert.assertEquals(c.methodsCount, 0);

		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns", "method1");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns", "method2");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns", "method3");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 1);
		Assert.assertEquals(c.methodsCount, 3);

		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "method1");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "method2");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "method3");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 2);
		Assert.assertEquals(c.methodsCount, 6);

		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "method1");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "method2");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "method3");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 2);
		Assert.assertEquals(c.methodsCount, 6);

		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), null, "method1");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", null);
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 2);
		Assert.assertEquals(c.methodsCount, 6);

		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "TestPatterns1", "");
		p.registerInstrumentedMethod(TestPatterns.class.getClassLoader(), "", "method2");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 2);
		Assert.assertEquals(c.methodsCount, 6);

		p.registerInstrumentedMethod(null, "TestPatterns2", "method1");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 3);
		Assert.assertEquals(c.methodsCount, 7);

		p.registerInstrumentedMethod(null, "TestPatterns2", "method1");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 3);
		Assert.assertEquals(c.methodsCount, 7);

		p.registerInstrumentedMethod(null, "TestPatterns2", "methodz(Ljava/lang/String;");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 3);
		Assert.assertEquals(c.methodsCount, 8);

		p.registerInstrumentedMethod(null, "TestPatterns2", "methodz(Ljava/lang/String;Ljava/lang/Object");
		c = p.getCounters();
		Assert.assertEquals(c.classesCount, 3);
		Assert.assertEquals(c.methodsCount, 9);
	}

	private static class MyClass {
	}

	private static class MyClass2 {
	}

	private static class MyClass3 {
	}

	@Test
	public void testGetDeinstrumentedMethodsNullResistance() {
		Assert.assertEquals(0, Patterns.getDeinstrumentedMethods(null, new ArrayList<Class<?>>()));
		Assert.assertEquals(0, Patterns.getDeinstrumentedMethods(new ArrayList<Patterns>(), null));
		Assert.assertEquals(0, Patterns.getDeinstrumentedMethods(null, null));
	}

	@Test
	public void testGetDeinstrumentedMethods() {
		List<Patterns> historyPatterns = new ArrayList<Patterns>();
		Patterns p = null;
		List<Class<?>> classList = new ArrayList<Class<?>>();

		p = new Patterns("class", "method", false);
		p.registerInstrumentedMethod(MyClass.class.getClassLoader(), MyClass.class.getName(), "myMethod1");
		p.registerInstrumentedMethod(MyClass.class.getClassLoader(), MyClass.class.getName(), "myMethod2");
		historyPatterns.add(p);

		classList.clear();
		classList.add(MyClass.class);
		Assert.assertEquals(2, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		p = new Patterns("class", "method", false);
		p.registerInstrumentedMethod(MyClass.class.getClassLoader(), MyClass.class.getName(), "myMethod3");
		p.registerInstrumentedMethod(MyClass.class.getClassLoader(), MyClass.class.getName(), "myMethod4");
		historyPatterns.add(p);

		classList.clear();
		classList.add(MyClass.class);
		Assert.assertEquals(4, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		p = new Patterns("class", "method", false);
		p.registerInstrumentedMethod(MyClass.class.getClassLoader(), MyClass.class.getName(), "myMethod4");
		p.registerInstrumentedMethod(MyClass.class.getClassLoader(), MyClass.class.getName(), "myMethod5");
		historyPatterns.add(p);

		classList.clear();
		classList.add(MyClass.class);
		Assert.assertEquals(5, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		p = new Patterns("class", "method", false);
		p.registerInstrumentedMethod(MyClass2.class.getClassLoader(), MyClass2.class.getName(), "myMethodA");
		p.registerInstrumentedMethod(MyClass2.class.getClassLoader(), MyClass2.class.getName(), "myMethodB");
		historyPatterns.add(p);

		classList.clear();
		classList.add(MyClass.class);
		Assert.assertEquals(5, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		classList.clear();
		classList.add(MyClass2.class);
		Assert.assertEquals(2, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		classList.clear();
		classList.add(MyClass.class);
		classList.add(MyClass2.class);
		Assert.assertEquals(7, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		classList.clear();
		classList.add(MyClass3.class);
		Assert.assertEquals(0, Patterns.getDeinstrumentedMethods(historyPatterns, classList));

		// different class loader test
		p = new Patterns("class", "method", false);
		p.registerInstrumentedMethod(null, MyClass2.class.getName(), "myMethodA");
		p.registerInstrumentedMethod(null, MyClass2.class.getName(), "myMethodB");
		historyPatterns.add(p);
		classList.clear();
		classList.add(MyClass2.class);
		Assert.assertEquals(2, Patterns.getDeinstrumentedMethods(historyPatterns, classList));
	}
}