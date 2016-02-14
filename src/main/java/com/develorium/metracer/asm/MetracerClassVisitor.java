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

import java.util.regex.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class MetracerClassVisitor extends ClassVisitor {
	private boolean isChanged = false;
	private String className = null;
	private Pattern pattern = null;

	public MetracerClassVisitor(ClassVisitor theClassVisitor, Pattern thePattern) {
		super(Opcodes.ASM5, theClassVisitor);
		pattern = thePattern;
	}

	public void visit(int theVersion, int theAccess, String theClassName, String theSignature, String theSuperClassName, String[] theInterfaces) {
		cv.visit(theVersion, theAccess, theClassName, theSignature, theSuperClassName, theInterfaces);
		className = theClassName;
	}

	public MethodVisitor visitMethod(
		int theAccess, String theName, String theDescription,
		String theSignature, String[] theExceptions) {
		MethodVisitor methodVisitor = cv.visitMethod(theAccess, theName, theDescription, theSignature, theExceptions);
		boolean isMethodChanged = false;

		if(theName.equals("findClass")) {
			FindClassMethodMutator.MethodSignature signature = null;

			switch(theDescription) {
			case "(Ljava/lang/String;)Ljava/lang/Class;":
				signature = FindClassMethodMutator.MethodSignature.CLASSIC;
				break;
			case "(Ljava/lang/String;ZZ)Ljava/lang/Class;":
				signature = FindClassMethodMutator.MethodSignature.JBOSS;
				break;
			}

			if(signature != null) {
				methodVisitor = new FindClassMethodMutator(className, signature, api, methodVisitor, theAccess, theName, theDescription);
				isMethodChanged = isChanged = true;
			}
		}

		if(!isMethodChanged) {
			String methodNameForPatternMatching = String.format("%1$s::%2$s", className.replace("/", "."), theName);
			
			if(pattern.matcher(methodNameForPatternMatching).find(0)) {
				methodVisitor = new PatternMatchedMethodMutator(className, api, methodVisitor, theAccess, theName, theDescription);
				isChanged = true;
			}
		}
		
		return methodVisitor;
	}

	public boolean getIsChanged() {
		return isChanged;
	}
}
