"""

"""

import support

import test242c

support.compileJPythonc("test242c.py", deep=1, output="test242.err")

support.runJava("test242c", classpath="jpywork")