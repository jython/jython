"""
Basic test, just raises an TestError
"""

import support
support.compileJPythonc("test315c.py", output="test315.err",
                        jar="test315.jar", core=1)
support.runJava("test315c", classpath="test315.jar")



