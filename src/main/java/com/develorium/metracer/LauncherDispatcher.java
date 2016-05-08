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
import java.io.*;

public abstract class LauncherDispatcher {
	public void main(String[] theArguments) {
		String osName = getOsName();
		AbstractLauncher launcher = getLauncher(osName);
		launcher.execute(theArguments);
	}

	private String getOsName() {
		return System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
	}

	private AbstractLauncher getLauncher(String theOsName) {
		if(theOsName.contains("windows"))
			return new WindowsLauncher();
		else if(theOsName.contains("linux"))
			return new LinuxLauncher();
		else 
			return null;
	}
}
