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
		public String testBaseString = "hello,world!";
	}

	public static class TestObject extends BaseTestObject {
		public byte testByte = 20;
		public short testShort = 32;
		public char testChar = '!';
		public int testInt = 1024;
		public long testLong = 9999999;
		public float testFloat = 3.14f;
		public double testDouble = 2.71;
		public boolean testBoolean = true;

		public Byte testBigByte = 20;
		public Short testBigShort = 32;
		public Character testBigChar = 42;
		public Integer testBigInt = 1024;
		public Long testBigLong = 9999999L;
		public Float testBigFloat = 3.14f;
		public Double testBigDouble = 2.71;
		public Boolean testBigBoolean = true;

		public String testString = "loremipsum";
		public Object testObject = new Terminator();
	}

	@Test
	public void testGetAllDeclaredFields() {
		List<Field> fields = ObjectDumper.getAllDeclaredFields(new TestObject());
		String[] samples = {
			"byte", "testByte",
			"short", "testShort",
			"char", "testChar",
			"int", "testInt",
			"long", "testLong",
			"float", "testFloat",
			"double", "testDouble",
			"boolean", "testBoolean",
			"java.lang.Byte", "testBigByte",
			"java.lang.Short", "testBigShort",
			"java.lang.Character", "testBigChar",
			"java.lang.Integer", "testBigInt",
			"java.lang.Long", "testBigLong",
			"java.lang.Float", "testBigFloat",
			"java.lang.Double", "testBigDouble",
			"java.lang.Boolean", "testBigBoolean",
			"java.lang.String", "testString",
			"java.lang.String", "testBaseString",
			"java.lang.Object", "testObject"
		};

		for(int i = 0; i < samples.length;) {
			String typeName = samples[i++];
			String fieldName = samples[i++];
			System.out.println(String.format("Looging for field %s of type %s", fieldName, typeName));
			Assert.assertTrue(hasField(fields, fieldName, typeName));
			Assert.assertFalse(hasField(fields, fieldName, typeName + "nonce"));
			Assert.assertFalse(hasField(fields, fieldName + "nonce", typeName));
			Assert.assertFalse(hasField(fields, fieldName + "nonce", typeName + "nonce"));
		}
	}

	@Test
	public void testDumpSingleField() {
		TestObject testObject = new TestObject();
		List<Field> fields = ObjectDumper.getAllDeclaredFields(new TestObject());
		String[] samples = {
			"testByte", "" + testObject.testByte,
			"testShort", "" + testObject.testShort,
			"testChar", "" + testObject.testChar
		};

		for(int i = 0; i < samples.length;) {
			String fieldName = samples[i++];
			String expectedDumpContent = samples[i++];
			Field field = findField(fields, fieldName);
			Assert.assertTrue(field != null);
			String dumpResult = ObjectDumper.dumpSingleField(testObject, field);
			System.out.println("Single field dump: " + dumpResult);
			Assert.assertTrue(dumpResult.contains(fieldName));	
			Assert.assertTrue(dumpResult.contains(expectedDumpContent));
		}
	}

	private static Field findField(List<Field> theFields, String theName) {
		return findField(theFields, theName, null);
	}

	private static Field findField(List<Field> theFields, String theName, String theTypeName) {
		for(Field f : theFields) {
			if((theTypeName == null || f.getType().getName().equals(theTypeName)) && f.getName().equals(theName))
				return f;
		}

		return null;
	}

	private static boolean hasField(List<Field> theFields, String theTypeName, String theName) {
		return findField(theFields, theTypeName, theName) != null;
	}
}