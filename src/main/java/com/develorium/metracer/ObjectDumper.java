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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectDumper {
	public static int MaxIterableElementsForPrinting = 10;
	public static int MaxDumpLength = 4096;
	private StringBuilder rv = new StringBuilder();

	private class TooLongValueException extends RuntimeException {
		public TooLongValueException() {
			super("too long value");
		}
	}

	public String get() {
		return rv.toString();
	}

	public String dumpObject(Object theObject) {
		try {
			dumpObject_impl(theObject);
			return rv.toString();
		} catch(TooLongValueException e) {
			return rv.toString() + "... (too long value)";
		} catch(Throwable e) {
			return rv.toString() + "... incomplete due to exception: " + e.getMessage();
		}
	}

	private void dumpObject_impl(Object theObject) {
		if(rv.length() >= MaxDumpLength)
			return; 

		if(theObject == null) {
			append("null");
			return;
		}

		Class<?> c = theObject.getClass();
		String typeName = c.getName();

		if(c.isArray()) {
			dumpIterable(getArrayArgumentAdapter(theObject));
			return;
		}
		else if(theObject instanceof java.util.Collection<?>) {
			dumpIterable(getCollectionArgumentAdapter((Collection<?>)theObject));
			return;
		}
		else if(theObject instanceof java.util.Map<?, ?>) {
			dumpIterable(getMapArgumentAdapter((Map<?, ?>)theObject));
			return;
		}

		if(isImmediatePrintable(c)) {
			append(theObject.toString());
			return;
		}

		List<Field> fields = getAllDeclaredFields(theObject);
		append(String.format("(%s){", typeName));
		
		for(Field f : fields) {
			append(f.getName() + "=");
			dumpObjectField(theObject, f);
			append(",");
		}

		if(rv.toString().endsWith(","))
			rv.deleteCharAt(rv.length() - 1);
		
		append("}");
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

	ObjectDumper dumpObjectField(Object theObject, Field theField) {
		assert(theObject != null);
		assert(theField != null);
		Object fieldValue = null;
		boolean isAccessible = theField.isAccessible();

		try {
			try {
				if(!isAccessible)
					theField.setAccessible(true);

				fieldValue = theField.get(theObject);
			} catch(SecurityException e) {
				append("N/A (security)");
				return this;
			} catch(IllegalAccessException e) {
				append("N/A (inaccessible)");
				return this;
			}
		} finally {
			if(!isAccessible)
				theField.setAccessible(isAccessible);
		}

		if(fieldValue == null) {
			append("null");
			return this;
		}

		dumpObject(fieldValue);
		return this;
	}

	abstract class IterableArgumentAdapter {
		public abstract boolean hasNext();
		public abstract void dumpNext();
	}

	abstract class ArrayArgumentAdapter extends IterableArgumentAdapter {
		ArrayArgumentAdapter(int theSize) {
			size = theSize;
		}
		public int i = 0;
		public int size = 0;
		public boolean hasNext() {  return i < size; }
		public void dumpNext() { dumpItem(i++); }
		public abstract void dumpItem(int theIndex);
	}

	IterableArgumentAdapter getArrayArgumentAdapter(final Object theArray) {
		if(theArray instanceof boolean[]) {
			return new ArrayArgumentAdapter(((boolean[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((boolean[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof byte[]) {
			return new ArrayArgumentAdapter(((byte[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((byte[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof char[]) {
			return new ArrayArgumentAdapter(((char[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((char[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof short[]) {
			return new ArrayArgumentAdapter(((short[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((short[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof int[]) {
			return new ArrayArgumentAdapter(((int[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((int[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof long[]) {
			return new ArrayArgumentAdapter(((long[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((long[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof float[]) {
			return new ArrayArgumentAdapter(((float[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((float[])theArray)[theIndex]);
				}
			};
		}
		else if(theArray instanceof double[]) {
			return new ArrayArgumentAdapter(((double[])theArray).length) {
				public void dumpItem(int theIndex) { 
					dumpObject_impl(((double[])theArray)[theIndex]); 
				}
			};
		}
		else if(theArray instanceof Object[]) {
			return new ArrayArgumentAdapter(((Object[])theArray).length) {
				public void dumpItem(int theIndex) { 
					Object o = ((Object[])theArray)[theIndex];
					dumpObject_impl(o);
				}
			};
		}

		return null;
	}

	IterableArgumentAdapter getCollectionArgumentAdapter(final Collection<?> theCollection) {
		return new IterableArgumentAdapter() {
			Iterator<?> it = theCollection.iterator();
			public boolean hasNext() { 
				return it.hasNext(); 
			}
			public void dumpNext() { 
				Object x = it.next();
				dumpObject_impl(x);
			}
		};
	}

	IterableArgumentAdapter getMapArgumentAdapter(final Map<?, ?> theMap) {
		return new IterableArgumentAdapter() {
			Iterator<? extends Map.Entry<?, ?>> it = theMap.entrySet().iterator();
			public boolean hasNext() { 
				return it.hasNext(); 
			}
			public void dumpNext() { 
				Map.Entry<?, ?> entry = it.next();
				Object k = entry.getKey();
				Object v = entry.getValue();
				rv.append((k != null ? k.toString() : "null") + "=>");
				dumpObject_impl(v);
			}
		};
	}

	void dumpIterable(IterableArgumentAdapter theAdapter) {
		if(theAdapter == null) {
			append("null");
			return;
		}

		append("[");

		for(int i = 0; i < MaxIterableElementsForPrinting && theAdapter.hasNext(); ++i) {
			if(i > 0) {
				append(",");
			}
		
			theAdapter.dumpNext();
		}
		
		if(theAdapter.hasNext()) {
			append(",...");
		}

		append("]");
	}

	private void append(String thePiece) {
		if(rv.length() >= MaxDumpLength) 
			throw new TooLongValueException();

		int delta = MaxDumpLength - rv.length();
		assert(delta > 0);
		
		if(thePiece.length() <= delta) {
			rv.append(thePiece);
			return;
		}

		rv.append(thePiece.substring(0, delta));
		throw new TooLongValueException();
	}

	static String[] ImmediatePrintableTypePrefixes = { "java.", "sun.", "com.sun." };

	private boolean isImmediatePrintable(Class<?> theClass) {
		if(theClass.isPrimitive() || theClass.isEnum())
			return true;

		String typeName = theClass.getName();

		for(String prefix : ImmediatePrintableTypePrefixes) {
			if(typeName.startsWith(prefix))
				return true;
		}

		return false;
	}
}