<table style="width:100%">
  <tr>
    <td><img src="http://develorium.com/wp-content/uploads/2016/06/metracer_logo.png"/></td>
    <td><h1>metracer</h1><img src="https://travis-ci.org/kocherovms/metracer.svg?branch=master"></td>
  </tr>
</table>

**metracer** is a tool to trace invocation of an arbitrary methods in Java programs. In some sense it's an analog of [strace] program. **metracer** is useful for:
- you want to quickly get acquainted with how a particular part of a Java program works - **metracer** will provide you with a call tree of a methods;
- you want to troubleshoot errors in Java program but debugger is not available - **metracer** will supply you with return values or exceptions thrown in a program;
- you want to spy for SQL statements issued by Hibernate in your JavaEE application - **metracer** will supply you with arguments of called methods.

# Getting Started

1) **Download** **metracer** JAR file from the latest release: https://github.com/kocherovms/metracer/releases/latest  
2) **List** Java programs available for tracing:
``` console
$ java -jar metracer.jar -l
PID	   NAME
6688   com.develorium.metracertest.Main
3726   org.pwsafe.passwordsafeswt.PasswordSafeJFace
```
3) **Start tracing** methods in a desired Java program using PID from the listing table, e.g.:
- to trace all methods from class `com.develorium.metracertest.Main` in Java program with PID 6688:
``` console
$ java -jar metracer.jar 6688 com.develorium.metracertest.Main
2016.06.18 11:31:33.749 [metracer.00000009] +++ [0] com.develorium.metracertest.Main.testBundle()
2016.06.18 11:31:33.749 [metracer.00000009]  +++ [1] com.develorium.metracertest.Main.testA()
2016.06.18 11:31:33.749 [metracer.00000009]  --- [1] com.develorium.metracertest.Main.testA => void
...
```
- to trace only method `doSomething` from class `com.develorium.metracertest.Main` in Java program with PID 6688:
``` console
$ java -jar metracer.jar 6688 com.develorium.metracertest.Main doSomething
2016.06.18 11:31:14.721 [metracer.00000009] +++ [0] com.develorium.metracertest.Main.doSomething()
2016.06.18 11:31:14.726 [metracer.00000009] --- [0] com.develorium.metracertest.Main.doSomething => void
...
```

- to trace all methods from classes `com.develorium.metracertest.Foo` and `com.develorium.metracertest.Bar` in Java program with PID 6688:
``` console
$ java -jar metracer.jar 6688 'com.develorium.metracertest.Foo|Bar'
2016.06.19 12:00:17.312 [metracer.00000009] +++ [0] com.develorium.metracertest.Foo.perform()
2016.06.19 12:00:17.314 [metracer.00000009] --- [0] com.develorium.metracertest.Foo.perform => void
2016.06.19 12:00:17.315 [metracer.00000009] +++ [0] com.develorium.metracertest.Bar.perform()
2016.06.19 12:00:17.315 [metracer.00000009] --- [0] com.develorium.metracertest.Bar.perform => void
...
```

4) When you are done with tracing press **q** - this will stop methods tracing in a Java program and quit  

---

There are other functions available in metracer (e.g. vertical instrumentation). Brief description of **metracer** functionality is available in a builtin help:
``` console
$ java -jar metracer.jar -h
```

For more detailed information on how to use **metracer** please visit http://develorium.com/metracer. 


# Technology

- [ASM] is used to instrument programs' code on the fly without a need to recompile anything;
- [Java Attach API] is used to inject tracing into target Java process (agent uploading);
- [JMX] is used to communicate and configure uploaded agent.

# Requirements

- Java 1.6 or higher;
- JDK is installed (tools.jar is needed).

# License

Apace License, Version 2.0.

[strace]: <http://linux.die.net/man/1/strace>
[StackMapFrames]: http://stackoverflow.com/questions/25109942/is-there-a-better-explanation-of-stack-map-frames
[ASM]: <http://asm.ow2.org/>
[Java Attach API]: https://docs.oracle.com/javase/7/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html
[JMX]: http://www.oracle.com/technetwork/articles/java/javamanagement-140525.html
