"""

"""

import support

import test221c

support.compileJPythonc("test221c.py", deep=1, output="test221.err")

support.runJava("test221c", classpath="jpywork")