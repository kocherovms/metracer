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

package com.develorium.metracertest;

public class MainJob {
	public void perform() {
		init("user", "123");
		compute();
		try {
			computeMoreData();
		} catch(Exception e) {
			System.err.format("Failed to compute additional data: %1$s\n", e.toString());
		}
		try {
			computeEvenMoreData();
		} catch(Exception e) {
			System.err.format("Failed to compute additional data: %1$s\n", e.toString());
		}
		computeDataLevel0();
		printResults();
	}

	private static void init(String theUser, String thePassword) {
		System.out.println("Initing program data");
	}

	private static void compute() {
		System.out.println("Computing data");
	}

	private static void computeMoreData() throws Exception {
		System.out.println("Computing more data but...");
		throw new Exception("Something went wrong");
	}

	private static void computeEvenMoreData() throws Exception {
		System.out.println("Computing even more data but...");
		try {
			throw new Exception("Something went wrong");
		} finally {
			System.err.println("Resources for even more data computation released");
		}
	}

	private static void computeDataLevel0() {
		computeDataLevel1();
	}

	private static void computeDataLevel1() {
		computeDataLevel2();
	}

	private static void computeDataLevel2() {
	}

	private static void printResults() {
		System.out.println("Result is 42");
	}
}
