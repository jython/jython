"""
[ 631017 ] Private fields mismangled
"""

import support

support.compileJPythonc("test375c.py", output="test375.err",
                        jar="test375.jar", core=1)
support.runJava("test375c", cp="test375.jar")
