"""

"""

import support
support.compileJava("classes/test217p/test217i.java")

support.compileJPythonc("test217c.py", output="test217.err")
support.compileJava("test217t.java", classpath="jpywork")
support.runJava("test217t", classpath="jpywork;.")
