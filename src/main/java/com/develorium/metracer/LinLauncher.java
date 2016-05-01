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

public class LinLauncher extends AbstractLauncher {
	public static void main(String[] theArguments) {
		new LinLauncher().execute(theArguments);
	}

	@Override
	protected String getJavaExeName() {
		return "java";
	}

	@Override
	protected String getDefaultSelfJarName() {
		return "metracer.jar";
	}

	@Override
	protected String resolveUserNameOfTargetJvm(int thePid) {
		try {
			InputStream stream = new FileInputStream(String.format("/proc/%d/status", thePid));
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			return processStatusContent(reader);
		} catch(Throwable e) {
			throw new RuntimeException(String.format("Failed to resolve user ID for PID %d: %s", thePid, e.getMessage()), e);
		}
	}

	@Override
	protected void prepareExecutionEnvironment(Map<String, String> theEnvVariables) {
	}

	@Override
	protected void cleanupExecutionEnvironment() {
	}

	@Override 
	protected void prepareImpersonation(List<String> theArguments, Map<String, String> theEnvVariables, String theUserName) {
		assert(theArguments != null);
		assert(theEnvVariables != null);
		assert(theUserName != null);
		String selfPid = Helper.getSelfPid();
		String currentUserName = resolveUserNameOfTargetJvm(Integer.parseInt(selfPid));
		
		if(currentUserName.equals(theUserName))
			return; 
		
		int i = 0;
		theArguments.add(i++, "sudo");
		theArguments.add(i++, "-u");
		theArguments.add(i++, "#" + theUserName);
		
		for(Map.Entry<String, String> kv : theEnvVariables.entrySet())
			theArguments.add(i++, String.format("%s=%s", kv.getKey(), kv.getValue()));
	}

	static String processStatusContent(BufferedReader theReader) throws IOException {
		String line = null;

		while((line = theReader.readLine()) != null) {
			line = line.trim().toLowerCase();

			if(!line.startsWith("uid:"))
				continue;

			// IDs goes in following order (http://man7.org/linux/man-pages/man5/proc.5.html): 
			// Real, effective, saved set, and filesystem UIDs. 
			// We need a real UID
			Scanner scanner = new Scanner(line);
			scanner.next(); // skip Uid:
			return "" + scanner.nextInt();
		}

		return null;
	}
}
