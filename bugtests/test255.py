"""

"""

import support

support.compileJPythonc("test255s1.py", output="test255s1.err")
support.compileJPythonc("test255s2.py", output="test255s2.err", keep=1)

support.runJava("test255s2", classpath="jpywork")
