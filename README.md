# metracer [![Build Status](https://travis-ci.org/kocherovms/metracer.svg?branch=master)](https://travis-ci.org/kocherovms/metracer)
![metracer logo](http://develorium.com/wp-content/uploads/2015/11/metracer.png)

* [General Information](#general-information)
* [Getting Started](#getting-started)
* [Usage Examples](#usage-examples)
* [Technology](#technology)
* [Requirements](#requirements)
* [More Information](#more-information)
* [License](#license)

# General Information

**metracer** is a tool to trace invocation of arbitrary methods in Java programs. It's an analog of [strace] program from *nix systems. 

# Getting Started

# Usage Examples

1) Spy for invocations of all methods of classes from package *com.myprogram* (and all subpackages as well) in a Java program with PID 345

``` console
$ sh metracer.sh 345 com.myprogram
```

2) Spy for invocations of all methods of classes which name starts from *com.myprogram.MyClass* (e.g. this could be *com.myprogram.MyClass1* and *com.myprogram.MyClass2*) in a Java program with PID 345

``` console
$ sh metracer.sh 345 com.myprogram.MyClass
```

3) Spy for invocations of all methods which name contains *doSomething* of classes which name starts from *com.myprogram.MyClass* (e.g. this could be *com.myprogram.MyClass1* and *com.myprogram.MyClass2*) in a Java program with PID 345

``` console
$ sh metracer.sh 345 com.myprogram.MyClass doSomething
```

4) Spy for invocations of all methods which name either contains *doThisThing* OR *doAnotherThing* of classes which name starts from *com.myprogram.MyClass* (e.g. this could be *com.myprogram.MyClass1* and *com.myprogram.MyClass2*) in a Java program with PID 345

``` console
$ sh metracer.sh 345 com.myprogram.MyClass "(doThisThing|doAnotherThing)"
```

5) Spy for invocations of all methods of classes which name either starts from *com.myprogram.ThisClass* or from *com.myprogram.ThatClass* in a Java program with PID 345

``` console
$ sh metracer.sh 345 "(com.myprogram.ThisClass|com.myprogram.ThatClass)"
```

6) Remove all previous instrumentation in a Java program with PID 345

``` console
$ sh metracer.sh 345 -r
```

7) Spy for invocations of method doSomething in com.myprogram.MyClass in a Java program with PID 345 and additionally save all stack traces into file /tmp/st.txt

``` console
$ sh metracer.sh 345 -S /tmp/st.txt com.myprogram.MyClass doSomething
```

8) Spy for invocation of methods listed in file /tmp/st.txt in a Java program with PID 345

``` console
$ sh metracer.sh 345 -f /tmp/st.txt
```

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

http://develorium.com/metracer

Just pass a regexp denoting interesting classes/methods and **metracer** would make these methods to report entry / exit, values of input arguments, return values and exceptions. 

Use **metracer** when:
 - you want to quickly get acquainted with how a particular part of a Java program works - **metracer** will provide you with a call tree of a methods;
 - you want to spy for SQL statements issued by Hibernate in your JavaEE application - **metracer** will supply you with arguments of called methods;
 - you want to troubleshoot errors in Java program but debugger is not available - **metracer** will supply you with return values or exceptions thrown in a program.

**metracer** is:
- easy to use: simple and straightforward CLI;
- lightweight: distro size is less that 300K;
- super fast: does instrumentation work in seconds;
- zero-deployment: no 3-rd party libraries are required - only single .sh script on Linux is needed (single .jar for all other OSes);
- Java [StackMapFrames]-friendly: **metracer** copes well with stack map frames introduced in Java 1.7 and above;
- class loaders isolation-friendly: **metracer** copes well with modern JaveEE app servers which impose strict isolation rules.

[strace]: <http://linux.die.net/man/1/strace>
[StackMapFrames]: http://stackoverflow.com/questions/25109942/is-there-a-better-explanation-of-stack-map-frames
[ASM]: <http://asm.ow2.org/>
[Java Attach API]: https://docs.oracle.com/javase/7/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html
[JMX]: http://www.oracle.com/technetwork/articles/java/javamanagement-140525.html
