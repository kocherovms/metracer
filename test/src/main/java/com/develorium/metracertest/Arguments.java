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

public class Arguments {
	public static void main( String[] args ) {
		testDummy();
		testBool(false, true);
		testNumbers(42, 2.718f, 3.1415);
		testBoxedNumbers(42, 2.718f, 3.1415);
		testString("Hello, world!");
		testLongString(new String(new char[100]).replace("\0", "Hello, world!"));
		testStructure(new Structure());
		new Arguments().testNonStaticMethod("Hello, world!", null);
	}

	static void testDummy() {
	}

	static void testBool(boolean theBool1, boolean theBool2) {
	}

	static void testNumbers(int theInt, float theFloat, double theDouble) {
	}

	static void testBoxedNumbers(Integer theInt, Float theFloat, Double theDouble) {
	}

	static void testString(String theString) {
	}

	static void testLongString(String theLongString) {
	}

	static void testStructure(Structure theStructure) {
	}

	void testNonStaticMethod(String theString, Object theObject) {
	}

	static class Structure {
	}
}