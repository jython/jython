"""
[ #480390 ] main() does not throw exceptions
"""

import support

support.compileJPythonc("test340c.py", core=1, jar="test340.jar",
                        output="test340.err")
support.compileJava("test340j.java")

rc = support.runJava("test340j", classpath=".", expectError=1)
if rc != 42:
     support.TestError("Did not catch exception correctly %d" % rc)


