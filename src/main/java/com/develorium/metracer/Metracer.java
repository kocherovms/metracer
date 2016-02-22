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
import org.objectweb.asm.*;
import com.develorium.metracer.asm.*;

public class Metracer implements ClassFileTransformer {
	private Pattern pattern = null;
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

		try {
			InstrumentClassResult icr = instrumentClass(classfileBuffer, loader);

			if(icr.hasSlf4jLogger)
				Runtime.registerClassWithSlf4jLogger(canonicalClassName, loader);

			classfileBuffer = icr.bytecode;
		} catch(Throwable t) {
			System.out.println("ERROR while instrumenting class " + className + ", loader " + loader + ", error message: " + t.toString());
			t.printStackTrace();
		}

		return classfileBuffer;
	}

	private static class InstrumentClassResult {
		byte[] bytecode = null;
		boolean hasSlf4jLogger = false;
	} 

	private InstrumentClassResult instrumentClass(byte theBytecode[], ClassLoader theLoader) {
		ClassReader reader = new ClassReader(theBytecode);
		MetracerClassWriter writer = new MetracerClassWriter(reader, theLoader);
		MetracerClassVisitor visitor = new MetracerClassVisitor(writer, pattern);
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);

		InstrumentClassResult rv = new InstrumentClassResult();
		rv.bytecode = visitor.getIsChanged() ? writer.toByteArray() : theBytecode;
		rv.hasSlf4jLogger = visitor.getHasSlf4Logger();
		return rv;
	}
}
