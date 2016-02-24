# metracer
![metracer logo](http://develorium.com/wp-content/uploads/2015/11/metracer.png)
*metracer* is a tool to spy for invocation of an arbitrary methods within Java program. In some sense it's similar to a famous [strace] program in Linux. Just pass a regexp denoting interesting methods and *metracer* would make these methods to report entry / exit, values of input arguments, return values and exceptions. 

*metracer* copes well with Java 1.7 and above (stack map frames) and supports JaveEE applications with theirs class loaders' isolation.

Use *metracer* when:
 - you want to quickly get acquainted with how a particular part of a Java program works - *metracer* will provide you with a call tree of a methods;
 - you want to spy for SQL statements issued by Hibernate in your JavaEE application - *metracer* will supply you with arguments of called methods;
 - you want to troubleshoot errors in Java program but debugger is not available - *metracer* will supply you with return values or exceptions thrown in a program.

# Usage
*metracer* is a Java agent program. Example of how it can be enabled:

`#java -javaagent:metracer-1.2.0.jar=com.myprogram -cp myprogram.jar com.myprogram.MyClass`

This would enable methods tracing for all classes from package com.myprogram.

# Technology
*metracer* uses [ASM] to instrument programs' code on the fly without a need to recompile anything.

# License
Apace License, Version 2.0.

[strace]: <http://linux.die.net/man/1/strace>
[ASM]: <http://asm.ow2.org/>
