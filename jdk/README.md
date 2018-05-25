JDK
===

This directory contains source code for OpenJDK 7, and a build system for compiling the JDK with PolyLLVM to produce a shared library.

The OpenJDK codebase is available directly from the [openjdk](http://openjdk.java.net/guide/repositories.html#clone) website, or from one of several git clones such as [this one](https://github.com/dmlloyd/openjdk). However, many JDK Java source files are script-generated, and the build system for JDK 7 is notoriously difficult to configure. So for convenience we track a zip file that contains all JDK sources, prebuilt. This zip file was obtained from [here](https://sourceforge.net/projects/jdk7src/).

Building
--------

The Makefile in this directory will:
(1) Extract JDK source files from the tracked zip file.
(2) Apply PolyLLVM-specific patches to work around a few unsupported features such as reflection and threads. These patches should be removed as the corresponding features are implemented.
(3) Use PolyLLVM to compile the JDK Java source files down to LLVM IR.
(4) Use clang to compile LLVM IR down to object files.
(5) Use clang to link object files from the JDK into a shared library.

Notes
-----

- In addition to direct source patches, there is also a file called `jdk-method-filter.txt`, which PolyLLVM uses to filter out about a dozen methods and field initializers that cause problems due to differences between PolyLLVM and javac. The file has a comment for each method explaining why the method causes issues. This takes advantage of Polyglot's `-method-filter` flag.