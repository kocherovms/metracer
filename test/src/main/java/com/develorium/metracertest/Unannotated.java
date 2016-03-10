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

class UnannotatedBase {
	UnannotatedBase() {
		System.out.println("UnannotatedBase");
	}
}

public class Unannotated extends UnannotatedBase {
	static String staticString = "hello, world";
	private Thread testJob = null;

	static {
		System.out.println("value of a staticString = " + staticString);
	}

	private String privateVar = "initial private var";

	Unannotated() {
		privateVar = "redefined private var";
	}

	Unannotated(String theArg) {
		privateVar = theArg;
	}

	// Subject to failures 'Stack map does not match the one at exception handler' after instrumentation
	Unannotated(Unannotated theOther) {
		super();
		privateVar = theOther.privateVar;
	}

	Unannotated(int theOther) {
		try {
			privateVar = "text";
		} finally {
			privateVar = "otherText";
		}
	}

	Unannotated(float theOther) {
		try {
			try {
				privateVar = "float";
			} catch(Throwable e) {
			}
		} finally {
		}
	}

	public static void main( String[] args ) {
		testA();
		int res = testB(false, true);
		System.out.println("result of testB = " + res);
		testC(42, 2.718f, 3.1415);
		testInheritanceBackward();
		testInheritanceForward();
		testIntRetVal();
		testDoubleRetVal();
		testStringRetVal();

		try {
			new Unannotated("A").findClass("test");
			new Unannotated("B").findClass("test", false, false);
		} catch(Exception e) {
			e.printStackTrace();
		}

		new Unannotated("C").startTestJob();

		try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
		throw new Throwable("Hello, world");
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
	static int testIntRetVal() {
		return 42;
	}
	static double testDoubleRetVal() {
		return 42.0;
	}
	static String testStringRetVal() {
		return "lorem ipsum...";
	}
	Class<?> findClass(String theClassName) throws ClassNotFoundException {
		System.out.println("zzz search for class " + theClassName);
		return null;
	}
	Class<?> findClass(String theClassName, boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
		System.out.println("yyy search for class " + theClassName);
		return null;
	}
	private void doSomething() {
		System.out.println("do something");
	}
	private void startTestJob() {
        testJob = new Thread(new Runnable() {
				public void run() {
					while(true) {
						doSomething();
						try {
							Thread.sleep(3000);
						} catch(InterruptedException e) {
						}
					}
				}
			});

		testJob.start();
    }
}