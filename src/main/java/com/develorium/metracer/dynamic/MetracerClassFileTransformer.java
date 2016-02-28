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

package com.develorium.metracer.dynamic;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.regex.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import com.develorium.metracer.asm.*;

public class MetracerClassFileTransformer implements ClassFileTransformer {
	private Agent agent = null;

	MetracerClassFileTransformer(Agent theAgent) {
		agent = theAgent;
	}

	@Override
	public byte[] transform(ClassLoader theLoader, String theClassName, Class<?> theClassBeingRedefined, ProtectionDomain theProtectionDomain, byte[] theClassfileBuffer) throws IllegalClassFormatException {
		Pattern pattern = agent.getMethodMatchingPattern();

		try {
			InstrumentClassResult icr = instrumentClass(theClassfileBuffer, theLoader != null ? theLoader : getClass().getClassLoader(), pattern);

			if(icr.isChanged)
				com.develorium.metracer.Runtime.say(String.format("%s (class loader %s) was instrumented", theClassName, theLoader != null ? theLoader.toString() : "<boostrap>"));

			return icr.bytecode;
		} catch(Throwable t) {
			System.err.format("Failed to instrument class %s, loader %s, error message: %s\n", theClassName, theLoader, t.toString());
			t.printStackTrace();
		}
	
		return theClassfileBuffer;
	}

	private static class InstrumentClassResult {
		boolean isChanged = false;
		byte[] bytecode = null;
	} 

	private InstrumentClassResult instrumentClass(byte theBytecode[], ClassLoader theLoader, Pattern thePattern) {
		ClassReader reader = new ClassReader(theBytecode);
		ClassNode parsedClass = new ClassNode();
		reader.accept(parsedClass, 0);
		
		MetracerClassWriter writer = new MetracerClassWriter(reader, theLoader);
		MetracerClassVisitor visitor = new MetracerClassVisitor(writer, thePattern, parsedClass);
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);

		InstrumentClassResult rv = new InstrumentClassResult();
		rv.isChanged = visitor.getIsChanged();
		rv.bytecode = visitor.getIsChanged() ? writer.toByteArray() : theBytecode;
		return rv;
	}
}
