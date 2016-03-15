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

public class TestArgumentValuePrinting {
	Runtime r = new Runtime(null);

	@Test
	public void testNull() {
		Assert.assertEquals(r.formatArgumentValue(null), "null");
	}

	@Test
	public void testInteger() {
		Integer v = 10;
		Assert.assertEquals(r.formatArgumentValue(v), v.toString());
	}

	@Test
	public void testDouble() {
		Double v = 42.0;
		Assert.assertEquals(r.formatArgumentValue(v), v.toString());
	}

	@Test
	public void testEmptyIntArray() {
		int[] v = {};
		Assert.assertEquals(r.formatArgumentValue(v), "[]");
	}

	@Test
	public void testBooleanArray() {
		boolean[] v = { true, false, true };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testByteArray() {
		byte[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testShortArray() {
		short[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testIntArray() {
		int[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testLongArray() {
		long[] v = { 1, 2, 3 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testFloatArray() {
		float[] v = { 1.0f, 2.0f, 3.0f };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testDoubleArray() {
		double[] v = { 1.0, 2.0, 3.0 };
		String expected = String.format("[%s, %s, %s]", "" + v[0], "" + v[1], "" + v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testStringArray() {
		String[] v = { "Hello", "world", "!" };
		String expected = String.format("[%s, %s, %s]", v[0], v[1], v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testStringArrayWithNulls() {
		String[] v = { "Hello", "world", "!", null };
		String expected = String.format("[%s, %s, %s, null]", v[0], v[1], v[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testCollection() {
		Integer[] a = { 1, 2, 3 };
		List<Integer> v = Arrays.asList(a);
		String expected = String.format("[%s, %s, %s]", a[0], a[1], a[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testEmptyCollection() {
		Integer[] a = {};
		List<Integer> v = Arrays.asList(a);
		String expected = "[]";
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testSingleElementCollection() {
		List<Integer> v = Arrays.asList(1);
		String expected = "[1]";
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testCollectionWithNulls() {
		Integer[] a = { 1, 2, null };
		List<Integer> v = Arrays.asList(a);
		String expected = String.format("[%s, %s, %s]", a[0], a[1], a[2]);
		Assert.assertEquals(r.formatArgumentValue(v), expected);
	}

	@Test
	public void testLongCollection() {
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
	public void testVeryLongCollection() {
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
	public void testMap() {
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
	public void testEmptyMap() {
		Assert.assertEquals(r.formatArgumentValue(new HashMap<String, String>()), "[]");
	}

	@Test
	public void testCustomObject() {
		Assert.assertEquals(r.formatArgumentValue(this), this.toString());
	}

	@Override
	public String toString() {
		return "TestArgumentValuePrinting.java";
	}
}