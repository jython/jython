"""

"""

import support

support.compileJPythonc("test297c.py", output="test297.err")

if support.grep("jpywork/test297c.java",
             r'TestFactory extends .*HTMLFactory', count=1) != 1:
    raise support.TestError('Subclassing an inner class should be possible')
