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

class FindClassMethodMutator extends AdviceAdapter {
	private String className = null;

	public FindClassMethodMutator(String theClassName, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
		super(theApiVersion, theDelegatingMethodVisitor, theAccess, theMethodName, theMethodDescription);
		className = theClassName;
	}

	protected void onMethodEnter() {
		// Corresponding code:
		//public Class<?> findClass(String className) {
		//	if(className.startsWith("com.develorium.metracer")) {
		//		ClassLoader loader = ClassLoader.class.cast(this);
		//		
		//		while(loader != null) {
		//			try {
		//				java.lang.reflect.Field f = ClassLoader.class.getDeclaredField("classes");
		//				f.setAccessible(true);
		//				java.util.Vector<?> classes = (java.util.Vector<?>)f.get(loader);
		//
		//				for(int i = 0; i < classes.size(); i++) {
		//					java.lang.Class<?> c = (java.lang.Class<?>)classes.get(i);
		//					
		//					if(c != null && c.getName().equals(className)) {
		//						return c;
		//					}
		//				}
		//			} catch(Exception e) {
		//			}
		//			
		//			loader = loader.getParent();
		//		}
		//	}
		//	return null;
		//}
		//System.out.println("kms@ FindClassMethodMutator.onMethodEnter for className = " + className);
		//Label l0 = new Label();
		//Label l1 = new Label();
		//Label l2 = new Label();
		//mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
		//Label l3 = new Label();
		//Label l4 = new Label();
		//mv.visitTryCatchBlock(l3, l4, l2, "java/lang/Exception");
		//Label l5 = new Label();
		//mv.visitLabel(l5);
		//mv.visitVarInsn(ALOAD, 1);
		//mv.visitLdcInsn("com.develorium.metracer");
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
		//Label l6 = new Label();
		//mv.visitJumpInsn(IFEQ, l6);
		//Label l7 = new Label();
		//mv.visitLabel(l7);
		//mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		//mv.visitVarInsn(ALOAD, 0);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "cast", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		//mv.visitTypeInsn(CHECKCAST, "java/lang/ClassLoader");
		//mv.visitVarInsn(ASTORE, 2);
		//Label l8 = new Label();
		//mv.visitLabel(l8);
		//Label l9 = new Label();
		//mv.visitJumpInsn(GOTO, l9);
		//mv.visitLabel(l0);
		//mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/ClassLoader"}, 0, null);
		//mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		//mv.visitLdcInsn("classes");
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
		//mv.visitVarInsn(ASTORE, 3);
		//Label l10 = new Label();
		//mv.visitLabel(l10);
		//mv.visitVarInsn(ALOAD, 3);
		//mv.visitInsn(ICONST_1);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
		//Label l11 = new Label();
		//mv.visitLabel(l11);
		//mv.visitVarInsn(ALOAD, 3);
		//mv.visitVarInsn(ALOAD, 2);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		//mv.visitTypeInsn(CHECKCAST, "java/util/Vector");
		//mv.visitVarInsn(ASTORE, 4);
		//Label l12 = new Label();
		//mv.visitLabel(l12);
		//mv.visitInsn(ICONST_0);
		//mv.visitVarInsn(ISTORE, 5);
		//Label l13 = new Label();
		//mv.visitLabel(l13);
		//Label l14 = new Label();
		//mv.visitJumpInsn(GOTO, l14);
		//Label l15 = new Label();
		//mv.visitLabel(l15);
		//mv.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/lang/reflect/Field", "java/util/Vector", Opcodes.INTEGER}, 0, null);
		//mv.visitVarInsn(ALOAD, 4);
		//mv.visitVarInsn(ILOAD, 5);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "get", "(I)Ljava/lang/Object;", false);
		//mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
		//mv.visitVarInsn(ASTORE, 6);
		//Label l16 = new Label();
		//mv.visitLabel(l16);
		//mv.visitVarInsn(ALOAD, 6);
		//mv.visitJumpInsn(IFNULL, l3);
		//mv.visitVarInsn(ALOAD, 6);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
		//mv.visitVarInsn(ALOAD, 1);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
		//mv.visitJumpInsn(IFEQ, l3);
		//Label l17 = new Label();
		//mv.visitLabel(l17);
		//mv.visitVarInsn(ALOAD, 6);
		//mv.visitLabel(l1);
		//mv.visitInsn(ARETURN);
		//mv.visitLabel(l3);
		//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		//mv.visitIincInsn(5, 1);
		//mv.visitLabel(l14);
		//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		//mv.visitVarInsn(ILOAD, 5);
		//mv.visitVarInsn(ALOAD, 4);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "size", "()I", false);
		//mv.visitJumpInsn(IF_ICMPLT, l15);
		//mv.visitLabel(l4);
		//Label l18 = new Label();
		//mv.visitJumpInsn(GOTO, l18);
		//mv.visitLabel(l2);
		//mv.visitFrame(Opcodes.F_FULL, 3, new Object[] { className, "java/lang/String", "java/lang/ClassLoader"}, 1, new Object[] {"java/lang/Exception"});
		//mv.visitVarInsn(ASTORE, 3);
		//mv.visitLabel(l18);
		//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		//mv.visitVarInsn(ALOAD, 2);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "getParent", "()Ljava/lang/ClassLoader;", false);
		//mv.visitVarInsn(ASTORE, 2);
		//mv.visitLabel(l9);
		//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		//mv.visitVarInsn(ALOAD, 2);
		//mv.visitJumpInsn(IFNONNULL, l0);
		//mv.visitLocalVariable("loader", "Ljava/lang/ClassLoader;", null, l8, l6, 2);
		//mv.visitLocalVariable("f", "Ljava/lang/reflect/Field;", null, l10, l4, 3);
		//mv.visitLocalVariable("classes", "Ljava/util/Vector;", "Ljava/util/Vector<*>;", l12, l4, 4);
		//mv.visitLocalVariable("i", "I", null, l13, l4, 5);
		//mv.visitLocalVariable("c", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l16, l3, 6);
		// ==========

		//Label l0 = new Label();
		//mv.visitLabel(l0);
		//mv.visitLineNumber(8, l0);
		//mv.visitVarInsn(ALOAD, 1);
		//mv.visitLdcInsn("com.develorium.metracer");
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
		//Label l1 = new Label();
		//mv.visitJumpInsn(IFEQ, l1);
		//Label l2 = new Label();
		//mv.visitLabel(l2);
		//mv.visitLineNumber(9, l2);
		//mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		//mv.visitVarInsn(ALOAD, 0);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "cast", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		//mv.visitTypeInsn(CHECKCAST, "java/lang/ClassLoader");
		//mv.visitVarInsn(ASTORE, 2);
		//Label l3 = new Label();
		//mv.visitLabel(l3);
		//mv.visitLineNumber(11, l3);
		//Label l4 = new Label();
		//mv.visitJumpInsn(GOTO, l4);
		//Label l5 = new Label();
		//mv.visitLabel(l5);
		//mv.visitLineNumber(12, l5);
		//mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/ClassLoader"}, 0, null);
		//mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		//mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		//mv.visitInsn(DUP);
		//mv.visitLdcInsn("kms@ loader = ");
		//mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
		//mv.visitVarInsn(ALOAD, 2);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
		//Label l6 = new Label();
		//mv.visitLabel(l6);
		//mv.visitLineNumber(13, l6);
		//mv.visitVarInsn(ALOAD, 2);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "getParent", "()Ljava/lang/ClassLoader;", false);
		//mv.visitVarInsn(ASTORE, 2);
		//mv.visitLabel(l4);
		//mv.visitLineNumber(11, l4);
		//mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		//mv.visitVarInsn(ALOAD, 2);
		//mv.visitJumpInsn(IFNONNULL, l5);
		//mv.visitLocalVariable("loader", "Ljava/lang/ClassLoader;", null, l3, l1, 2);
		// =======
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(8, l0);
		mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z", false);
		Label l1 = new Label();
		mv.visitJumpInsn(IFEQ, l1);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLineNumber(9, l2);
		mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "cast", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/lang/ClassLoader");
		mv.visitVarInsn(ASTORE, 2);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLineNumber(10, l3);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("kms@ loader = ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
		mv.visitLabel(l1);
		mv.visitLocalVariable("loader", "Ljava/lang/ClassLoader;", null, l3, l1, 2);
	}

	//public void visitMaxs(int maxStack, int maxLocals) {
	//	//mv.visitMaxs(maxStack + 10, maxLocals + 10);
	//	//super.visitMaxs(maxStack + 20, maxLocals + 20);
	//}
}