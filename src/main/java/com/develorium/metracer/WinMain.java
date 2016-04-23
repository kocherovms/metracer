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
import org.apache.commons.csv.*;

public class WinMain {
	public static void main(String[] theArguments) {
		try {
			Config config = new Config(theArguments);
			CSVParser parser = getTaskList();
			String userName = resolveUserNameOfPid(parser, "" + config.pid);

			if(userName != null)
				System.out.println(userName);
		} catch(Config.BadConfig e) {
			System.exit(1);
		} catch(IOException e) {
			System.exit(2);
		} catch(Throwable e) {
			System.exit(3);
		}
	}

	private static CSVParser getTaskList() throws IOException {
		String[] args = {
			"tasklist.exe",
			"/nh",
			"/fo",
			"/csv",
			"/v"
		};
		Process p = java.lang.Runtime.getRuntime().exec(args);
		InputStream stdout = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		return CSVFormat.DEFAULT.parse(reader);
	}

	private static String resolveUserNameOfPid(CSVParser theParser, String thePid) {
		for(CSVRecord record : theParser) {
			String recordPid = record.get(1);

			if(recordPid.equals(thePid)) {
				return record.get(6);
			}
		}

		return null;
	}
}
