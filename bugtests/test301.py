"""
Basic test, just raises an TestError
"""

import support

support.compileJava("classes/test301p/A.java")
support.compileJava("classes/test301p/B.java")
support.compileJPythonc("test301c.py", jar="test301.jar", core=1,
                                       addpackages="test301p",
                                       output="test301.err")

ret = support.runJava("test301c", cp="test301.jar", output="test301c.err",
                                  expectError=1)
if ret != 0:
    raise support.TestWarning('This should work. See test301c.err for details')

