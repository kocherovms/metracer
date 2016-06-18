# metracer [![Build Status](https://travis-ci.org/kocherovms/metracer.svg?branch=master)](https://travis-ci.org/kocherovms/metracer)
![metracer logo](http://develorium.com/wp-content/uploads/2016/06/metracer_logo.png)

**metracer** is a tool to trace invocation of arbitrary methods in Java programs. In some sense it's an analog of [strace] program from *nix systems.  
Use **metracer** when:
 - you want to quickly get acquainted with how a particular part of a Java program works - **metracer** will provide you with a call tree of a methods;
 - you want to spy for SQL statements issued by Hibernate in your JavaEE application - **metracer** will supply you with arguments of called methods;
 - you want to troubleshoot errors in Java program but debugger is not available - **metracer** will supply you with return values or exceptions thrown in a program.

# Getting Started

1) Download metracer JAR file from the latest release: https://github.com/kocherovms/metracer/releases/latest

2) List JVM available for attachment:
`java -jar metracer.jar -l`

3) Instrument interesting methods with metracer


# Technology

- [ASM] is used to instrument programs' code on the fly without a need to recompile anything;
- [Java Attach API] is used to inject tracing into target Java process (agent uploading);
- [JMX] is used to communicate and configure uploaded agent.

# Requirements

- Java 1.6 or higher;
- JDK is installed (tools.jar is needed).

# License

Apace License, Version 2.0.

# More Information

Detailed information on **metracer** is available here: http://develorium.com/metracer

[strace]: <http://linux.die.net/man/1/strace>
[StackMapFrames]: http://stackoverflow.com/questions/25109942/is-there-a-better-explanation-of-stack-map-frames
[ASM]: <http://asm.ow2.org/>
[Java Attach API]: https://docs.oracle.com/javase/7/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html
[JMX]: http://www.oracle.com/technetwork/articles/java/javamanagement-140525.html
