
import support

support.compileJava("classes/test246p/test246j.java")

support.compileJPythonc("test246c.py", package="test246p", output="test246.err")


#
# This will fuck up the remaining tests if this test is aborted midways.
#
import sys
sys.path[:0] = ["jpywork"]

# make the test246 a package. Hmm.
open("jpywork/test246p/__init__.py", "w").close()

from test246c import testStatic
#from test246p.test246c import testStatic

v = testStatic.staticMethode('test')
if v != "staticMethode called in testStaticBase":
   raise support.TestError, "wrong result #1: %s" % v

t=testStatic()

v = t.staticMethode('test')
if v != "staticMethode called in testStaticBase":
   raise support.TestError, "wrong result #2: %s" % v

v = t.notStaticMethode('test')
if v != "notStaticMethode is called in testStatic":
   raise support.TestError, "wrong result #3: %s" % v

del sys.path[0] 
