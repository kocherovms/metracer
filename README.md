# Metracer
Metracer is a tool to spy for invocation of an arbitrary methods within Java program. In some sense it's similar to a famous [strace] program in Linux. Just pass a regexp denoting interesing methods and metracer would make these methods to report entry / exit, values of input arguments, return values and exceptions. 

Metracer copes well with Java 1.7 and above (stack map frames) and supports JaveEE applications with theirs class loaders' isolation.

Use metracer when:
 - you want to quickly get acquainted with how a particular part of a Java program works - metracer will provide you with a call tree of a methods;
 - you want to spy for SQL statements issued by Hibernate in your JavaEE application - metracer will supply you with arguments of called methods;
 - you want to troubleshoot errors in Java program but debugger is not available - metracer will supply you with return values or exceptions throw in a program.

# Usage
Metracer is a Java agent program. Example of how it can be enabled:

`#java -javaagent:metracer-1.2.0.jar=com.myprogram -cp myprogram.jar com.myprogram.MyClass`

This would enable methods tracing for all classes from package com.myprogram.

# Technology
Metracer uses [ASM] to instrument programs' code on the fly without then need to recomple anything.

# License
Apace License, Version 2.0.

[strace]: <http://linux.die.net/man/1/strace>
[ASM]: <http://asm.ow2.org/>
