/*
 * Copyright 2015 Michael Kocherov
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
	private Runtime runtime = new Runtime(); // to force loading of Runtime

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

			if(cc.isFrozen()) {
				return classfileBuffer;
			}

			boolean wasInstrumented = false; //instrumentViaAnnotation(cc);

			if(!wasInstrumented && pattern != null)
				wasInstrumented = instrumentViaPattern(cc);

			if(canonicalClassName.indexOf("org.hibernate.loader.Loader") >= 0){
				while(loader != null) {
					System.out.println("kms@ loader = " + loader.toString());
					loader = loader.getParent();
				}
			}

		
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
	private boolean instrumentViaAnnotation(CtClass theClass) {
		boolean wasInstrumented = false;
		TracingCodeInjector.Logger logger = new TracingCodeInjector.Logger();

		for(CtMethod method : theClass.getDeclaredMethods()) {
			Object[] annotations = null;
			try {
				annotations = method.getAnnotations();
			} catch (ClassNotFoundException e) {
				System.err.format("Failed to get annotations of method %1$s: %2$s\n", 
						  method.getLongName(), e.toString());
				continue;
			}
			
			for(Object ann : annotations) {
				if(ann instanceof Traced) {
					try {
						if(instrumentMethod(theClass, method, logger))
							wasInstrumented = true;
					} catch(Exception e) {
						// Failure to instrument a method typically leads to a malformed class
						return false;
					}

					break;
				}
			}
		}

		return wasInstrumented;
	}
	private boolean instrumentViaPattern(CtClass theClass) {
		boolean wasInstrumented = false;
		TracingCodeInjector.Logger logger = new TracingCodeInjector.Logger();

		for(CtMethod method : theClass.getDeclaredMethods()) {
			String methodNameForPatternMatching = String.format("%1$s::%2$s", theClass.getName(), method.getName());

			if(pattern.matcher(methodNameForPatternMatching).find(0)) {
				try {
					if(instrumentMethod(theClass, method, logger))
						wasInstrumented = true;
				} catch(Exception e) {
					return false;
				}
			}
		}

		return wasInstrumented;
	}
	private boolean instrumentMethod(CtClass theClass, CtMethod theMethod, TracingCodeInjector.Logger theLogger) throws java.lang.Exception {
		try {
			if(!injector.isMethodInstrumentable(theMethod)) 
				return false;

			if(!theLogger.isInitialized())
				injector.initializeLogger(theClass, theLogger);

			injector.injectTracingCode(theClass, theMethod, theLogger);
			return true;
		} catch (Exception e) {
			System.err.format("Failed to add tracing to method %1$s: %2$s\n", theMethod.getLongName(), e.toString());
			throw e;
		}
	}
}
