"""
[ #489836 ] Private names is not mangled
"""

import support

support.compileJPythonc("test345c.py", jar="test345.jar", core=1,
                        output="test345.out")
support.runJava("-jar test345.jar")
