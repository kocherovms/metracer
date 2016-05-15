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

import java.lang.reflect.Field;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;

public class ObjectDumperTest {
	public static class Terminator {
		public String name = "T1000";
	}

	public static class BaseTestObject {
		public boolean testBoolean = true;
	}

	public static class TestObject extends BaseTestObject {
		public String testString = "loremipsum";
		public byte testByte = 20;
		public short testShort = 32;
		public char testChar = 42;
		public int testInt = 1024;
		public long testLong = 9999999;
		public float testFloat = 3.14;
		public double testDouble = 2.72;
		public Object testObject = new Terminator();
	}

	@Test
	public void testGetAllDeclaredFields() {
		List<Field> fields = ObjectDumper.getAllDeclaredFields(new TestObject());
		Assert.assertTrue(hasField(fields, "java.lang.String", "testString"));
		Assert.assertTrue(hasField(fields, "int", "testInt"));
		Assert.assertTrue(hasField(fields, "java.lang.Object", "testObject"));
		Assert.assertTrue(hasField(fields, "boolean", "testBoolean"));
		Assert.assertFalse(hasField(fields, "java.lang.Object", "anotherTestObject"));
	}

	@Test
	public void testDumpSingleField() {
		TestObject testObject = new TestObject();
		testObject.testBoolean = true;
		testObject.testString = "loremipsum";
		testObject.testInt = 123;
		
		List<Field> fields = ObjectDumper.getAllDeclaredFields(new TestObject());
		Assert.assertTrue(hasField(fields, "java.lang.String", "testString"));
		Assert.assertTrue(hasField(fields, "int", "testInt"));
		Assert.assertTrue(hasField(fields, "java.lang.Object", "testObject"));
		Assert.assertTrue(hasField(fields, "boolean", "testBoolean"));
		Assert.assertFalse(hasField(fields, "java.lang.Object", "anotherTestObject"));
	}

	private static Field findField(List<Field> theFields, String theTypeName, String theName) {
		for(Field f : theFields) {
			if(f.getType().getName().equals(theTypeName) && f.getName().equals(theName))
				return f;
		}

		return null;
	}

	private static boolean hasField(List<Field> theFields, String theTypeName, String theName) {
		return findField(theFields, theTypeName, theName) != null;
	}
}