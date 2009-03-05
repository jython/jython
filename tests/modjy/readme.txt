In order to run these unit tests

1. Set the JYTHON_HOME environment variable to point to the jython installation directory.
   If you are working from an SVN checkout, build jython, and then set the value to the
   "dist" subdirectory of your checkout.
2. Set the MOCKRUNNER_HOME environment variable. This should point to the install root of
   mockrunner, which you can download from http://mockrunner.sourceforge.net
3. Run the tests by simply typing "ant": the default target is "test".
4. Other available targets are
   test_java_15 - which tests modjy against java 1.5 and three different servlet versions
   test_java_16 - which tests modjy against java 1.6 and three different servlet versions
