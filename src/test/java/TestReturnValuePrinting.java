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

public class TestReturnValuePrinting {
	Runtime r = new Runtime(null);

	@Test
	public void testVoid() {
		Assert.assertTrue(r.formatReturnValue(true, null).contains(" => void"));
		Assert.assertTrue(r.formatReturnValue(true, new Integer(123)).contains(" => void"));
	}

	@Test
	public void testNull() {
		String v = r.formatReturnValue(false, null);
		Assert.assertTrue(v.contains("=> return") && v.contains("null"));
	}

	@Test
	public void testNotNull() {
		Integer i = 123;
		String v = r.formatReturnValue(false, i);
		Assert.assertTrue(v.contains(" => return") && v.contains(i.toString()));
	}

	@Test
	public void testException() {
		NullPointerException e = new NullPointerException();
		String v = r.formatReturnValue(false, e);
		Assert.assertTrue(v.contains(" => exception") && v.contains(e.toString()));
	}
}