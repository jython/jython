"""

"""

import support

support.compileJPythonc("test191c.py", output="test191.err")

lines = [
   r'public boolean after\(java.util.Date when2, boolean flag\) {',
   r'public boolean after\(java.util.Date when2\) {'
]

for l in lines:
    if support.grep("jpywork/test191c.java", l, count=1) != 1:
	raise support.TestError("Line %s not found in .java files" % l)
