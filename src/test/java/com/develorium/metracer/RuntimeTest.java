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

public class RuntimeTest {
	Runtime r = new Runtime(null);

	@Test
	public void testFormatArgumentValueNull() {
		Assert.assertEquals(r.formatArgumentValue(null), "null");
	}

	@Test
	public void testFormatArgumentValueInteger() {
		Integer v = 10;
		Assert.assertEquals(r.formatArgumentValue(v), v.toString());
	}

	@Test
	public void testFormatArgumentValueDouble() {
		Double v = 42.0;
		Assert.assertEquals(r.formatArgumentValue(v), v.toString());
	}

	@Test
	public void testFormatArgumentValueEmptyIntArray() {
		int[] v = {};
		Assert.assertEquals(r.formatArgumentValue(v), "[]");
	}

	@Test
	public void testFormatArgumentValueBooleanArray() {
		boolean[] v = { true, false, true };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueByteArray() {
		byte[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueShortArray() {
		short[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueIntArray() {
		int[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueLongArray() {
		long[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueFloatArray() {
		float[] v = { 1.0f, 2.0f, 3.0f };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueDoubleArray() {
		double[] v = { 1.0, 2.0, 3.0 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueStringArray() {
		String[] v = { "Hello", "world", "!" };
		String expected = String.format("[%s, %s, %s]", v[0], v[1], v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueStringArrayWithNulls() {
		String[] v = { "Hello", "world", "!", null };
		String expected = String.format("[%s, %s, %s, null]", v[0], v[1], v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueCollection() {
		Integer[] a = { 1, 2, 3 };
		List<Integer> v = Arrays.asList(a);
		String expected = String.format("[%s, %s, %s]", a[0], a[1], a[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueEmptyCollection() {
		Integer[] a = {};
		List<Integer> v = Arrays.asList(a);
		String expected = "[]";
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueSingleElementCollection() {
		List<Integer> v = Arrays.asList(1);
		String expected = "[1]";
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueCollectionWithNulls() {
		Integer[] a = { 1, 2, null };
		List<Integer> v = Arrays.asList(a);
		String expected = String.format("[%s, %s, %s]", a[0], a[1], a[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueLongCollection() {
		List<Integer> v = new ArrayList<Integer>();
		StringBuilder b = new StringBuilder();

		for(int i = 0; i < Runtime.MaxElementsForPrinting; ++i) {
			v.add(i);
			
			if(b.length() > 0)
				b.append(", ");

			b.append(i);
		}

		String expected = String.format("[%s]", b.toString());
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueVeryLongCollection() {
		List<Integer> v = new ArrayList<Integer>();
		StringBuilder b = new StringBuilder();

		for(int i = 0; i < Runtime.MaxElementsForPrinting + 1; ++i) {
			v.add(i);
			
			if(i < Runtime.MaxElementsForPrinting) {
				if(b.length() > 0)
					b.append(", ");

				b.append(i);
			}
		}

		String expected = String.format("[%s, ...]", b.toString());
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueMap() {
		Map<String, String> v = new HashMap<String, String>();
		v.put("Hello", "world");
		v.put("Lorem", "ipsum");
		v.put("no", "pasaran");
		v.put("x", null);
		v.put(null, "y");
		StringBuilder b = new StringBuilder();

		for(Map.Entry<String, String> e: v.entrySet()) {
			if(b.length() > 0) 
				b.append(", ");

			b.append(e.getKey() + " => " + e.getValue());
		}

		String expected = "[" + b.toString() + "]";
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFormatArgumentValueEmptyMap() {
		Assert.assertEquals(r.formatArgumentValue(new HashMap<String, String>()), "[]");
	}

	@Test
	public void testFormatArgumentValueCustomObject() {
		Assert.assertEquals(r.formatArgumentValue(this), this.toString());
	}

	@Override
	public String toString() {
		return "TestArgumentValuePrinting.java";
	}

	@Test
	public void testFormatReturnValueVoid() {
		Assert.assertTrue(r.formatReturnValue(true, null).contains(" => void"));
		Assert.assertTrue(r.formatReturnValue(true, new Integer(123)).contains(" => void"));
	}

	@Test
	public void testFormatReturnValueNull() {
		String v = r.formatReturnValue(false, null);
		Assert.assertTrue(v.contains("=> return") && v.contains("null"));
	}

	@Test
	public void testFormatReturnValueNotNull() {
		Integer i = 123;
		String v = r.formatReturnValue(false, i);
		Assert.assertTrue(v.contains(" => return") && v.contains(i.toString()));
	}

	@Test
	public void testFormatReturnValueException() {
		NullPointerException e = new NullPointerException();
		String v = r.formatReturnValue(false, e);
		Assert.assertTrue(v.contains(" => exception") && v.contains(e.toString()));
	}

	@Test
	public void testFormatReturnValueArray() {
		int[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		String rv = r.formatReturnValue(false, v);
		Assert.assertTrue(rv.contains(expected));
	}

	@Test
	public void testFormatReturnValueCollection() {
		Integer[] a = { 1, 2, 3 };
		List<Integer> v = Arrays.asList(a);
		String expected = String.format("[%s, %s, %s]", a[0], a[1], a[2]);
		String rv = r.formatReturnValue(false, v);
		Assert.assertTrue(rv.contains(expected));
	}

	@Test
	public void testFormatReturnValueMap() {
		Map<String, String> v = new HashMap<String, String>();
		v.put("Hello", "world");
		v.put("Lorem", "ipsum");
		v.put("no", "pasaran");
		v.put("x", null);
		v.put(null, "y");
		StringBuilder b = new StringBuilder();

		for(Map.Entry<String, String> e: v.entrySet()) {
			if(b.length() > 0) 
				b.append(", ");

			b.append(e.getKey() + " => " + e.getValue());
		}

		String expected = "[" + b.toString() + "]";
		String rv = r.formatReturnValue(false, v);
		Assert.assertTrue(rv.contains(expected));
	}
}