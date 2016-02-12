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
import javassist.*;
import javassist.bytecode.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class Metracer implements ClassFileTransformer {
	private Pattern pattern = null;
	private ClassPools classPools = new ClassPools();
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

	//if(ClassLoader.class.isAssignableFrom(getClass()))

	public byte[] transform(ClassLoader loader, String className,
				Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
				byte[] classfileBuffer) throws IllegalClassFormatException {
		final String canonicalClassName = className.replaceAll("/", ".");

		if(canonicalClassName.indexOf("java.lang") == 0)
			return classfileBuffer;

		try {
			classfileBuffer = crackClassLoader(classfileBuffer);
		} catch(Throwable t) {
			System.out.println("kms@ ERROR " + t.toString());
			t.printStackTrace();
		}

		return classfileBuffer;

		//ClassPool cp = classPools.getClassPool(loader);
		//cp.insertClassPath(new ByteArrayClassPath(canonicalClassName, classfileBuffer));
		//
		//try {
		//	CtClass cc = cp.get(canonicalClassName);
		//
		//	if(!isInstrumentable(cc)) 
		//		return classfileBuffer;
		//
		//	boolean wasInstrumented = false;
		//
		//	if(isClassLoader(cc))
		//		wasInstrumented = wasInstrumented || tuneClassLoader(cc);
		//
		//	if(hasSlf4jLogger(cc))
		//		Runtime.registerClassWithSlf4jLogger(cc.getName(), loader);
		//
		//	wasInstrumented = wasInstrumented || instrumentClass(cc);
		//
		//	if(wasInstrumented) {
		//		try {
		//			return cc.toBytecode();
		//		} catch (Exception e) {
		//			System.err.format("Failed to compile instrumented class %1$s: %2$s\n",
		//					  canonicalClassName, e.toString());
		//		}
		//	}
		//} catch (NotFoundException e) {
		//	System.err.format("Failed to register class %1$s in a javaassist ClassPool: %2$s\n",
		//			  canonicalClassName, e.toString());
		//}
		//
		//return classfileBuffer;
	}

	class FindClassMethodCracker extends AdviceAdapter {
		private String className = null;

		public FindClassMethodCracker(String theClassName, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
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
			System.out.println("kms@ FindClassMethodCracker.onMethodEnter for className = " + className);
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
			Label l3 = new Label();
			Label l4 = new Label();
			mv.visitTryCatchBlock(l3, l4, l2, "java/lang/Exception");
			Label l5 = new Label();
			mv.visitLabel(l5);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("com.develorium.metracer");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
			Label l6 = new Label();
			mv.visitJumpInsn(IFEQ, l6);
			Label l7 = new Label();
			mv.visitLabel(l7);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "cast", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "java/lang/ClassLoader");
			mv.visitVarInsn(ASTORE, 2);
			Label l8 = new Label();
			mv.visitLabel(l8);
			Label l9 = new Label();
			mv.visitJumpInsn(GOTO, l9);
			mv.visitLabel(l0);
			mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/ClassLoader"}, 0, null);
			mv.visitLdcInsn(Type.getType("Ljava/lang/ClassLoader;"));
			mv.visitLdcInsn("classes");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
			mv.visitVarInsn(ASTORE, 3);
			Label l10 = new Label();
			mv.visitLabel(l10);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
			Label l11 = new Label();
			mv.visitLabel(l11);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "java/util/Vector");
			mv.visitVarInsn(ASTORE, 4);
			Label l12 = new Label();
			mv.visitLabel(l12);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, 5);
			Label l13 = new Label();
			mv.visitLabel(l13);
			Label l14 = new Label();
			mv.visitJumpInsn(GOTO, l14);
			Label l15 = new Label();
			mv.visitLabel(l15);
			mv.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/lang/reflect/Field", "java/util/Vector", Opcodes.INTEGER}, 0, null);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitVarInsn(ILOAD, 5);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "get", "(I)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
			mv.visitVarInsn(ASTORE, 6);
			Label l16 = new Label();
			mv.visitLabel(l16);
			mv.visitVarInsn(ALOAD, 6);
			mv.visitJumpInsn(IFNULL, l3);
			mv.visitVarInsn(ALOAD, 6);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
			mv.visitJumpInsn(IFEQ, l3);
			Label l17 = new Label();
			mv.visitLabel(l17);
			mv.visitVarInsn(ALOAD, 6);
			mv.visitLabel(l1);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitIincInsn(5, 1);
			mv.visitLabel(l14);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ILOAD, 5);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", "size", "()I", false);
			mv.visitJumpInsn(IF_ICMPLT, l15);
			mv.visitLabel(l4);
			Label l18 = new Label();
			mv.visitJumpInsn(GOTO, l18);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_FULL, 3, new Object[] { className, "java/lang/String", "java/lang/ClassLoader"}, 1, new Object[] {"java/lang/Exception"});
			mv.visitVarInsn(ASTORE, 3);
			mv.visitLabel(l18);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "getParent", "()Ljava/lang/ClassLoader;", false);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitLabel(l9);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitJumpInsn(IFNONNULL, l0);
			mv.visitLocalVariable("loader", "Ljava/lang/ClassLoader;", null, l8, l6, 2);
			mv.visitLocalVariable("f", "Ljava/lang/reflect/Field;", null, l10, l4, 3);
			mv.visitLocalVariable("classes", "Ljava/util/Vector;", "Ljava/util/Vector<*>;", l12, l4, 4);
			mv.visitLocalVariable("i", "I", null, l13, l4, 5);
			mv.visitLocalVariable("c", "Ljava/lang/Class;", "Ljava/lang/Class<*>;", l16, l3, 6);
		}
	}

	class PatternMatchedMethodModifier extends AdviceAdapter {
		private String className = null;
		private String methodName = null;

		public PatternMatchedMethodModifier(String theClassName, int theApiVersion, MethodVisitor theDelegatingMethodVisitor, int theAccess, String theMethodName, String theMethodDescription) {
			super(theApiVersion, theDelegatingMethodVisitor, theAccess, theMethodName, theMethodDescription);
			className = theClassName;
			methodName = theMethodName;
		}

		protected void onMethodEnter() {
			System.out.println("kms@ PatternMatchedMethodModifier.onMethodEnter for className = " + className);
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitLdcInsn("[agent] LOG!!! " + className + "." + methodName);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

			//mv.visitLdcInsn(Type.getType(String.format("L%1$s;", className)));
			//mv.visitLdcInsn(methodName);
			//mv.visitInsn(ACONST_NULL);
			//mv.visitInsn(ACONST_NULL);
			//mv.visitMethodInsn(INVOKESTATIC, "com/develorium/m333etracer/Runtime", "traceEntry", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V", false);
		}

		public void visitMaxs(int stack, int locals) { 
			System.out.println("kms@ stack = " + stack + ", locals = " + locals);
			super.visitMaxs(stack + 100, locals + 100); 
		} 
	}

	//package asmtest;
	//
	//public class AsmTest {
	//	static class Runtime {
	//		public static void traceEntry(Class<?> theClass, String theMethodName, String theArgumentNames[], Object theArgumentValues[]) {
	//			
	//		}
	//	}
	//	public static void main(String args[]) {
	//	}
	//	public Class<?> findClass(String className) {
	//		if(className.startsWith("com.develorium.metracer")) {
	//			ClassLoader loader = ClassLoader.class.cast(this);
	//			
	//			while(loader != null) {
	//				try {
	//					java.lang.reflect.Field f = ClassLoader.class.getDeclaredField("classes");
	//					f.setAccessible(true);
	//					java.util.Vector<?> classes = (java.util.Vector<?>)f.get(loader);
	//
	//					for(int i = 0; i < classes.size(); i++) {
	//						java.lang.Class<?> c = (java.lang.Class<?>)classes.get(i);
	//						
	//						if(c != null && c.getName().equals(className)) {
	//							return c;
	//						}
	//					}
	//				} catch(Exception e) {
	//				}
	//				
	//				loader = loader.getParent();
	//			}
	//		}
	//		return null;
	//	}
	//	public void testMethod() {
	//		Runtime.traceEntry(AsmTest.class, "testMethod", null, null);
	//	}
	//}

	class ClassLoaderVisitor extends ClassVisitor {
		public boolean isChanged = false;
		private String className = null;

		ClassLoaderVisitor(ClassVisitor theClassVisitor) {
			super(Opcodes.ASM5, theClassVisitor);
			//System.out.println("kms@ ClassLoaderVisitor");
		}

		public void visit(int theVersion, int theAccess, String theClassName, String theSignature, String theSuperClassName, String[] theInterfaces) {
			className = theClassName;
		}

		public MethodVisitor visitMethod(
			int theAccess, String theName, String theDescription,
			String theSignature, String[] theExceptions) {
			MethodVisitor methodVisitor = cv.visitMethod(theAccess, theName, theDescription, theSignature, theExceptions);

			if(theName.equals("findClass111") && theDescription.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
				methodVisitor = new FindClassMethodCracker(className, api, methodVisitor, theAccess, theName, theDescription);
				isChanged = true;
			}
			else {
				String methodNameForPatternMatching = String.format("%1$s::%2$s", className.replace("/", "."), theName);
				
				if(pattern.matcher(methodNameForPatternMatching).find(0)) {
					System.out.println("kms@ mmm = " + className + "." + theName);
					methodVisitor = new PatternMatchedMethodModifier(className, api, methodVisitor, theAccess, theName, theDescription);
					isChanged = true;
				}
			}

			return methodVisitor;
		}
	}

	private byte[] crackClassLoader(byte theBytecode[]) {
		ClassReader reader = new ClassReader(theBytecode);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		ClassLoaderVisitor visitor = new ClassLoaderVisitor(writer);
		//System.out.println("kms@ --- new ClassLoaderVisitor");

		System.out.println("kms@ +++ accept");
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		System.out.println("kms@ --- accept ? " + visitor.isChanged);
		return visitor.isChanged ? writer.toByteArray() : theBytecode;
	}

	private boolean isInstrumentable(CtClass theClass) {
		return !theClass.isFrozen() && !theClass.isInterface();
	}

	private boolean isClassLoader(CtClass theClass) {
		try {
			while(theClass != null) {
				if(theClass.getName().equals(ClassLoader.class.getName()))
					return true;

				theClass = theClass.getSuperclass();
			}
		} catch(javassist.NotFoundException e) {
			System.err.format("Failed to qualify %1$s as a class loader: %2$s", theClass.getName(), e.toString());
			return false;
		}

		return false;
	}

	private boolean hasSlf4jLogger(CtClass theClass) {
		String slf4jSignature = Descriptor.of(Runtime.Slf4jLoggerClassName);
		CtField[] fields = theClass.getDeclaredFields();

		for(CtField field : fields) {
			if(field.getSignature().equals(slf4jSignature)) {
				return true;
			}
		}

		return false;
	}

	// Method allows to use classes from com.develorium.metracer for class loaders with strict isolation, e.g. JBoss module class loader
	private boolean tuneClassLoader(CtClass theClass) {
		boolean wasInstrumented = false;

		for(CtMethod method : theClass.getDeclaredMethods()) {
			try {
				if(method.getName().equals("findClass")) {
					TracingCodeInjector.injectDirectAccessToMetracerClasses(method);
					wasInstrumented = true;
					break;
				}
			} catch (Exception e) {
				System.err.format("Failed to tune class loader %1$s: %2$s\n", theClass.getName(), e.toString());
			}
		}

		return wasInstrumented;
	}

	private boolean makeSlf4jLoggerAutoRegisration(CtClass theClass) {
		boolean wasInstrumented = false;
		String descriptor = Descriptor.of(String.class.getName());
		try {
			CtConstructor ctor = theClass.getConstructor(descriptor);
			ctor.insertBefore(String.format("%1$s.registerLogger(this, $1);", Runtime.class.getName()));
			wasInstrumented = true;
		} catch(NotFoundException e) {
			System.err.format("Failed to locate constructor with descriptor \"%1$s\" within class \"%2$s\" which is ought to be there: %3$s\n", 
				descriptor, theClass.getName(), e.toString());
		} catch(CannotCompileException e) {
			System.err.format("Failed to make logger auto registration within class \"%1$s\": %2$s\n", theClass.getName(), e.toString());
		}

		return wasInstrumented;
	}

	private boolean instrumentClass(CtClass theClass) {
		boolean wasInstrumented = false;

		for(CtMethod method : theClass.getDeclaredMethods()) {
			try {
				final String methodNameForPatternMatching = String.format("%1$s::%2$s", theClass.getName(), method.getName());
				
				if(!pattern.matcher(methodNameForPatternMatching).find(0)) 
					continue;

				if(instrumentMethod(theClass, method))
					wasInstrumented = true;
			} catch (Exception e) {
				System.err.format("Failed to add tracing to method %1$s: %2$s\n", method.getLongName(), e.toString());
				// class can be damaged in this case so don't try to proceed with half-instrumented class
				return false;
			}
		}

		return wasInstrumented;
	}

	private boolean instrumentMethod(CtClass theClass, CtMethod theMethod) throws java.lang.Exception {
		if(!TracingCodeInjector.isMethodInstrumentable(theMethod)) 
			return false;

		TracingCodeInjector.injectTracingCode(theClass, theMethod);
		return true;
	}
}
