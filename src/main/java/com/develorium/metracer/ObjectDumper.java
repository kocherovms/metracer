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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectDumper {
	public static String dumpObject(Object theObject) {
		List<Field> fields = getAllDeclaredFields(theObject);
		
		for(Field f : fields) {
			
		}

		return null;
	}

	static List<Field> getAllDeclaredFields(Object theObject) {
		List<Field> rv = new ArrayList<Field>();
		Class<?> c = theObject.getClass();

		while(c != null) {
			Field[] fields = c.getDeclaredFields();
			List<Field> z = Arrays.asList(fields);
			rv.addAll(z);
			c = c.getSuperclass();
		}

		return rv;
	}

	static Set<String> PrintableTypeNames = new HashSet<String> { {
			"java.lang.String",
			"java.lang.Byte",
			"java.lang.Short",
			"java.lang.Integer",
			"java.lang.Long",
			"java.lang.Character",
			"java.lang.Float",
			"java.lang.Double",
			"java.lang.Boolean"
		}
	};

	static String dumpSingleField(Object theObject, Field theField) {
		Object fieldValue = theField.get(theObject);

		if(fieldValue == null) 
			return String.format("%s=null", theField.getName());

		Class<?> t = theField.getType();
		String typeName = t.getName();

		if(t.isPrimitive() || PrintableTypeNames.contains(typeName)) 
			return String.format("%s=%s", theField.getName(), fieldValue.toString());

		return String.format("%s=?", theField.getName());
	}
}