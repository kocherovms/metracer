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

public class LauncherDispatcher {
	public static void main(String[] theArguments) {
		String osName = getOsName();
		AbstractLauncher launcher = getLauncher(osName);

		if(launcher == null) {
			reportLauncherMiss(osName, theArguments);
			System.exit(1);
		}

		launcher.execute(theArguments);
	}

	private static String getOsName() {
		return System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
	}

	private static AbstractLauncher getLauncher(String theOsName) {
		if(theOsName.contains("windows"))
			return new WindowsLauncher();
		else if(theOsName.contains("linux"))
			return new LinuxLauncher();
		else 
			return null;
	}

	private static void reportLauncherMiss(String theOsName, String[] theArguments) {
		StringBuilder argumentsInlined = new StringBuilder();

		for(String arg : theArguments)
			argumentsInlined.append(arg + " ");

		String message = Helper.loadTextResource("nolauncher.txt");
		message = message.replace("${osname}", theOsName);
		String metracerJarPath = Helper.getSelfJarFileSafe() != null 
			? Helper.getSelfJarFileSafe().getAbsolutePath() 
			: "metracer.jar";
		message = message.replace("${metracerjarpath}", metracerJarPath);
		message = message.replace("${arguments}", argumentsInlined.toString());
		System.err.format(message);
	}
}
