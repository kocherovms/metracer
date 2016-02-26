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

package com.develorium.metracer.dynamic;

import java.lang.instrument.Instrumentation;
import javax.management.*;
import java.lang.management.*;
import java.util.concurrent.atomic.*;

public class Agent extends NotificationBroadcasterSupport implements AgentMXBean, com.develorium.metracer.Runtime.LoggerInterface {
	public static final String MxBeanName = "com.develorium.metracer.dynamic:type=Agent";
	public static final String NotificationType = "com.develorium.metracer.traceevent";
	private com.develorium.metracer.Runtime runtime = null;
	private AtomicInteger messageSerial = new AtomicInteger();
	private Thread testJob = null;

	public static void agentmain(String theArguments, Instrumentation theInstrumentation) {
		System.out.println("Hello, world!");
		new Agent().bootstrap(theArguments, theInstrumentation);
	}

	@Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo info = new MBeanNotificationInfo(
			new String[] { NotificationType }, 
			Notification.class.getName(), 
			"Notification about metracer event (entry / exit of methods)");
        return new MBeanNotificationInfo[] { info };
	}

	@Override
	public void printMessage(Class<?> theClass, String theMethodName, String theMessage) {
		System.out.println("kms@ " + theClass + " " + theMethodName + " " + theMessage);
		Notification notification = new Notification(NotificationType, this, messageSerial.incrementAndGet(), theMessage);
		sendNotification(notification);
	}

	@Override
	public void test() {
		System.out.println("kms@ TEST");
	}

	private void bootstrap(String theArguments, Instrumentation theInstrumentation) {
		try {
			createRuntime();
			registerMxBean();
			startTestJob();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void createRuntime() {
		runtime = new com.develorium.metracer.Runtime(this);
	}

	private void registerMxBean() throws Exception {
		MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName beanName = new ObjectName(MxBeanName);
		beanServer.registerMBean(this, beanName);
	}

	private void startTestJob() {
		testJob = new Thread(new Runnable() {
			public void run() {
				while(true) {
					printMessage(getClass(), "run", "Hello, world");
					try {
						Thread.sleep(3000);
					} catch(InterruptedException e) {
					}
				}
			}
		});

		testJob.start();
	}
}
