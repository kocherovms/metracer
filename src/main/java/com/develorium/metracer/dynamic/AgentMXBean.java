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

public interface AgentMXBean {
	public static class Counters implements Serializable {
		private static final long serialVersionUID = 5304832712221989304L;
		public int methodsCount = 0;
		public int classesCount = 0;
		public int failedClassesCount = 0;

		public byte[] serialize() throws IOException {
			ByteArrayOutputStream backend = new ByteArrayOutputStream();
			ObjectOutputStream stream = new ObjectOutputStream(backend);
			stream.writeObject(this);
			stream.close();
			return backend.toByteArray();
		}

		public static Counters deserialize(byte[] theData) throws IOException, ClassNotFoundException {
			ByteArrayInputStream backend = new ByteArrayInputStream(theData);
			ObjectInputStream stream = new ObjectInputStream(backend);
			Counters counters = (Counters)stream.readObject();
			stream.close();
			return counters;
		}
	}

	public void setIsVerbose(boolean theIsVerbose);
	// returns serialized Counters
	public byte[] setPatterns(String theClassMatchingPattern, String theMethodMatchingPattern, boolean theIsWithStackTraces);
	// returns serialized Counters
	public byte[] removePatterns();
}
