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
import javassist.*;

class ClassPools {
	private Map<Integer, ClassPool> classPools = new HashMap<Integer, ClassPool>();
		
	public ClassPool getClassPool(ClassLoader theLoader) {
		if(theLoader == null) 
			return ClassPool.getDefault();

		Integer loaderHashCode = theLoader.hashCode();

		if(classPools.containsKey(loaderHashCode))
			return classPools.get(loaderHashCode);

		ClassPool parentClassPool = getClassPool(theLoader.getParent());
		ClassPool newClassPool = new ClassPool(parentClassPool);
		newClassPool.appendSystemPath();
		LoaderClassPath loaderClassPath = new LoaderClassPath(theLoader);
		newClassPool.appendClassPath(loaderClassPath);
		classPools.put(loaderHashCode, newClassPool);
		return newClassPool;
	}
}