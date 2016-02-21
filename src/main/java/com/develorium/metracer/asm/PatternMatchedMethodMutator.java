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
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

class PatternMatchedMethodMutator extends AdviceAdapter {
	private String className = null;
	private String methodName = null;
	private String methodDescription = null;
	private boolean isConstructor = false;
	private boolean isInvokeSpecialEncountered = false;
	private class LocalVariable {
		String name;
		String type;
	}
	private TreeMap<Integer, LocalVariable> localVariables = new TreeMap<Integer, LocalVariable>();
	private Label startFinally = new Label(); 
	private Label endFinally = new Label(); 

	public PatternMatchedMethodMutator(String theClassName, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
		super(theApiVersion, theDelegatingMethodVisitor, theAccess, theMethodName, theMethodDescription);
		className = theClassName;
		methodName = theMethodName;
		methodDescription = theMethodDescription;
		isConstructor = methodName.equals("<init>");
	}

	@Override
	public void visitLocalVariable(String theName, String theDescription, String theSignature, Label theStart, Label theEnd, int theIndex) {
		LocalVariable localVariable = new LocalVariable();
		localVariable.name = theName;
		localVariable.type = theDescription;
		localVariables.put(theIndex, localVariable);
		super.visitLocalVariable(theName, theDescription, theSignature, theStart, theEnd, theIndex);
	}

	@Override
	public void visitCode() {
		super.visitCode();

		if(!isConstructor)
			mv.visitLabel(startFinally);
	}

	@Override
	public void visitMethodInsn(int theOpcode, String theOwner, String theName, String theDescription, boolean theIsInterfaceMethod) {
		super.visitMethodInsn(theOpcode, theOwner, theName, theDescription, theIsInterfaceMethod);

		if(isConstructor && !isInvokeSpecialEncountered) {
			// try / finally within contstructors can be etablished ONLY after invokespecial.
			// Otherwise there might be a VerifyError complaining:
			// ... Stack map does not match the one at exception handler XXX..., Reason: "Current frame's flags are not assignable to stack map frame's"
			// Happens because before invokespecial initial stack map frame could contain flag flagIsUninit.
			// Presence of this flag violates assignability of an initial stack map frame to the generated one by ASM

			if(theOpcode == INVOKESPECIAL) {
				isInvokeSpecialEncountered = true;
				mv.visitLabel(startFinally);
			}
		}
	}

	@Override
	public void visitMaxs(int theMaxStack, int theMaxLocals) {
		mv.visitTryCatchBlock(startFinally,	endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		onFinally(ATHROW);
		mv.visitInsn(ATHROW);
		super.visitMaxs(theMaxStack, theMaxLocals);
	}

	@Override
	protected void onMethodEnter() {
		Type[] argumentTypes = Type.getArgumentTypes(methodDescription);
		String[] argumentNames = null;
	
		if(argumentTypes != null && argumentTypes.length > 0) {
			argumentNames = new String[argumentTypes.length];
		
			for(int i = 0; i < argumentTypes.length; ++i) {
				LocalVariable localVariable = localVariables.get(i);
				argumentNames[i] = localVariable != null ? localVariable.name : "$arg" + i;
			}
		}
	
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitLdcInsn(methodName);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ACONST_NULL);
		mv.visitMethodInsn(INVOKESTATIC, "com/develorium/metracer/Runtime", "traceEntry", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V", false);
	}

	@Override
	protected void onMethodExit(int theOpcode) {
		if(theOpcode != ATHROW) {
			onFinally(theOpcode);
		}
	}

	private void onFinally(int theOpcode) {
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitLdcInsn(methodName);
		mv.visitLdcInsn(theOpcode != ATHROW);
		mv.visitMethodInsn(INVOKESTATIC, "com/develorium/metracer/Runtime", "traceExit", "(Ljava/lang/Class;Ljava/lang/String;Z)V", false);
	}
}