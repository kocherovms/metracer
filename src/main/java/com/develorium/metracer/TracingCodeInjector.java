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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javassist.*;
import javassist.bytecode.*;

class TracingCodeInjector {
	static final String selfPackageName = TracingCodeInjector.class.getPackage().getName() + ".";
	static final String MetracedSuffix = "_com_develorium_metracer";
	static final AtomicInteger MetracerNonce = new AtomicInteger();

	public static boolean isMethodInstrumentable(CtMethod theMethod) {
		// Do not try to patch ourselves
		if(theMethod.getLongName().startsWith(selfPackageName))
			return false;
		// Do not try to patch interface / abstract / native methods
		int modifiers = theMethod.getModifiers();
		return !Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isNative(modifiers);
	}

	public static void injectDirectAccessToMetracerClasses(CtMethod theMethod) throws CannotCompileException {
		String clFieldName = "cmdr_e6b8085c9db3473eae21204661c80d4e";
		StringBuilder runtimeResolutionCode = new StringBuilder();
		runtimeResolutionCode.append(String.format("if($1.startsWith(\"%1$s\")) {", selfPackageName));
		runtimeResolutionCode.append(String.format(" java.lang.ClassLoader %1$s = $0;", clFieldName));
		runtimeResolutionCode.append(String.format(" while(%1$s != null) {", clFieldName));
		runtimeResolutionCode.append(String.format("  java.lang.reflect.Field f = java.lang.ClassLoader.class.getDeclaredField(\"classes\");"));
		runtimeResolutionCode.append(String.format("  f.setAccessible(true);"));
		runtimeResolutionCode.append(String.format("  java.util.Vector classes = (java.util.Vector)f.get(%1$s);", clFieldName));
		runtimeResolutionCode.append(String.format("  for(int i = 0; i < classes.size(); i++) {"));
		runtimeResolutionCode.append(String.format("   java.lang.Class c = (java.lang.Class)classes.get(i);", Runtime.class.getName()));
		runtimeResolutionCode.append(String.format("   if(c != null && c.getName().equals(\"%1$s\")) {", Runtime.class.getName()));
		runtimeResolutionCode.append(String.format("    return c;"));
		runtimeResolutionCode.append(String.format("    break;"));
		runtimeResolutionCode.append(String.format("   }"));
		runtimeResolutionCode.append(String.format("  }"));
		runtimeResolutionCode.append(String.format("  %1$s = %1$s.getParent();", clFieldName));
		runtimeResolutionCode.append(String.format(" }"));
		runtimeResolutionCode.append(String.format("}"));
		theMethod.insertBefore(runtimeResolutionCode.toString());
	}

	public static void injectTracingCode(CtClass theClass, CtMethod theMethod) throws NotFoundException, CannotCompileException {
		String originalMethodName = theMethod.getName();
		// Adding nonce is req-d to avoid unwanted downstream (Base->Inherited) virtual calls from _metraced methods
		String wrappedMethodName = String.format("%s%s%d", originalMethodName, MetracedSuffix, MetracerNonce.incrementAndGet());
		final CtMethod wrappedMethod = CtNewMethod.copy(theMethod, wrappedMethodName, theClass, null);
		theClass.addMethod(wrappedMethod);
		
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

		StringBuilder body = new StringBuilder();
		body.append(String.format("{"));
		body.append(String.format(" java.lang.String[] argumentNames = %1$s;", getArgumentNamesForArrayInitialization(theMethod)));
		body.append(String.format(" %1$s.traceEntry(%2$s.class, \"%3$s\", argumentNames, $args);", Runtime.class.getName(), theClass.getName(), theMethod.getName()));
		body.append(String.format(" boolean isFinishedOk = false;"));
		body.append(String.format(" try {"));
		body.append(String.format("  %1$s", callMethodCode));
		body.append(String.format("  isFinishedOk = true;"));
		body.append(String.format("  %1$s", returnResultCode));
		body.append(String.format(" } finally {"));
		body.append(String.format("  %1$s.traceExit(%2$s.class, \"%3$s\", isFinishedOk);", Runtime.class.getName(), theClass.getName(), theMethod.getName()));
		body.append(String.format(" }"));
		body.append(String.format("}"));
		theMethod.setBody(body.toString());
	}

 	private static String getArgumentNamesForArrayInitialization(CtMethod theMethod) {
		MethodInfo methodInfo = theMethod.getMethodInfo();
		
		if(methodInfo != null && methodInfo.getCodeAttribute() != null) {
			LocalVariableAttribute table = (LocalVariableAttribute)methodInfo.getCodeAttribute().getAttribute(LocalVariableAttribute.tag);

			if(table != null) {
				TreeMap<Integer, String> localVariables = new TreeMap<Integer, String>();
				int maxIndex = -1;

				for(int i = 0; i < table.tableLength(); ++i) {
					int variableIndex = table.index(i);
					String variableName = table.variableName(i);
					localVariables.put(variableIndex, variableName);
					maxIndex = Math.max(variableIndex, maxIndex);
				}

				if(maxIndex >= 0) {
					StringBuilder rv = new StringBuilder();
					int offset = Modifier.isStatic(theMethod.getModifiers()) ? 0 : 1; // if method is static then there is no 'this' within variable table
					
					for(int i = offset; i <= maxIndex; ++i) {
						if(rv.length() > 0)
							rv.append(", ");

						String variableName = localVariables.get(i);
						rv.append(variableName != null ? String.format("\"%1$s\"", variableName) : "null");
					}

					if(rv.length() > 0) 
						return String.format("{ %1$s }", rv.toString());
				}
			}
		}

		return "null";
	}
}
