"""

"""

import support

support.compileJPythonc("test190c.py", output="test190.err")

if support.grep("jpywork/test190c.java", "SubDate extends .*Date", count=1) != 1:
   raise support.TestError("SubDate should extends java.util.Date")
