"""

"""

import support

support.compileJava("test265j.java")

import test265j

try:
   test265j()
except TypeError:
   pass
else:
   raise support.TestError("expected a TypeError (abstract java class)")



