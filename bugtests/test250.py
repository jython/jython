
import support

support.compileJava("test250j.java")

import test250j

t = test250j()
v = t[3]
if v != 6:
   raise support.TestError, "wrong return value %d" % v

try:
  print t.a
except AttributeError:
  # Do not support __getattr__ on java classes. (for now at least)
  pass 
