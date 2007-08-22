Welcome to Jython NewCompiler branch
====================================

This branch is forked from the 2.3 branch (at revision 3287) of Jython.

The new features in this branch has been developed by Damien Lejeune and
Tobias Ivarsson during the Google Summer of Code 2007.

External Dependancis
--------------------

The changes in this branch introduce dependancies on some external libraries.
These have to be installed prior to building this branch of Jython.

 * ASM 3.0 - The new bytecode generation framework depends on the main ASM jar
   and the asm-commons jar from version 3 of the ASM project. It also depends
   on the asm-tree jar and the asm-util jar for debugging.
   These jars can be obtained from the ASM website: http://asm.objectweb.org/
   There is also one jar (asm-all) that contains all the various parts of ASM.


QuickStart for PyASM
--------------------

1. Add asm-all-3.0.jar from http://asm.objectweb.org/ to your java classpath
2. Build Jython useing ant
3. Try PyASM on <your_python_source_file>.py with:
.../JythonNewcompilerBranch/PyASM $ jython test.py <your_python_source_file>.py


Further Documentation
---------------------

For further reading about how to use the new ASM based bytecode generation
framework (PyASM), and how this framework works, please refer to the file
PyASM/README.txt


