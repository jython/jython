"""
Check nonpublic constructor.
"""

import support

support.compileJava("classes/test121p/test121j.java")

from test121p import test121j

try:
   test121j()
except TypeError, e:
   support.compare(e, "no public constructor")
else:
   raise support.TestError("Should fail (access)")
