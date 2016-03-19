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

import java.io.*;
import junit.framework.Assert;
import org.junit.Test;

public class TestAgentCounters {
	@Test
	public void testSerialization() throws IOException, ClassNotFoundException {
		AgentMXBean.Counters counters = new AgentMXBean.Counters();
		counters.classesCount = 1;
		counters.methodsCount = 2;
		counters.failedClassesCount = 3;
		byte[] data = counters.serialize();
		AgentMXBean.Counters restoredCounters = AgentMXBean.Counters.deserialize(data);
		Assert.assertEquals(counters.classesCount, restoredCounters.classesCount);
		Assert.assertEquals(counters.methodsCount, restoredCounters.methodsCount);
		Assert.assertEquals(counters.failedClassesCount, restoredCounters.failedClassesCount);
	}
}
