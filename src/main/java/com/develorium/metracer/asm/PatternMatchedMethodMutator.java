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

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

class PatternMatchedMethodMutator extends AdviceAdapter {
	private String className = null;
	private String methodName = null;

	public PatternMatchedMethodMutator(String theClassName, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
		super(theApiVersion, theDelegatingMethodVisitor, theAccess, theMethodName, theMethodDescription);
		className = theClassName;
		methodName = theMethodName;
	}

	protected void onMethodEnter() {
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitLdcInsn(methodName);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ACONST_NULL);
		mv.visitMethodInsn(INVOKESTATIC, "com/develorium/metracer/Runtime", "traceEntry", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V", false);
	}
}