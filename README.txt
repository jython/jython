Jython: Python for the Java Platform

Welcome to Jython 2.7.1 beta 3!

This is the third beta release of the 2.7.1 version of Jython. Along with
language and runtime compatibility with CPython 2.7.1, Jython 2.7 provides
substantial support of the Python ecosystem. This includes built-in support of
pip/setuptools (you can use with bin/pip) and a native launcher for Windows
(bin/jython.exe), with the implication that you can finally install Jython
scripts on Windows.

* Note that if you have JYTHON_HOME set, you should unset it to avoid problems
with the installer and pip/setuptools.

Jim Baker presented a talk at PyCon 2015 about Jython 2.7, including
demos of new features: https://www.youtube.com/watch?v=hLm3garVQFo

The release was compiled on OSX using JDK 7 and requires a minimum of
Java 7 to run.

Please try this release out and report any bugs at http://bugs.jython.org
You can test your installation of Jython (not the standalone JAR) by running
the regression tests, with the command:

jython -m test.regrtest -e -m regrtest_memo.txt

For Windows, there is a simple script to do this: jython_regrtest.bat. In
either case, the memo file regrtest_memo.txt will be useful in the bug report
if you see test failures. The regression tests can take about half an hour.

Please see ACKNOWLEDGMENTS for details about Jython's copyright,
license, contributors, and mailing lists; and NEWS for detailed
release notes, including bugs fixed, backwards breaking changes, and
new features. Thanks go to Amobee (http://www.amobee.com/) for
sponsoring this release. We also deeply thank all who contribute to
Jython, including - but not limited to - bug reports, patches, pull
requests, documentation changes, support emails, and fantastic
conversation on Freenode at #jython.
