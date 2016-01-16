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

import java.util.concurrent.*;

public class Runtime {
	private static Runtime instance = new Runtime();

	static class Loggers extends ConcurrentHashMap<String, Object> {
		// id of a class loader -> instance of a logger (org.slf4j.Logger)
	}
	
	private ConcurrentMap<String, Loggers> classesLoggers = new ConcurrentHashMap<String, Loggers>(); // class name -> available loggers for this class

	private Runtime() {
		
	}

	public static Runtime getInstance() {
		//System.out.println("me called");
		return instance;
	}

	public void learnClass(Class theClass) {
		System.out.println("kms@ learnClass " + theClass.getName());
	}

	public void log(String theMessage) {
		
	}
}
