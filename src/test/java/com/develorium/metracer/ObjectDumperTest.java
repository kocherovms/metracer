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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;

public class ObjectDumperTest {
	public static class Terminator {
		public String model;

		public Terminator(String theModel) {
			model = theModel;
		}
	}

	public static class Gun {
		public String name;
		public String caliber;
		public double muzzleVelocity;

		public Gun(String theName, String theCaliber, double theMuzzleVelocity) {
			name = theName;
			caliber = theCaliber;
			muzzleVelocity = theMuzzleVelocity;
		}
	}

	public static class HollowObject {
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
		public Object testObject = new Terminator("T600");
		public Object testNullObject = null;
		public Terminator testTerminator = new Terminator("T1000");
		public Gun ak47 = new Gun("AK47", "7.62", 715.0);
		public HollowObject hollowObject = new HollowObject();
		
		public int[] testInts = new int[] { 1, 2, 3 };
		public Integer[] testIntegers = new Integer[] { 1, 2, 3 };
		public Terminator[] terminators = new Terminator[] { new Terminator("T600"), new Terminator("T700"), new Terminator("T1000") };

		public Map<String, Gun> gunsMap = new HashMap<String, Gun>() { {
				put("AK47", new Gun("AK47", "7.62", 715));
				put("M16", new Gun("M16", "5.56", 790));
			}
		};

		private String privateString = "private property";
		public Date myDate = new Date();

		public enum Mode {
			a, b;
		}

		public Mode mode = Mode.a;
	}

	public static class CircularReference {
		public String value;
		public CircularReference reference;

		CircularReference(String theValue, CircularReference theReference) {
			value = theValue;
			reference = theReference;
		}
	}

