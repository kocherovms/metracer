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

import java.util.*;

class MainBase {
	MainBase() {
		System.out.println("MainBase");
	}
}

public class Main extends MainBase {
	static String staticString = "hello, world";
	private Thread testJob = null;

	static {
		System.out.println("value of a staticString = " + staticString);
	}

	private String privateVar = "initial private var";

	Main() {
		privateVar = "redefined private var";
	}

	Main(String theArg) {
		privateVar = theArg;
	}

	// Subject to failures 'Stack map does not match the one at exception handler' after instrumentation
	Main(Main theOther) {
		super();
		privateVar = theOther.privateVar;
	}

	Main(int theOther) {
		try {
			privateVar = "text";
		} finally {
			privateVar = "otherText";
		}
	}

	Main(float theOther) {
		try {
			try {
				privateVar = "float";
			} catch(Throwable e) {
			}
		} finally {
		}
	}

	public static void main(String[] args) {
		System.out.println("kms@ sample program started");
		testBundle();

		new Main("C").startTestJob();

		try {
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static void testBundle() {
		testA();
		int res = testB(false, true);
		System.out.println("result of testB = " + res);
		testC(42, 2.718f, 3.1415);
		testInheritanceBackward();
		testInheritanceForward();
		testIntRetVal();
		testIntegerRetVal();
		testDoubleRetVal();
		testStringRetVal();
		testStringArray(new String[] { "hello", "world!" });
		testIntArray(new int[] { 1, 2, 3 });
		testIntegerArray(new Integer[] { 1, 2, 3 });
		testList(Arrays.asList("lorem", "ipsum"));
		HashMap<Integer, String> dict = new HashMap<Integer, String>();
		dict.put(1, "test");
		dict.put(2, "alice");
		dict.put(3, "bob");
		dict.put(4, null);
		testMap(dict);
		testCustomObject();

		try {
			new Main("A").findClass("test");
			new Main("B").findClass("test", false, false);
		} catch(Exception e) {
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
	static Integer testIntegerRetVal() {
		return new Integer(42);
	}
	static double testDoubleRetVal() {
		return 42.0;
	}
	static String testStringRetVal() {
		return "lorem ipsum...";
	}
	static void testStringArray(String[] theStrings) {
	}
	static void testIntArray(int[] theNumbers) {
	}
	static void testIntegerArray(Integer[] theNumbers) {
	}
	static void testList(List<String> theStrings) {
	}
	static void testMap(HashMap<Integer, String> theMap) {
	}

	private static class Profession {
		private String name;

		public Profession(String theName) {
			name = theName;
		}

		public String getName() {
			return name;
		}
	}

	private static class Employee {
		public String name = "John Doe";
		public int age = 38;
		public Profession profession = new Profession("Advocate");
		public enum Role {
			SPECIALIST, MANAGER, BOSS
		};
		public Role role = Role.MANAGER;
	}

	static void testCustomObject() {
		Employee e = getEmployee();
		processPayroll(e);
	}

	static Employee getEmployee() {
		return new Employee();
	}

	static void processPayroll(Employee theEmployee) {
		// do something
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
	private void checkNpe() {
		try {
			throw new NullPointerException();
		} catch(NullPointerException e) {
			System.out.println("NPE catched");
		}
	}
	private void testStackTrace0() {
		testStackTrace1();
	}
	private void testStackTrace1() {
		testStackTrace2();
	}
	private void testStackTrace2() {
		testStackTrace3();
	}
	private void testStackTrace3() {
		testStackTrace4();
	}
	private void testStackTrace4() {
	}
	private Map<String, String> testReturnsMap() {
		Map<String, String> rv = new HashMap<String, String>();
		rv.put("hello", "world");
		rv.put("lorem", "ipsum");
		rv.put("2*2", "4");
		return rv;
	}

	private void startTestJob() {
		testJob = new Thread(new Runnable() {
				public void run() {
					while(true) {
						testBundle();
						doSomething();
						checkNpe();
						testStackTrace0();
						testReturnsMap();
						try {
							Thread.sleep(500);
						} catch(InterruptedException e) {
						}
					}
				}
			});

		testJob.start();
    }
}
