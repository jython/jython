"""

"""

import support

support.compileJPythonc("test189c.py", output="test189.err")

if support.grep("jpywork/test189c.java", r'makeClass\("test189c".* test189c.class\)', count=1) != 1:
    raise support.TestError("test189c should use test189c.class as proxy")

if support.grep("jpywork/test189c.java", r'makeClass\("test189c2".* test189c2.class\)', count=1) != 1:
    raise support.TestError("test189c2 should use test189c.class as proxy")



