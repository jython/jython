
This directory contains small tests that attempt to ensure that old fixed
bugs do not reappear.

Running
=======

Start a command prompt in this ("bugtests") directory. Make sure that 
the "classes" subdirectory is included in the CLASSPATH environment 
variable and that the "bugtests" directory is *not* included in in CLASSPATH.

Create a file called "support_config.py" which contains the following three
entries:

-----
java_home = ""
jython_home = ""
classpath = ""
-----

This is used to make the tests more platform independent.  My file for OS X
looks like:

-----
java_home = "/Library/Java/Home"
jython_home = "/Users/bzimmer/Development/sourceforge/jython/dist"
classpath = jython_home + "/jython.jar:classes"
-----

Run the script "driver.py".

After running the tests the applets should be tested by starting the 
appletviewer on all the *.html files. Both MS and javasoft's appletviewer
should be tested.

Adding new tests
================

The tests follow a strict naming scheme that ensure that we always know
which files that belong to each test. The main script of each test is called
"testNNN" where NNN is a 3-digit number. All other files that belong to this
test also starts with testNNN. There are no exceptions to this rule!

Dependent file normally follow a simple naming

testNNNp    a python package
testNNNm    a python module
testNNNj    a java class
testNNNi    a java interface
testNNNc    a python module meant for compilation with jythonc.
testNNNa    an applet, compiled with jythonc.
testNNNs    modules with deliberate syntax errors.

The tests should always complete without throwing exceptions or errors. Since
these tests also cover bugs which may not have been fixed yet, the test should
instead throw a TestWarning exception. When the bug is fixed the TestWarning
should be removed and replaced with a TestError instead.




