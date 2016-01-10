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

import java.util.concurrent.atomic.AtomicInteger;
import javassist.*;
import javassist.bytecode.*;

class TracingCodeInjector {
	private String selfPackageName = this.getClass().getPackage().getName() + ".";;
	static final String MetracedSuffix = "_com_develorium_metraced";
	static final AtomicInteger MetracerNonce = new AtomicInteger();

	public void injectTracingCode(CtClass theClass, CtMethod theMethod) throws NotFoundException, CannotCompileException {
		String methodNameForPrinting = String.format("%s.%s", theClass.getName(), theMethod.getName());
		String originalMethodName = theMethod.getName();
		// Adding nonce is req-d to avoid unwanted downstream (Base->Inherited) virtual calls from _metraced methods
		String wrappedMethodName = String.format("%s%s%d", originalMethodName, MetracedSuffix, MetracerNonce.incrementAndGet());
		final CtMethod wrappedMethod = CtNewMethod.copy(theMethod, wrappedMethodName, theClass, null);
		theClass.addMethod(wrappedMethod);

		StringBuilder body = new StringBuilder();
		body.append("{");
		body.append("java.lang.Thread thread = java.lang.Thread.currentThread();");
		body.append("java.lang.StackTraceElement[] stackTraceElements = thread.getStackTrace();");
		body.append("java.lang.StringBuilder indent = new java.lang.StringBuilder();");
		body.append("int callDepth = 0;");
		body.append("for(int i = 0; i < stackTraceElements.length; i++) {");
		body.append(" java.lang.StackTraceElement element = stackTraceElements[i];");
		body.append(String.format(" if(element.getMethodName().indexOf(\"%1$s\") >= 0) {", MetracedSuffix));
		body.append("  callDepth++;");
		body.append("  if(callDepth < 32) {");
		body.append("   indent.append(\" \");");
		body.append("  }");
		body.append(" }");
		body.append("}");
		body.append(getArgumentPrintingCode(theMethod, "argumentValuesRaw"));
		body.append("java.lang.String argumentValues = argumentValuesRaw == null ? \"\" : argumentValuesRaw.toString();");
		body.append(String.format("System.out.println(indent.toString() + \"+++[\" + callDepth + \"] %1$s(\" + argumentValues + \")\");", methodNameForPrinting));
		body.append("boolean isFinishedOk = false;");
		body.append("try {");

		CtClass returnType = theMethod.getReturnType();
		String callMethodCode = "";
		String returnResultCode = "";

		if(returnType == CtClass.voidType) {
			callMethodCode = String.format("%1$s($$);", wrappedMethodName);
		}
		else {
			callMethodCode = String.format("%1$s rv = %2$s($$);", returnType.getName(), wrappedMethodName);
			returnResultCode = "return rv;";
		}

		body.append(callMethodCode);
		body.append("isFinishedOk = true;");
		body.append(returnResultCode);

		body.append("} finally {");
		body.append("java.lang.String trailingExceptionInfo = isFinishedOk ? \"\" : \" (by exception)\";");
		body.append(String.format("System.out.println(indent.toString() + \"---[\" + callDepth + \"] %1$s\" + trailingExceptionInfo);", methodNameForPrinting));
		body.append("}");
		body.append("}");
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
