"""

"""

import support

support.compileJava("classes/test182j.java")

support.compileJPythonc("test182c.py", core=1, jar="test182c.jar", output="test182.err")
support.runJava("test182c", classpath="test182c.jar")

#raise support.TestError("" + `x`)
