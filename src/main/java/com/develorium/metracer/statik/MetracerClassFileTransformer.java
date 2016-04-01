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

package com.develorium.metracer.statik;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.regex.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import com.develorium.metracer.*;
import com.develorium.metracer.asm.*;

public class MetracerClassFileTransformer implements ClassFileTransformer {
	private Patterns patterns = null;
	private RuntimeLogger runtimeLogger = new RuntimeLogger();
	private com.develorium.metracer.Runtime runtime = new com.develorium.metracer.Runtime(runtimeLogger);

	public MetracerClassFileTransformer(String theArguments) {
		if(theArguments == null)
			throw new RuntimeException("Arguments are missing");

		patterns = new Patterns(theArguments, null, StackTraceMode.DISABLED);
	}

	@Override
	public byte[] transform(ClassLoader theLoader, String theClassName, Class<?> theClassBeingRedefined, ProtectionDomain theProtectionDomain, byte[] theClassfileBuffer) throws IllegalClassFormatException {
		if(theClassName.startsWith("java/lang"))
			return theClassfileBuffer;

		try {
			InstrumentClassResult icr = instrumentClass(theClassfileBuffer, theLoader != null ? theLoader : getClass().getClassLoader());

			if(icr.hasSlf4jLogger)
				runtimeLogger.registerClassWithSlf4jLogger(theClassName.replaceAll("/", "."), theLoader);

			theClassfileBuffer = icr.bytecode;
		} catch(Throwable t) {
			System.err.format("Failed to instrument class %1$s, loader %2$s, error message: %3$s\n", theClassName, theLoader, t.toString());
			t.printStackTrace();
		}

		return theClassfileBuffer;
	}

	private static class InstrumentClassResult {
		byte[] bytecode = null;
		boolean hasSlf4jLogger = false;
	} 

	private InstrumentClassResult instrumentClass(byte theBytecode[], ClassLoader theLoader) {
		ClassReader reader = new ClassReader(theBytecode);
		ClassNode parsedClass = new ClassNode();
		reader.accept(parsedClass, 0);
		
		MetracerClassWriter writer = new MetracerClassWriter(reader, theLoader);
		MetracerClassVisitor visitor = new MetracerClassVisitor(writer, theLoader, patterns, parsedClass);
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);

		InstrumentClassResult rv = new InstrumentClassResult();
		rv.bytecode = visitor.getIsChanged() ? writer.toByteArray() : theBytecode;
		rv.hasSlf4jLogger = visitor.getHasSlf4Logger();
		return rv;
	}
}
