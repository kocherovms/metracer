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
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;

class PatternMatchedMethodMutator extends AdviceAdapter {
	private String className = null;
	private MethodNode method = null;
	private String methodName = null;
	private boolean isStatic = false;
	private boolean isInvokeSpecialEncountered = false;
	private Label methodEnterStart = new Label(); 
	private Label methodEnterEnd = new Label(); 
	private Label startFinally = new Label(); 
	private Label endFinally = new Label(); 

	public PatternMatchedMethodMutator(String theClassName, MethodNode theMethod, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
		super(theApiVersion, theDelegatingMethodVisitor, theAccess, theMethodName, theMethodDescription);
		className = theClassName;
		method = theMethod;
		methodName = theMethodName;
		isStatic = (theAccess & Opcodes.ACC_STATIC) != 0;
	}

	@Override
	public void visitMaxs(int theMaxStack, int theMaxLocals) {
		mv.visitTryCatchBlock(startFinally,	endFinally, endFinally, null);
		mv.visitLabel(endFinally);
		injectTraceExit(ATHROW);
		mv.visitInsn(ATHROW);

		super.visitMaxs(theMaxStack, theMaxLocals);
	}

	@Override
	protected void onMethodEnter() {
		int argumentNamesVariableIndex = -1;
		mv.visitLabel(startFinally);
		Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
		boolean areAnyArguments = argumentTypes != null && argumentTypes.length > 0;
		mv.visitLabel(methodEnterStart);
		
		if(areAnyArguments) {
			argumentNamesVariableIndex = newLocal(Type.getType("[Ljava/lang/String;"));
			mv.visitLdcInsn(new Integer(argumentTypes.length));
			mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
			mv.visitVarInsn(ASTORE, argumentNamesVariableIndex);
			List<LocalVariableNode> localVariableNodes = method != null ? method.localVariables : null;
			TreeMap<Integer, String> localVariables = new TreeMap<Integer, String>();

			if(localVariableNodes != null) {
				for(LocalVariableNode node : localVariableNodes) {
					localVariables.put(node.index, node.name);
				}
			}
		
			for(int i = 0; i < argumentTypes.length; ++i) {
				int argIndex = isStatic ? i : i + 1;
				String localVariableName = localVariables.get(argIndex);
				String argumentName = localVariableName != null ? localVariableName : "$arg" + i;
				mv.visitVarInsn(ALOAD, argumentNamesVariableIndex);
				mv.visitLdcInsn(new Integer(i));
				mv.visitLdcInsn(argumentName);
				mv.visitInsn(AASTORE);
			}
		}

		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitLdcInsn(methodName);

		if(areAnyArguments) {
			mv.visitVarInsn(ALOAD, argumentNamesVariableIndex);
			loadArgArray();
		}
		else {
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ACONST_NULL);
		}

		mv.visitMethodInsn(INVOKESTATIC, "com/develorium/metracer/Runtime", "traceEntry", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V", false);
		mv.visitLabel(methodEnterEnd);
		Label methodEnterEndUltimate = new Label();
		mv.visitJumpInsn(GOTO, methodEnterEndUltimate);
		
		int exceptionVariableIndex = newLocal(Type.getType("[Ljava/lang/Throwable;"));
		Label methodEnterCatchBlock = new Label();
		mv.visitLabel(methodEnterCatchBlock);
		mv.visitVarInsn(ASTORE, exceptionVariableIndex);
		Label methodEnterCatchStart = new Label();
		Label methodEnterCatchEnd = new Label();
		mv.visitLabel(methodEnterCatchStart);
		mv.visitLabel(methodEnterCatchEnd);
		mv.visitLabel(methodEnterEndUltimate);
		
		mv.visitTryCatchBlock(methodEnterStart, methodEnterEnd, methodEnterCatchBlock, "java/lang/Throwable"); 

		if(argumentNamesVariableIndex > -1)
			mv.visitLocalVariable("argumentNames_4195e1f8144944ab856301acecbf6a43", "[Ljava/lang/String;", null, methodEnterStart, methodEnterEnd, argumentNamesVariableIndex);

		mv.visitLocalVariable("e_e9e8d057c71043328d688bec83715d7e", "Ljava/lang/Throwable;", null, methodEnterCatchStart, methodEnterCatchEnd, exceptionVariableIndex);
	}

	@Override
	protected void onMethodExit(int theOpcode) {
		if(theOpcode != ATHROW) {
			injectTraceExit(theOpcode);
		}
	}

	private void injectTraceExit(int theOpcode) {
		Label startLabel = new Label(); 
		Label endLabel = new Label(); 
		//mv.visitLabel(startLabel);

		if(theOpcode == RETURN) {
			visitInsn(ACONST_NULL);
		} else if(theOpcode == ARETURN || theOpcode == ATHROW) {
			dup();
		} else {
			if(theOpcode == LRETURN || theOpcode == DRETURN) {
				dup2();
			} else {
				dup();
			}

			box(Type.getReturnType(methodDesc));
		}

		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitLdcInsn(methodName);
		mv.visitMethodInsn(INVOKESTATIC, "com/develorium/metracer/Runtime", "traceExit", "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;)V", false);
		//mv.visitLabel(endLabel);
		//mv.visitTryCatchBlock(startLabel, endLabel, endLabel, null); // do nothing on any errors
	}
}