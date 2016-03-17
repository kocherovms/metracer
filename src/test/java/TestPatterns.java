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

package com.develorium.metracer.dynamic;

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

	Patterns nullNullTruePattern = new Patterns(null, null, true);
	Patterns nullNullFalsePattern = new Patterns(null, null, false);
	Patterns nullTestTruePattern = new Patterns(null, "test", true);
	Patterns nullTestFalsePattern = new Patterns(null, "test", false);
	Patterns nullNullTruePattern2 = new Patterns(null, null, true);
	Patterns nullNullFalsePattern2 = new Patterns(null, null, false);
	Patterns nullTestTruePattern2 = new Patterns(null, "test", true);
	Patterns nullTestFalsePattern2 = new Patterns(null, "test", false);

	@Test
	public void testEquality() {
		Assert.assertEquals(testNullFalsePattern, testNullFalsePattern2);
		Assert.assertEquals(testTestFalsePattern, testTestFalsePattern2);
		Assert.assertEquals(testNullTruePattern, testNullTruePattern2);
		Assert.assertEquals(testTestTruePattern, testTestTruePattern2);

		Assert.assertEquals(nullNullTruePattern, nullNullTruePattern2);
		Assert.assertEquals(nullNullFalsePattern, nullNullFalsePattern2);
		Assert.assertEquals(nullTestTruePattern, nullTestTruePattern2);
		Assert.assertEquals(nullTestFalsePattern, nullTestFalsePattern2);
	}

	@Test
	public void testNonEquality() {
		Assert.assertFalse(testNullFalsePattern.equals(testTestFalsePattern));
		Assert.assertFalse(testNullFalsePattern.equals(testNullTruePattern));
		Assert.assertFalse(testNullFalsePattern.equals(testTestTruePattern));
		Assert.assertFalse(testNullFalsePattern.equals(testTestFalsePattern2));
		Assert.assertFalse(testNullFalsePattern.equals(testNullTruePattern2));
		Assert.assertFalse(testNullFalsePattern.equals(testTestTruePattern2));

		Assert.assertFalse(nullNullTruePattern.equals(nullNullFalsePattern));
		Assert.assertFalse(nullNullTruePattern.equals(nullTestTruePattern));
		Assert.assertFalse(nullNullTruePattern.equals(nullTestFalsePattern));
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
	public void testCounters() {
		Patterns p = new Patterns(null, null, false);
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
	}
}