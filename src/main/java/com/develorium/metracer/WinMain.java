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

public class WinMain {
	public static void main(String[] theArguments) {
		try {
			String userName = resolveUserName(theArguments);

			if(userName != null)
				System.out.println(userName);
		} catch(Config.BadConfig e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		} catch(IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(2);
		} catch(Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(3);
		}
	}

	static String resolveUserName(String[] theArguments) throws Config.BadConfig, IOException {
		Config config = new Config(theArguments);
		BufferedReader reader = getTaskList();
		return resolveUserNameOfPid(reader, "" + config.pid);
	}
	
 	static BufferedReader getTaskList() throws IOException {
		String[] args = {
			"tasklist.exe",
			"/fo",
			"list", // not using a CSV, because tasklist produces incorrect CSV (e.g. when program caption contains " symbol)
			"/v"
		};
		Process p = java.lang.Runtime.getRuntime().exec(args);
		InputStream stdout = p.getInputStream();
		return new BufferedReader(new InputStreamReader(stdout));
	}
	
	static String resolveUserNameOfPid(BufferedReader theReader, String thePid) throws IOException {
		String line = null;
		int fieldIndex = 0;
		boolean pidFound = false;

		while((line = theReader.readLine()) != null) {
			line = line.trim();
			
			if(line.isEmpty()) {
				fieldIndex = 0;
				continue;
			}

			if(fieldIndex == 1 && !pidFound) {
				String currentPid = extractFieldValue(line);
				
				if(currentPid != null && currentPid.equals(thePid)) {
					pidFound = true;
				}
			}
			else if(fieldIndex == 6 && pidFound) {
				return extractFieldValue(line);
			}

			fieldIndex++;
		}

		return null;
	}

	static String extractFieldValue(String theField) {
		int index = theField.indexOf(':');
		
		if(index < 0 || (index == theField.length() - 1))
			return null;

		String rv = theField.substring(index + 1);
		return rv.trim();
	}
}
