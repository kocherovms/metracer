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
import javassist.bytecode.*;

public class Metracer implements ClassFileTransformer {
	private Pattern pattern = null;
	private ClassPools classPools = new ClassPools();
	private Runtime runtime = new Runtime();

	public Metracer(String theArguments) throws Exception {
		if(theArguments == null)
			throw new Exception("Arguments are missing");

		final String PatternStanza = "pattern=";

		if(!theArguments.startsWith(PatternStanza)) 
			throw new Exception("First argument must be a \"pattern\"");
				
		String patternString = theArguments.substring(PatternStanza.length());

		if(patternString.isEmpty())
			throw new Exception("Pattern is not specified");

		try {
			pattern = Pattern.compile(patternString);
		} catch(PatternSyntaxException e) {
			throw new Exception(String.format("Provided pattern \"%1$s\" is malformed: %2$s\n", patternString, e.toString()));
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

			if(isClassLoader(cc))
				wasInstrumented = wasInstrumented || tuneClassLoader(cc);

			if(hasSlf4jLogger(cc))
				Runtime.registerClassWithSlf4jLogger(cc.getName(), loader);

			wasInstrumented = wasInstrumented || instrumentClass(cc);
		
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

	private boolean isClassLoader(CtClass theClass) {
		try {
			while(theClass != null) {
				if(theClass.getName().equals(ClassLoader.class.getName()))
					return true;

				theClass = theClass.getSuperclass();
			}
		} catch(javassist.NotFoundException e) {
			System.err.format("Failed to qualify %1$s as a class loader: %2$s", theClass.getName(), e.toString());
			return false;
		}

		return false;
	}

	private boolean hasSlf4jLogger(CtClass theClass) {
		String slf4jSignature = Descriptor.of(Runtime.Slf4jLoggerClassName);
		CtField[] fields = theClass.getDeclaredFields();

		for(CtField field : fields) {
			if(field.getSignature().equals(slf4jSignature)) {
				return true;
			}
		}

		return false;
	}

	// Method allows to use classes from com.develorium.metracer for class loaders with strict isolation, e.g. JBoss module class loader
	private boolean tuneClassLoader(CtClass theClass) {
		boolean wasInstrumented = false;

		for(CtMethod method : theClass.getDeclaredMethods()) {
			try {
				if(method.getName().equals("findClass")) {
					TracingCodeInjector.injectDirectAccessToMetracerClasses(method);
					wasInstrumented = true;
					break;
				}
			} catch (Exception e) {
				System.err.format("Failed to tune class loader %1$s: %2$s\n", theClass.getName(), e.toString());
			}
		}

		return wasInstrumented;
	}

	private boolean makeSlf4jLoggerAutoRegisration(CtClass theClass) {
		boolean wasInstrumented = false;
		String descriptor = Descriptor.of(String.class.getName());
		try {
			CtConstructor ctor = theClass.getConstructor(descriptor);
			ctor.insertBefore(String.format("%1$s.registerLogger(this, $1);", Runtime.class.getName()));
			wasInstrumented = true;
		} catch(NotFoundException e) {
			System.err.format("Failed to locate constructor with descriptor \"%1$s\" within class \"%2$s\" which is ought to be there: %3$s\n", 
				descriptor, theClass.getName(), e.toString());
		} catch(CannotCompileException e) {
			System.err.format("Failed to make logger auto registration within class \"%1$s\": %2$s\n", theClass.getName(), e.toString());
		}

		return wasInstrumented;
	}

	private boolean instrumentClass(CtClass theClass) {
		boolean wasInstrumented = false;

		for(CtMethod method : theClass.getDeclaredMethods()) {
			try {
				final String methodNameForPatternMatching = String.format("%1$s::%2$s", theClass.getName(), method.getName());
				
				if(!pattern.matcher(methodNameForPatternMatching).find(0)) 
					continue;

				if(instrumentMethod(theClass, method))
					wasInstrumented = true;
			} catch (Exception e) {
				System.err.format("Failed to add tracing to method %1$s: %2$s\n", method.getLongName(), e.toString());
				// class can be damaged in this case so don't try to proceed with half-instrumented class
				return false;
			}
		}

		return wasInstrumented;
	}

	private boolean instrumentMethod(CtClass theClass, CtMethod theMethod) throws java.lang.Exception {
		if(!TracingCodeInjector.isMethodInstrumentable(theMethod)) 
			return false;

		TracingCodeInjector.injectTracingCode(theClass, theMethod);
		return true;
	}
}
