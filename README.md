# metracer
![metracer logo](http://develorium.com/wp-content/uploads/2015/11/metracer.png)

**metracer** is a tool to spy for invocation of an arbitrary methods in Java program. In some sense it's similar to a famous [strace] program. Just pass a regexp denoting interesting classes/methods and **metracer** would make these methods to report entry / exit, values of input arguments, return values and exceptions. 

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

# Usage Examples
1) Spy for invocations of all methods of classes from package *com.myprogram* (and all subpackages as well) in a Java program with PID 345

`# sh metracer.sh 345 com.myprogram`

2) Spy for invocations of all methods of classes which name starts from *com.myprogram.MyClass* (e.g. this could be *com.myprogram.MyClass1* and *com.myprogram.MyClass2*) in a Java program with PID 345

`# sh metracer.sh 345 com.myprogram.MyClass`

3) Spy for invocations of all methods which name contains *doSomething* of classes which name starts from *com.myprogram.MyClass* (e.g. this could be *com.myprogram.MyClass1* and *com.myprogram.MyClass2*) in a Java program with PID 345

`# sh metracer.sh 345 com.myprogram.MyClass doSomething`

4) Spy for invocations of all methods which name either contains *doThisThing* OR *doAnotherThing* of classes which name starts from *com.myprogram.MyClass* (e.g. this could be *com.myprogram.MyClass1* and *com.myprogram.MyClass2*) in a Java program with PID 345

`# sh metracer.sh 345 com.myprogram.MyClass "(doThisThing|doAnotherThing)"`

5) Spy for invocations of all methods of classes which name either starts from *com.myprogram.ThisClass* or from *com.myprogram.ThatClass* in a Java program with PID 345

`# sh metracer.sh 345 "(com.myprogram.ThisClass|com.myprogram.ThatClass)"`


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
