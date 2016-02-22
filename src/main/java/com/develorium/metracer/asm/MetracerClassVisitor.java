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

package com.develorium.metracer.asm;

import java.util.*;
import java.util.regex.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class MetracerClassVisitor extends ClassVisitor {
	private boolean isChanged = false;
	private boolean hasSlf4Logger = false;
	private String className = null;
	private Pattern pattern = null;
	private ClassNode parsedClass = null;

	public MetracerClassVisitor(ClassVisitor theClassVisitor, Pattern thePattern, ClassNode theParsedClass) {
		super(Opcodes.ASM5, theClassVisitor);
		pattern = thePattern;
		parsedClass = theParsedClass;
	}

	@Override
	public void visit(int theVersion, int theAccess, String theClassName, String theSignature, String theSuperClassName, String[] theInterfaces) {
		cv.visit(theVersion, theAccess, theClassName, theSignature, theSuperClassName, theInterfaces);
		className = theClassName;
	}

	@Override
	public FieldVisitor visitField(int theAccess, String theName, String theDescription, String theSignature, Object theValue) {
		FieldVisitor rv = super.visitField(theAccess, theName, theDescription, theSignature, theValue);

		if(!hasSlf4Logger) {
			String slf4jLoggerClassNameForCompare = com.develorium.metracer.Runtime.Slf4jLoggerClassName.replace(".", "/");
			theDescription.equals(String.format("L%1$s;", slf4jLoggerClassNameForCompare));
			hasSlf4Logger = true;
		}

		return rv;
	}

	@Override
	public MethodVisitor visitMethod(
		int theAccess, String theName, String theDescription,
		String theSignature, String[] theExceptions) {
		MethodVisitor methodVisitor = cv.visitMethod(theAccess, theName, theDescription, theSignature, theExceptions);
		boolean isMethodChanged = false;

		if(theName.equals("findClass") && theDescription.startsWith("(Ljava/lang/String;") && theDescription.endsWith("Ljava/lang/Class;")) {
			// Looks like a findClass of ClassLoader, need to drill a hole
			methodVisitor = new FindClassMethodMutator(className, api, methodVisitor, theAccess, theName, theDescription);
			isMethodChanged = isChanged = true;
		}

		if(!isMethodChanged) {
			String methodNameForPatternMatching = String.format("%1$s::%2$s", className.replace("/", "."), theName);
			
			if(pattern.matcher(methodNameForPatternMatching).find(0)) {
				List<MethodNode> methods = parsedClass.methods;
				MethodNode method = null;
				
				for(MethodNode m : methods) {
					if(m.name.equals(theName) && m.desc.equals(theDescription)) {
						method = m;
						break;
					}
				}
				
				methodVisitor = new PatternMatchedMethodMutator(className, method, api, methodVisitor, theAccess, theName, theDescription);
				isChanged = true;
			}
		}
		
		return methodVisitor;
	}

	public boolean getIsChanged() {
		return isChanged;
	}

	public boolean getHasSlf4Logger() {
		return hasSlf4Logger;
	}
}
