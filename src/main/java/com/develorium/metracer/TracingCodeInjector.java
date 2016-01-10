/*
 * Copyright 2015 Michael Kocherov
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

import java.util.concurrent.atomic.AtomicInteger;
import javassist.*;
import javassist.bytecode.*;

class TracingCodeInjector {
	public static class Logger {
		public String getLogCallCode(String theText) {
			return backend.getLogCallCode(theText);
		}
		public boolean isInitialized() {
			return backend != null;
		}
		public void setBackend(LoggerBackend theBackend) {
			backend = theBackend;
		}
		private LoggerBackend backend = null;
	}
	private interface LoggerBackend {
		public String getLogCallCode(String theText);
	}
	private class DefaultLoggerBackend implements LoggerBackend {
		public String getLogCallCode(String theText) {
			return String.format("System.out.println(%1$s);", theText);
		}
	}
	private class Slf4jLoggerBackend extends DefaultLoggerBackend {
		Slf4jLoggerBackend(String theFieldName, boolean theIsSynthetic) {
			fieldName = theFieldName;
			isSynthetic = theIsSynthetic;
		}
		public String getLogCallCode(String theText) {
			//return isSynthetic
			//	? String.format("((org.slf4j.Logger)%1$s).info(%2$s);", fieldName, theText)
			//	: String.format("%1$s.info(%2$s);", fieldName, theText);
			if(!isSynthetic) 
				return String.format("%1$s.info(%2$s);\n", fieldName, theText);

			// Here we may have uninitializer logger (e.g. due to class loading problem)
			// Will fallback to default logger behavior
			//code.append(String.format(" ((org.slf4j.Logger)%1$s).info(%2$s);", fieldName, theText));
			StringBuilder code = new StringBuilder();
			code.append(String.format("if(%1$s != null) {\n", fieldName));
			code.append(String.format(" try {\n"));
			code.append(String.format("  java.lang.Class[] __infoMethodArgumentTypes = { java.lang.String.class };\n"));
			code.append(String.format("  java.lang.reflect.Method __infoMethod = %1$s.getClass().getMethod(\"info\", __infoMethodArgumentTypes);\n", fieldName));
			code.append(String.format("  java.lang.String[] __infoMethodArguments = { %1$s };\n", theText));
			code.append(String.format("  __infoMethod.invoke(%1$s, __infoMethodArguments);\n", fieldName));
			code.append(String.format(" } catch(Exception e) {\n"));
			code.append(String.format("  %1$s\n", super.getLogCallCode("\"kms@ zzz \" + e.toString()")));
			code.append(String.format("  %1$s\n", super.getLogCallCode(theText))); // Fallback to a default logger
			code.append(String.format(" }\n"));
			code.append(String.format("}\n"));
			code.append(String.format("else {\n"));
			code.append(String.format(" %1$s\n", super.getLogCallCode("\"kms@ yyy \" + " + theText))); // Fallback to a default logger
			code.append(String.format("}\n"));
			return code.toString();
		}
		private String fieldName;
		private boolean isSynthetic;
	}

	private String selfPackageName = this.getClass().getPackage().getName() + ".";;
	static final String MetracedSuffix = "_com_develorium_metraced";
	static final AtomicInteger MetracerNonce = new AtomicInteger();

	public void initializeLogger(CtClass theClass, Logger theLogger) {
		//theLogger.setBackend(new DefaultLoggerBackend());
		//return;
		final String Slf4jClassName = "org.slf4j.Logger";

		for(CtField field : theClass.getDeclaredFields()) {
			try {
				if(field.getType().getName().equals(Slf4jClassName)) {
					theLogger.setBackend(new Slf4jLoggerBackend(field.getName(), false));
					return;
				}
			} catch(NotFoundException e) {
			}
		}
		//Problems with DomainRegistrationPluginFeatures - seems it's being loaded in different class loaders
		ClassPool classPool = theClass.getClassPool();
		//CtClass slf4jClass = classPool.getOrNull(Slf4jClassName);
		CtClass slf4jClass = classPool.getOrNull("java.lang.Object");
		
		if(slf4jClass != null) {
			try {
				final String SynthenticLoggerInitializerMethodName = "__com_develorium_initialize_synthetic_logger";
				StringBuilder code = new StringBuilder();
				// dynamic class loading is used to avoid failures when slf4j is not available
				code.append(String.format("private static java.lang.Object %1$s() {\n", SynthenticLoggerInitializerMethodName));
				code.append(String.format(" try {\n"));
				code.append(String.format("  java.lang.Class loggerFactory = %1$s.class.getClassLoader().loadClass(\"org.slf4j.LoggerFactory\");\n", theClass.getName()));
				code.append(String.format("  java.lang.Class[] argumentTypes = { java.lang.String.class };\n"));
				code.append(String.format("  java.lang.reflect.Method getLogger = loggerFactory.getMethod(\"getLogger\", argumentTypes);\n"));
				code.append(String.format("  java.lang.String[] args = { \"%1$s\" };\n", theClass.getName()));
				code.append(String.format("  return getLogger.invoke(null, args);\n"));
				code.append(String.format(" } catch(Exception e) {\n"));
				code.append(String.format("  System.out.println(\"kms@ xxx \" + e.toString());"));
				code.append(String.format(" }\n"));
				code.append(String.format(" return null;\n"));
				code.append(String.format("}\n"));

				//System.out.println("kms@ zzz3\n" + code.toString());

				CtMethod syntheticLoggerInitializerMethod = CtNewMethod.make(code.toString(), theClass);



				theClass.addMethod(syntheticLoggerInitializerMethod);
				System.out.println("kms@ synth logger initializer method created in " + theClass.getName());


				final String SynthenticLoggerFieldName = "__com_develorium_synthetic_logger";
				CtField newSlf4LoggerField = new CtField(slf4jClass, SynthenticLoggerFieldName, theClass);
				System.out.println("kms@ synth logger field created in " + theClass.getName());
				newSlf4LoggerField.setModifiers(Modifier.STATIC);
				//theClass.addField(newSlf4LoggerField, CtField.Initializer.byExpr(String.format("{ return org.slf4j.LoggerFactory.getLogger(%1$s.class); }", theClass.getName())));
				//theClass.addField(newSlf4LoggerField, CtField.Initializer.byExpr(String.format("org.slf4j.LoggerFactory.getLogger(%1$s.class);", theClass.getName())));

				theClass.addField(newSlf4LoggerField, CtField.Initializer.byExpr(String.format("%1$s();", SynthenticLoggerInitializerMethodName)));
				System.out.println("kms@ synth logger field added to class " + theClass.getName());
				theLogger.setBackend(new Slf4jLoggerBackend(SynthenticLoggerFieldName, true));
				System.out.println("kms@ synth logger field successfully initialized in " + theClass.getName());
				return;
			} catch(CannotCompileException e) {
				System.out.println("kms@ failed to add synth logger " + e.getMessage());
			}
		}
		
		theLogger.setBackend(new DefaultLoggerBackend());
	}
	public void injectTracingCode(CtClass theClass, CtMethod theMethod, Logger theLogger) throws NotFoundException, CannotCompileException, Exception {
		if(!theLogger.isInitialized())
			throw new Exception("Logger is not initializer");

		String methodNameForPrinting = String.format("%s.%s", theClass.getName(), theMethod.getName());
		String originalMethodName = theMethod.getName();
		// Adding nonce is req-d to avoid unwanted downstream (Base->Inherited) virtual calls from _metraced methods
		String wrappedMethodName = String.format("%s%s%d", originalMethodName, MetracedSuffix, MetracerNonce.incrementAndGet());
		final CtMethod wrappedMethod = CtNewMethod.copy(theMethod, wrappedMethodName, theClass, null);
		theClass.addMethod(wrappedMethod);
		StringBuilder body = new StringBuilder();
		body.append("{");
		body.append(" java.lang.reflect.Method logMethod = null;");
		body.append(" java.lang.Class runtimeClass = null;");
		body.append(" java.lang.ClassLoader classLoader = " + theClass.getName() + ".class.getClassLoader();");
		body.append(" while(classLoader != null) {");
		body.append("  try {");
		body.append("   runtimeClass = java.lang.Class.forName(\"com.develorium.metracer.Runtime\", false, classLoader);");
		body.append("   if(runtimeClass != null) break;");
		body.append("  } catch(Exception e) { ");
		body.append("  } ");
		body.append("  classLoader = classLoader.getParent();");
		body.append(" }");
		body.append(" Class[] methodTypes = { String.class };");
		body.append(" logMethod = runtimeClass.getMethod(\"log\", methodTypes);");
		body.append(" java.lang.Thread thread = java.lang.Thread.currentThread();");
		body.append(" java.lang.StackTraceElement[] stackTraceElements = thread.getStackTrace();");
		body.append(" java.lang.StringBuilder indent = new java.lang.StringBuilder();");
		body.append(" int callDepth = 0;");
		body.append(" for(int i = 0; i < stackTraceElements.length; i++) {");
		body.append("  java.lang.StackTraceElement element = stackTraceElements[i];");
		body.append("  if(element.getMethodName().indexOf(\"" + MetracedSuffix + "\") >= 0) {");
		body.append("   callDepth++;");
		body.append("   if(callDepth < 32) {");
		body.append("    indent.append(\" \");");
		body.append("   }");
		body.append("  }");
		body.append(" }");
		body.append(getArgumentPrintingCode(theMethod, "argumentValuesRaw"));
		body.append(" java.lang.String argumentValues = argumentValuesRaw == null ? \"\" : argumentValuesRaw.toString();");
		//body.append(" java.lang.String[] logArg1 = { indent.toString() + \"+++[\" + callDepth + \"] " + methodNameForPrinting + "\" + argumentValues };");
		//body.append(" if(logMethod != null) logMethod.invoke(null, logArg1);");
		body.append(theLogger.getLogCallCode("indent.toString() + \"+++[\" + callDepth + \"] " + methodNameForPrinting + "\" + argumentValues"));
		body.append(" boolean isFinishedOk = false;");
		body.append(" try {");

		CtClass returnType = theMethod.getReturnType();
		String callMethodCode = null;
		String returnResultCode = null;

		if(returnType == CtClass.voidType) {
			callMethodCode = String.format("%1$s($$);", wrappedMethodName);
			returnResultCode = "";
		}
		else {
			callMethodCode = String.format("%1$s rv = %2$s($$);", returnType.getName(), wrappedMethodName);
			returnResultCode = "return rv;";
		}

		body.append(callMethodCode);
		body.append("  isFinishedOk = true;");
		body.append(returnResultCode);

		body.append(" } finally {");
		body.append("  java.lang.String trailingExceptionInfo = isFinishedOk ? \"\" : \" (by exception)\";");
		//body.append("  java.lang.String[] logArg2 = { indent.toString() + \"---[\" + callDepth + \"] " + methodNameForPrinting + "\" + trailingExceptionInfo };");
		//body.append("  if(logMethod != null) logMethod.invoke(null, logArg2);");
		body.append(theLogger.getLogCallCode("indent.toString() + \"---[\" + callDepth + \"] " + methodNameForPrinting + "\" + trailingExceptionInfo"));
		body.append(" }");
		body.append("}");
		//System.out.println(body);
		theMethod.setBody(body.toString());
	}
	public boolean isMethodInstrumentable(CtMethod theMethod) {
		// Do not try to patch ourselves
		if(theMethod.getLongName().startsWith(selfPackageName))
			return false;
		// Do not try to patch interface / abstract / native methods
		int modifiers = theMethod.getModifiers();
		return !Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isNative(modifiers);
	}
	private String getArgumentPrintingCode(CtMethod theMethod, String theResultHolderName) {
		StringBuilder rv = new StringBuilder();
		// Generated code tries to avoid unnecessary objects' creation. 
		// Also we don't use generics due to javaassist limitations
		rv.append("java.lang.StringBuilder resultHolderName = null;");
		rv.append("java.util.HashMap argumentNames = null;");
		rv.append("if($args.length > 0) {");
		rv.append(" resultHolderName = new java.lang.StringBuilder();");
		rv.append(" argumentNames = new java.util.HashMap();");

		MethodInfo methodInfo = theMethod.getMethodInfo();
		
		if(methodInfo != null && methodInfo.getCodeAttribute() != null) {
			LocalVariableAttribute table = (LocalVariableAttribute)methodInfo.getCodeAttribute().getAttribute(LocalVariableAttribute.tag);

			if(table != null) {
				// this is to skip a 'this' argument in case of a non-static method
				int offset = Modifier.isStatic(theMethod.getModifiers()) ? 0 : 1;
		
				for(int i = 0; i < table.tableLength(); ++i) {
					int argumentIndex = table.index(i) - offset;
		
					if(argumentIndex < 0) continue;
				
					String argumentName = table.variableName(i);
					rv.append(" argumentNames.put(new Integer(");
					rv.append(argumentIndex);
					rv.append("), \"");
					rv.append(argumentName);
					rv.append("\");");
				}
			}
		}

		rv.append("}");
		rv.append("for(int i = 0; i < $args.length; ++i) {");
		rv.append(" java.lang.Object rawN = argumentNames.get(new Integer(i));");
		rv.append(" java.lang.String n = rawN == null ? \"<unk>\" : (String)rawN;");
		rv.append(" java.lang.Object o = $args[i];");
		rv.append(" java.lang.String v = o == null ? \"null\" : o.toString();");
		rv.append(" if(v.length() > 128) v = v.substring(0, 128) + \"...\";");
		rv.append(" if(i > 0) resultHolderName.append(\", \");");
		rv.append(" resultHolderName.append(n);");
		rv.append(" resultHolderName.append(\" = \");");
		rv.append(" resultHolderName.append(v);");
		rv.append("}");
		return rv.toString().replaceAll("resultHolderName", theResultHolderName);
	}
}
