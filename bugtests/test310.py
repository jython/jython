"""
[ #444292 ] local var binding overrides local import with jythonc
"""

import support

support.compileJPythonc("test310c.py", output="test310.err",
                        jar="test310.jar", core=1)
support.runJava("test310c", classpath="test310.jar")

