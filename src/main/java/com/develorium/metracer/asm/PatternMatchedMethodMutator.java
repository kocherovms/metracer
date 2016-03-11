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
		mv.visitLabel(startFinally);

		// ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		int systemClassLoaderVariableIndex = newLocal(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
		mv.visitVarInsn(ASTORE, systemClassLoaderVariableIndex);

		// Class<?> runtimeClass = Class.forName("com.develorium.metracer.Runtime", true, systemClassLoader);
		mv.visitLdcInsn("com.develorium.metracer.Runtime");
		mv.visitInsn(ICONST_1);
		mv.visitVarInsn(ALOAD, systemClassLoaderVariableIndex);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		int runtimeClassVariableIndex = newLocal(Type.getType("Ljava/lang/Class;"));
		mv.visitVarInsn(ASTORE, runtimeClassVariableIndex);

		// Class<?>[] traceEntryArgumentTypes = { Class.class, String.class, String[].class, Object[].class };
		mv.visitInsn(ICONST_4);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Class;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_2);
		mv.visitLdcInsn(Type.getType("[Ljava/lang/String;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_3);
		mv.visitLdcInsn(Type.getType("[Ljava/lang/Object;"));
		mv.visitInsn(AASTORE);
		int traceEntryArgumentTypesVariableIndex = newLocal(Type.getType("[Ljava/lang/Class;"));
		mv.visitVarInsn(ASTORE, traceEntryArgumentTypesVariableIndex);

		// Method traceEntryMethod = runtimeClass.getMethod("traceEntry", traceEntryArgumentTypes);
		mv.visitVarInsn(ALOAD, runtimeClassVariableIndex);
		mv.visitLdcInsn("traceEntry");
		mv.visitVarInsn(ALOAD, traceEntryArgumentTypesVariableIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
		int traceEntryMethodVariableIndex = newLocal(Type.getType("Ljava/lang/reflect/Method;"));
		mv.visitVarInsn(ASTORE, traceEntryMethodVariableIndex);

		// String[] argumentNames = null;
		int argumentNamesVariableIndex = newLocal(Type.getType("[Ljava/lang/String;"));
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, argumentNamesVariableIndex);

		// String[] argumentValues = null;
		int argumentValuesVariableIndex = newLocal(Type.getType("[Ljava/lang/Object;"));
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, argumentValuesVariableIndex);

		Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
		boolean areAnyArguments = argumentTypes != null && argumentTypes.length > 0;
		
		if(areAnyArguments) {
			// populate argumentNames array with names of method arguments
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

			//populate argumentValues array with values of method arguments
			loadArgArray();
			mv.visitVarInsn(ASTORE, argumentValuesVariableIndex);
		}

		// Object[] traceEntryArgumentValues = { ?.class, "testMethod", argumentNames, argumentValues };
		mv.visitInsn(ICONST_4);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(methodName);
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_2);
		mv.visitVarInsn(ALOAD, argumentNamesVariableIndex);
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_3);
		mv.visitVarInsn(ALOAD, argumentValuesVariableIndex);
		mv.visitInsn(AASTORE);
		int traceEntryArgumentValuesVariableIndex = newLocal(Type.getType("Ljava/lang/Object;"));
		mv.visitVarInsn(ASTORE, traceEntryArgumentValuesVariableIndex);

		// traceEntryMethod.invoke(null, traceEntryArgumentValues);
		mv.visitVarInsn(ALOAD, traceEntryMethodVariableIndex);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ALOAD, traceEntryArgumentValuesVariableIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitInsn(POP);

		Label methodEnterEnd = new Label(); 
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
		
		mv.visitTryCatchBlock(startFinally, methodEnterEnd, methodEnterCatchBlock, "java/lang/Throwable"); 

		mv.visitLocalVariable("systemClassLoader", "Ljava/lang/ClassLoader;", null, startFinally, methodEnterEnd, systemClassLoaderVariableIndex);
		mv.visitLocalVariable("runtimeClass", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", startFinally, methodEnterEnd, runtimeClassVariableIndex);
		mv.visitLocalVariable("traceEntryArgumentTypes", "[Ljava/lang/Class;", null, startFinally, methodEnterEnd, traceEntryArgumentTypesVariableIndex);
		mv.visitLocalVariable("traceEntryMethod", "Ljava/lang/reflect/Method;", null, startFinally, methodEnterEnd, traceEntryMethodVariableIndex);
		mv.visitLocalVariable("argumentNames", "[Ljava/lang/String;", null, startFinally, methodEnterEnd, argumentNamesVariableIndex);
		mv.visitLocalVariable("argumentValues", "[Ljava/lang/Object;", null, startFinally, methodEnterEnd, argumentValuesVariableIndex);
		mv.visitLocalVariable("traceEntryArgumentValues", "[Ljava/lang/Object;", null, startFinally, methodEnterEnd, traceEntryArgumentValuesVariableIndex);
		mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, methodEnterCatchStart, methodEnterCatchEnd, exceptionVariableIndex);
	}

	@Override
	protected void onMethodExit(int theOpcode) {
		if(theOpcode != ATHROW) {
			injectTraceExit(theOpcode);
		}
	}

	private void injectTraceExit(int theOpcode) {
		// need to grab return value from a stack into local variable before establishing new try / catch frame,
		// otherwise VerifyError would be thrown
		boolean isReturnValueBoxed = false;
		int returnValueVariableIndex = newLocal(Type.getType("[Ljava/lang/Object;"));
		Label methodExitEarlyStart = new Label(); 
		mv.visitLabel(methodExitEarlyStart);

		if(theOpcode == RETURN) {
			visitInsn(ACONST_NULL);
		} else {
			switch(theOpcode) {
			case LRETURN:
			case DRETURN:
			case FRETURN:
			case IRETURN:
				box(Type.getReturnType(methodDesc));
				isReturnValueBoxed = true;
				break;
			}
		}

		mv.visitVarInsn(ASTORE, returnValueVariableIndex);

		Label methodExitStart = new Label(); 
		mv.visitLabel(methodExitStart);

		// ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		int systemClassLoaderVariableIndex = newLocal(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
		mv.visitVarInsn(ASTORE, systemClassLoaderVariableIndex);

		// Class<?> runtimeClass = Class.forName("com.develorium.metracer.Runtime", true, systemClassLoader);
		mv.visitLdcInsn("com.develorium.metracer.Runtime");
		mv.visitInsn(ICONST_1);
		mv.visitVarInsn(ALOAD, systemClassLoaderVariableIndex);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		int runtimeClassVariableIndex = newLocal(Type.getType("Ljava/lang/Class;"));
		mv.visitVarInsn(ASTORE, runtimeClassVariableIndex);

		// Class<?>[] traceExitArgumentTypes = { Class.class, String.class, Object.class };
		mv.visitInsn(ICONST_3);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Class;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_2);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
		mv.visitInsn(AASTORE);
		int traceExitArgumentTypesVariableIndex = newLocal(Type.getType("[Ljava/lang/Class;"));
		mv.visitVarInsn(ASTORE, traceExitArgumentTypesVariableIndex);

		// Method traceExitMethod = runtimeClass.getMethod("traceExit", traceExitArgumentTypes);
		mv.visitVarInsn(ALOAD, runtimeClassVariableIndex);
		mv.visitLdcInsn("traceExit");
		mv.visitVarInsn(ALOAD, traceExitArgumentTypesVariableIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
		int traceExitMethodVariableIndex = newLocal(Type.getType("Ljava/lang/reflect/Method;"));
		mv.visitVarInsn(ASTORE, traceExitMethodVariableIndex);

		// Object[] traceExitArgumentValues = { ?.class, "testMethod", rv };
		mv.visitInsn(ICONST_3);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(methodName);
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_2);
		mv.visitVarInsn(ALOAD, returnValueVariableIndex);
		mv.visitInsn(AASTORE);
		int traceExitArgumentValuesVariableIndex = newLocal(Type.getType("Ljava/lang/Object;"));
		mv.visitVarInsn(ASTORE, traceExitArgumentValuesVariableIndex);

		// traceExitMethod.invoke(null, traceExitArgumentValues);
		mv.visitVarInsn(ALOAD, traceExitMethodVariableIndex);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ALOAD, traceExitArgumentValuesVariableIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitInsn(POP);

		Label methodExitEnd = new Label(); 
		mv.visitLabel(methodExitEnd);

		Label methodExitEndUltimate = new Label();
		mv.visitJumpInsn(GOTO, methodExitEndUltimate);
		int exceptionVariableIndex = newLocal(Type.getType("[Ljava/lang/Throwable;"));
		Label methodExitCatchBlock = new Label();
		mv.visitLabel(methodExitCatchBlock);
		mv.visitVarInsn(ASTORE, exceptionVariableIndex);
		Label methodExitCatchStart = new Label();
		Label methodExitCatchEnd = new Label();
		mv.visitLabel(methodExitCatchStart);
		mv.visitLabel(methodExitCatchEnd);
		mv.visitLabel(methodExitEndUltimate);

		mv.visitVarInsn(ALOAD, returnValueVariableIndex);

		if(isReturnValueBoxed)
			unbox(Type.getReturnType(methodDesc));
		
		mv.visitTryCatchBlock(methodExitStart, methodExitEnd, methodExitCatchBlock, "java/lang/Throwable"); 
		mv.visitLocalVariable("rv", "Ljava/lang/Object;", null, methodExitEarlyStart, methodExitEndUltimate, returnValueVariableIndex);
		mv.visitLocalVariable("systemClassLoader", "Ljava/lang/ClassLoader;", null, methodExitStart, methodExitEnd, systemClassLoaderVariableIndex);
		mv.visitLocalVariable("runtimeClass", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", methodExitStart, methodExitEnd, runtimeClassVariableIndex);
		mv.visitLocalVariable("traceExitArgumentTypes", "[Ljava/lang/Class;", null, methodExitStart, methodExitEnd, traceExitArgumentTypesVariableIndex);
		mv.visitLocalVariable("traceExitMethod", "Ljava/lang/reflect/Method;", null, methodExitStart, methodExitEnd, traceExitMethodVariableIndex);
		mv.visitLocalVariable("traceExitArgumentValues", "[Ljava/lang/Object;", null, methodExitStart, methodExitEnd, traceExitArgumentValuesVariableIndex);
		mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, methodExitCatchStart, methodExitCatchEnd, exceptionVariableIndex);
	}
}