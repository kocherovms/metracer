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
	// TODO: get rid of this enum - onMethodEnter must properly handle all possible signatures
	public enum MethodSignature {
		CLASSIC,  // e.g. findClass(String theClassName)
		JBOSS     // e.g. findClass(String theClassName, boolean theExportsOnly, final boolean theResolve)
	}

	private String className = null;
	private MethodSignature signature = MethodSignature.CLASSIC;

	public FindClassMethodMutator(String theClassName, MethodSignature theSignature, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
		super(theApiVersion, theDelegatingMethodVisitor, theAccess, theMethodName, theMethodDescription);
		className = theClassName;
		signature = theSignature;
	}

	protected void onMethodEnter() {
		//Corresponding code:
		//if(ClassLoader.class.isAssignableFrom(Program.class)) {
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
		//}
		int loaderIndex = newLocal(Type.getType("Ljava/lang/ClassLoader;"));
		int fIndex = newLocal(Type.getType("Ljava/lang/reflect/Field;"));
		int classesIndex = newLocal(Type.getType("Ljava/util/Vector;"));
		int iIndex = newLocal(Type.getType("I"));
		int cIndex = newLocal(Type.getType("Ljava/lang/Class;"));
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
		Label l3 = new Label();
		Label l4 = new Label();
		mv.visitTryCatchBlock(l3, l4, l2, "java/lang/Exception");
		Label l5 = new Label();
		mv.visitLabel(l5);
		mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z", false);
		Label l6 = new Label();
		mv.visitJumpInsn(IFEQ, l6);
		Label l7 = new Label();
		mv.visitLabel(l7);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn("com.develorium.metracer");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(IFEQ, l6);
		Label l8 = new Label();
		mv.visitLabel(l8);
		mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "cast", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/lang/ClassLoader");
		mv.visitVarInsn(ASTORE, loaderIndex);
		Label l9 = new Label();
		mv.visitLabel(l9);
		Label l10 = new Label();
		mv.visitJumpInsn(GOTO, l10);
		mv.visitLabel(l0);
		mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/ClassLoader"}, 0, null);
		mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
		mv.visitLdcInsn("classes");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
		mv.visitVarInsn(ASTORE, fIndex);
		Label l11 = new Label();
		mv.visitLabel(l11);
		mv.visitVarInsn(ALOAD, fIndex);
		mv.visitInsn(ICONST_1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
		Label l12 = new Label();
		mv.visitLabel(l12);
		mv.visitVarInsn(ALOAD, fIndex);
		mv.visitVarInsn(ALOAD, loaderIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/util/Vector");
		mv.visitVarInsn(ASTORE, classesIndex);
		Label l13 = new Label();
		mv.visitLabel(l13);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, iIndex);
		Label l14 = new Label();
		mv.visitLabel(l14);
		Label l15 = new Label();
		mv.visitJumpInsn(GOTO, l15);
		Label l16 = new Label();
		mv.visitLabel(l16);
		mv.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/lang/reflect/Field", "java/util/Vector", Opcodes.INTEGER}, 0, null);
		mv.visitVarInsn(ALOAD, classesIndex);
		mv.visitVarInsn(ILOAD, iIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "get", "(I)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
		mv.visitVarInsn(ASTORE, cIndex);
		Label l17 = new Label();
		mv.visitLabel(l17);
		mv.visitVarInsn(ALOAD, cIndex);
		mv.visitJumpInsn(IFNULL, l3);
		mv.visitVarInsn(ALOAD, cIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
		mv.visitJumpInsn(IFEQ, l3);
		Label l18 = new Label();
		mv.visitLabel(l18);
		mv.visitVarInsn(ALOAD, cIndex);
		mv.visitLabel(l1);
		mv.visitInsn(ARETURN);
		mv.visitLabel(l3);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitIincInsn(iIndex, 1);
		mv.visitLabel(l15);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ILOAD, iIndex);
		mv.visitVarInsn(ALOAD, classesIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "size", "()I", false);
		mv.visitJumpInsn(IF_ICMPLT, l16);
		mv.visitLabel(l4);
		Label l19 = new Label();
		mv.visitJumpInsn(GOTO, l19);
		mv.visitLabel(l2);

		switch(signature) {
		case CLASSIC: 
			mv.visitFrame(Opcodes.F_FULL, 3, new Object[] {className, "java/lang/String", "java/lang/ClassLoader"}, 1, new Object[] {"java/lang/Exception"});
			break;
		case JBOSS:
			mv.visitFrame(Opcodes.F_FULL, 5, new Object[] {className, "java/lang/String", Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/ClassLoader"}, 1, new Object[] {"java/lang/Exception"});
			break;
		}

		mv.visitVarInsn(ASTORE, fIndex);
		mv.visitLabel(l19);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, loaderIndex);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "getParent", "()Ljava/lang/ClassLoader;", false);
		mv.visitVarInsn(ASTORE, loaderIndex);
		mv.visitLabel(l10);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, loaderIndex);
		mv.visitJumpInsn(IFNONNULL, l0);
		mv.visitLabel(l6);
		mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
		mv.visitLocalVariable("loader", "Ljava/lang/ClassLoader;", null, l9, l6, loaderIndex);
		mv.visitLocalVariable("f", "Ljava/lang/reflect/Field;", null, l11, l4, fIndex);
		mv.visitLocalVariable("classes", "Ljava/util/Vector;", "Ljava/util/Vector<*>;", l13, l4, classesIndex);
		mv.visitLocalVariable("i", "I", null, l14, l4, iIndex);
		mv.visitLocalVariable("c", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l17, l3, cIndex);
	}
}