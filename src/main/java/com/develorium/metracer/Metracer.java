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

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.regex.*;
import javassist.*;

public class Metracer implements ClassFileTransformer {
	private Pattern pattern = null;
	private ClassPools classPools = new ClassPools();
	private TracingCodeInjector injector = new TracingCodeInjector();
	private Runtime runtime = Runtime.getInstance();

	public Metracer(String theArguments) {
		if(theArguments == null)
			return;

		final String PatternStanza = "pattern=";

		if(theArguments.startsWith(PatternStanza)) {
			String patternString = theArguments.substring(PatternStanza.length());
			try {
				pattern = Pattern.compile(patternString);
			} catch(PatternSyntaxException e) {
				System.err.format("Provided pattern \"%1$s\" is malformed: %2$s\n", patternString, e.toString());
			}
		}
	}
	public byte[] transform(ClassLoader loader, String className,
				Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
				byte[] classfileBuffer) throws IllegalClassFormatException {
		final String canonicalClassName = className.replaceAll("/", ".");

		if(canonicalClassName.indexOf("java.lang") == 0)
			return classfileBuffer;

		ClassPool cp = classPools.getClassPool(loader);
		cp.insertClassPath(new ByteArrayClassPath(canonicalClassName, classfileBuffer));

		try {
			CtClass cc = cp.get(canonicalClassName);

			if(!isInstrumentable(cc)) 
				return classfileBuffer;

			boolean wasInstrumented = false;

			if(isClassLoader(cc)) {
				System.out.println("kms@ detected classloader " + cc.getName());
				wasInstrumented = tuneClassLoader(cc);
			}

			//if(canonicalClassName.equals("org.jboss.modules.ModuleClassLoader") || canonicalClassName.equals("org.jboss.modules.ConcurrentClassLoader")) {
			//	wasInstrumented = instrumentClassLoader(cc);
			//}

			if(pattern != null)
				wasInstrumented = wasInstrumented || instrumentViaPattern(cc);
		
			if(wasInstrumented) {
				try {
					return cc.toBytecode();
				} catch (Exception e) {
					System.err.format("Failed to compile instrumented class %1$s: %2$s\n",
							  canonicalClassName, e.toString());
				}
			}
		} catch (NotFoundException e) {
			System.err.format("Failed to register class %1$s in a javaassist ClassPool: %2$s\n",
					  canonicalClassName, e.toString());
		}
	
		return classfileBuffer;
	}
	private boolean isInstrumentable(CtClass theClass) {
		return !theClass.isFrozen() && !theClass.isInterface();
	}
	static class FirstInjectionHolder {
		public boolean v = true;
	}
	private boolean isClassLoader(CtClass theClass) {
		try {
			while(theClass != null) {
				if(theClass.getName().equals("java.lang.ClassLoader"))
					return true;

				theClass = theClass.getSuperclass();
			}
		} catch(javassist.NotFoundException e) {
			System.err.format("Failed to qualify %1$s as a class loader: %2$s", theClass.getName(), e.toString());
			return false;
		}

		return false;
	}
	// Method allows to use classes from com.develorium.metracer for class loaders with strict isolation, e.g. JBoss module class loader
	private boolean tuneClassLoader(CtClass theClass) {
		boolean wasInstrumented = false;

		for(CtMethod method : theClass.getDeclaredMethods()) {
			try {
				if(method.getName().equals("findClass")) {
					String clFieldName = "cmdr_e6b8085c9db3473eae21204661c80d4e";
					StringBuilder runtimeResolutionCode = new StringBuilder();
					runtimeResolutionCode.append(String.format("if($1.startsWith(\"%1$s\")) {", this.getClass().getPackage().getName()));
					runtimeResolutionCode.append(String.format(" java.lang.ClassLoader %1$s = $0;", clFieldName));
					runtimeResolutionCode.append(String.format(" while(%1$s != null) {", clFieldName));
					runtimeResolutionCode.append(String.format("  java.lang.reflect.Field f = java.lang.ClassLoader.class.getDeclaredField(\"classes\");"));
					runtimeResolutionCode.append(String.format("  f.setAccessible(true);"));
					runtimeResolutionCode.append(String.format("  java.util.Vector classes = (java.util.Vector)f.get(%1$s);", clFieldName));
					runtimeResolutionCode.append(String.format("  for(int i = 0; i < classes.size(); i++) {"));
					runtimeResolutionCode.append(String.format("   java.lang.Class c = (java.lang.Class)classes.get(i);", Runtime.class.getName()));
					runtimeResolutionCode.append(String.format("   if(c != null && c.getName().equals(\"%1$s\")) {", Runtime.class.getName()));
					runtimeResolutionCode.append(String.format("    return c;"));
					runtimeResolutionCode.append(String.format("    break;"));
					runtimeResolutionCode.append(String.format("   }"));
					runtimeResolutionCode.append(String.format("  }"));
					runtimeResolutionCode.append(String.format("  %1$s = %1$s.getParent();", clFieldName));
					runtimeResolutionCode.append(String.format(" }"));
					runtimeResolutionCode.append(String.format("}"));
					method.insertBefore(runtimeResolutionCode.toString());
					wasInstrumented = true;
					break;
				}
			} catch (Exception e) {
				System.err.format("Failed to tune class loader %1$s: %2$s\n", theClass.getName(), e.toString());
			}
		}

		return wasInstrumented;
	}
	private boolean instrumentViaPattern(CtClass theClass) {
		FirstInjectionHolder isFirstInjection = new FirstInjectionHolder();
		boolean wasInstrumented = false;

		for(CtMethod method : theClass.getDeclaredMethods()) {
			try {
				final String methodNameForPatternMatching = String.format("%1$s::%2$s", theClass.getName(), method.getName());
				
				if(!pattern.matcher(methodNameForPatternMatching).find(0)) 
					continue;

				if(instrumentMethod(theClass, method, isFirstInjection))
					wasInstrumented = true;
			} catch (Exception e) {
				System.err.format("Failed to add tracing to method %1$s: %2$s\n", 
						  method.getLongName(), e.toString());
			}
		}

		return wasInstrumented;
	}
	private boolean instrumentMethod(CtClass theClass, CtMethod theMethod, FirstInjectionHolder theIsFirstInjection) throws java.lang.Exception {
		if(!injector.isMethodInstrumentable(theMethod)) 
			return false;

		if(theIsFirstInjection.v) {
			try {
				injector.injectRuntimeReference(theClass);
			} finally {
				theIsFirstInjection.v = false;
			}
		}

		injector.injectTracingCode(theClass, theMethod);
		return true;
	}
}
