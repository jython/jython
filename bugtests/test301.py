"""
[ #434324 ] jythonc -A and dead java vars
"""

import support

support.compileJava("classes/test301p/B.java")
support.compileJava("classes/test301p/A.java")
support.compileJPythonc("test301c.py", jar="test301.jar", core=1,
                                       addpackages="test301p",
                                       output="test301.err")

support.runJava("test301c", classpath="test301.jar", output="test301c.err")

