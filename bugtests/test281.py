"""

"""

import support

support.compileJPythonc("test281c.py", core=1, jar="test281c.jar", output="test281.err")
support.runJava("test281c", classpath="test281c.jar")

#raise support.TestError("" + `x`)
