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

public class TestAgentPatterns {
	Agent.Patterns testNullFalsePattern = Agent.createPatterns("test", null, false);
	Agent.Patterns testTestFalsePattern = Agent.createPatterns("test", "test", false);
	Agent.Patterns testNullTruePattern = Agent.createPatterns("test", null, true);
	Agent.Patterns testTestTruePattern = Agent.createPatterns("test", "test", true);
	Agent.Patterns testNullFalsePattern2 = Agent.createPatterns("test", null, false);
	Agent.Patterns testTestFalsePattern2 = Agent.createPatterns("test", "test", false);
	Agent.Patterns testNullTruePattern2 = Agent.createPatterns("test", null, true);
	Agent.Patterns testTestTruePattern2 = Agent.createPatterns("test", "test", true);

	Agent.Patterns nullNullTruePattern = Agent.createPatterns(null, null, true);
	Agent.Patterns nullNullFalsePattern = Agent.createPatterns(null, null, false);
	Agent.Patterns nullTestTruePattern = Agent.createPatterns(null, "test", true);
	Agent.Patterns nullTestFalsePattern = Agent.createPatterns(null, "test", false);
	Agent.Patterns nullNullTruePattern2 = Agent.createPatterns(null, null, true);
	Agent.Patterns nullNullFalsePattern2 = Agent.createPatterns(null, null, false);
	Agent.Patterns nullTestTruePattern2 = Agent.createPatterns(null, "test", true);
	Agent.Patterns nullTestFalsePattern2 = Agent.createPatterns(null, "test", false);

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
}