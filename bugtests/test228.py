"""

"""

import support

import test228s

support.compileJPythonc("test228s.py", deep=1, output="test228.err")

support.runJava("test228s", classpath="jpywork")