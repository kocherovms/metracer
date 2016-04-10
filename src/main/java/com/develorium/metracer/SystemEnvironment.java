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

import java.io.*;

public class SystemEnvironment implements Environment {
	@Override
	public InputStream getStdin() {
		return System.in;
	}

	@Override
	public PrintStream getStdout() {
		return System.out;
	}

	@Override
	public PrintStream getStderr() {
		return System.err;
	}

	@Override
	public void exit(int theExitCode) {
		System.exit(theExitCode);
	}
}
