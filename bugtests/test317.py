"""
[ #448485 ] Tuple unpacking raises KeyError 
"""

import support
support.compileJPythonc("test317c.py", output="test317.err",
                        jar="test317.jar", core=1)
support.runJava("test317c", cp="test317.jar")



