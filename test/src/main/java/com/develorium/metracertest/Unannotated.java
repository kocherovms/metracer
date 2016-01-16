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

public class Unannotated {
	static String staticString = "hello, world";

	static {
		System.out.println("value of a staticString = " + staticString);
	}

	public static void main( String[] args ) {
		testA();
		int res = testB(false, true);
		System.out.println("result of testB = " + res);
		testC(42, 2.718f, 3.1415);
		testInheritanceBackward();
		testInheritanceForward();
	}
	static void testA() {
		System.out.println("testA");
	}
	static int testB(boolean theBool1, boolean theBool2) {
		if(!theBool1)
			return testB(true, true);

		return 42;
	}
	static void testC(int theInt, float theFloat, double theDouble) {
		testInnerC();
	}
	static void testInnerC() {
		try {
			testFaulty();
		} catch(Throwable e) {
		}
	}
	static void testFaulty() throws Throwable {
		throw new Throwable();
	}
	static class InnerBase {
		public void test() {
			System.out.println("InnerBase.test");
		}
		public void test2() {
			System.out.println("InnerBase.test2");
		}
	}
	static class InnerChild extends InnerBase {
		public void test() {
			System.out.println("InnerChild.test");
			super.test();
		}
		public void test2() {
			System.out.println("InnerChild.test2");
		}
	}
	static void testInheritanceBackward() {
		InnerChild i = new InnerChild();
		i.test();
	}
	static void testInheritanceForward() {
		InnerBase i = new InnerChild();
		i.test2();
	}
}