	@Test
	public void testGetAllDeclaredFields() {
		Integer myZ = new Integer(999);
		System.out.println("<kms@>" + myZ.toString() + "</kms@>");

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
			"java.lang.Object", "testObject",
			"[I", "testInts",
			"[Ljava.lang.Integer;", "testIntegers",
			"[Lcom.develorium.metracer.ObjectDumperTest$Terminator;", "terminators",
			null, "gunsMap",
			"java.lang.String", "privateString",
			"java.util.Date", "myDate",
			"com.develorium.metracer.ObjectDumperTest$TestObject$Mode", "mode",
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
	public void testDumpObjectField() {
		TestObject testObject = new TestObject();
		List<Field> fields = ObjectDumper.getAllDeclaredFields(new TestObject());
		String[] samples = {
			"testByte", "" + testObject.testByte,
			"testShort", "" + testObject.testShort,
			"testChar", "" + testObject.testChar,
			"testInt", "" + testObject.testInt,
			"testLong", "" + testObject.testLong,
			"testFloat", "" + testObject.testFloat,
			"testDouble", "" + testObject.testDouble,
			"testBoolean", "" + testObject.testBoolean,
			"testBigByte", "" + testObject.testBigByte, 
			"testBigShort", "" + testObject.testBigShort, 
			"testBigChar", "" + testObject.testBigChar, 
			"testBigInt", "" + testObject.testBigInt, 
			"testBigLong", "" + testObject.testBigLong, 
			"testBigFloat", "" + testObject.testBigFloat, 
			"testBigDouble", "" + testObject.testBigDouble, 
			"testBigBoolean", "" + testObject.testBigBoolean, 
			"testString", "" + testObject.testString, 
			"testBaseString", "" + testObject.testBaseString, 
			"testObject", "" + "model=" + ((Terminator)testObject.testObject).model,
			"testNullObject", "null",
			"testTerminator", "" + "model=" + testObject.testTerminator.model,
			"ak47", "" + "name=" + testObject.ak47.name,
			"ak47", "" + "caliber=" + testObject.ak47.caliber,
			"ak47", "" + "muzzleVelocity=" + testObject.ak47.muzzleVelocity,
			"hollowObject", "{}",
			"testInts", "1",
			"testInts", "2",
			"testInts", "3",
			"testIntegers", "1",
			"testIntegers", "2",
			"testIntegers", "3",
			"terminators", "T600",
			"terminators", "T700",
			"terminators", "T1000",
			"gunsMap", "AK47",
			"gunsMap", "7.62",
			"gunsMap", "715",
			"gunsMap", "M16",
			"gunsMap", "5.56",
			"gunsMap", "790",
			"privateString", "private property",
			"myDate", testObject.myDate.toString(),
			"mode", testObject.mode.toString(),
		};

		for(int i = 0; i < samples.length;) {
			String fieldName = samples[i++];
			String expectedDumpContent = samples[i++];
			Field field = findField(fields, fieldName);
			Assert.assertTrue(field != null);
			String dumpResult = new ObjectDumper().dumpObjectField(testObject, field).get();
			System.out.format("Object field dump: %s = %s\n", fieldName, dumpResult);
			Assert.assertTrue(dumpResult.contains(expectedDumpContent));
		}
	}

	@Test
	public void testDumpObject() {
		TestObject testObject = new TestObject();
		String dumpResult = new ObjectDumper().dumpObject(testObject);
		System.out.println("Object dump result = " + dumpResult);
		String[] samples = {
			"testByte=" + testObject.testByte,
			"testShort=" + testObject.testShort,
			"testChar=" + testObject.testChar,
			"testInt=" + testObject.testInt,
			"testLong=" + testObject.testLong,
			"testFloat=" + testObject.testFloat,
			"testDouble=" + testObject.testDouble,
			"testBoolean=" + testObject.testBoolean,
			"testBigByte=" + testObject.testBigByte, 
			"testBigShort=" + testObject.testBigShort, 
			"testBigChar=" + testObject.testBigChar, 
			"testBigInt=" + testObject.testBigInt, 
			"testBigLong=" + testObject.testBigLong, 
			"testBigFloat=" + testObject.testBigFloat, 
			"testBigDouble=" + testObject.testBigDouble, 
			"testBigBoolean=" + testObject.testBigBoolean, 
			"testString=" + testObject.testString, 
			"testBaseString=" + testObject.testBaseString, 
			"testNullObject=null",
			"testObject=",
			new ObjectDumper().dumpObject(testObject.testObject),
			"ak47=",
			new ObjectDumper().dumpObject(testObject.ak47),
			"testTerminator=",
			new ObjectDumper().dumpObject(testObject.testTerminator),
			"hollowObject=",
			new ObjectDumper().dumpObject(testObject.hollowObject),
			"testInts=",
			new ObjectDumper().dumpObject(testObject.testInts),
			"testIntegers=",
			new ObjectDumper().dumpObject(testObject.testIntegers),
			"terminators=",
			new ObjectDumper().dumpObject(testObject.terminators),
			"gunsMap=",
			new ObjectDumper().dumpObject(testObject.gunsMap),
			"myDate=",
			new ObjectDumper().dumpObject(testObject.myDate),
			"mode=",
			new ObjectDumper().dumpObject(testObject.mode),
		};
		
		for(String sample : samples) {
			System.out.println("Checking for sample " + sample);
			Assert.assertTrue(dumpResult.contains(sample));
		}
	}

	@Test
	public void testCircularReference() {
		CircularReference a = new CircularReference("hello", null);
		CircularReference b = new CircularReference("world", a);
		a.reference = b;
		String result = new ObjectDumper().dumpObject(a);
		System.out.println("Result of circular reference dump = " + result);
		Assert.assertTrue(result.contains("too long value"));
	}

	@Test
	public void testDumpArray() {
		Integer[] a = { 1, 2, 3 };
		List<Integer> v = Arrays.asList(a);
		String expected = String.format("[%s,%s,%s]", a[0], a[1], a[2]);
		Assert.assertEquals(expected, new ObjectDumper().dumpObject(v));
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