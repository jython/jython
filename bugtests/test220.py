"""

"""

import support

support.compileJava("classes/test220j.java")
support.compileJPythonc("test220c.py", output="test220.err")

support.runJava("test220c", classpath="jpywork")
