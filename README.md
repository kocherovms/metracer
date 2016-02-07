# Metracer
Metracer is a tool to ease troubleshooting of Java programs. It allows you to spy for invocation of arbitrary methods within Java program. In some sense it's similar to a famous [strace] program in Linux.

Use metracer when:
 - you want to quickly get acquainted with how a particular part of a Java program works - metracer will provide you with a call tree of a methods;
 - you want to spy for SQL statements issued by Hibernate in your JavaEE application - metracer will supply you with arguments of called methods;
 - you want to troubleshoot errors in Java program but debugger is not available - metracer will supply you with exceptions throw in a program.

# Technology
Metracer uses [Javassist] to instrument programs' code on the fly without then need to recomple anything.

# License
Apace License, Version 2.0.

[strace]: <http://linux.die.net/man/1/strace>
[Javassist]: <http://jboss-javassist.github.io/javassist/>
