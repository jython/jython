"""
Jythonc test of xml
"""

import support

support.compileJPythonc("test313c.py", output="test313.err",
                        jar="test313.jar", core=1)
support.runJava("test313c", classpath="test313.jar")

