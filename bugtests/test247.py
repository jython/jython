
import support

support.compileJava("test247j.java")

import java

try:
  import test247j
except java.lang.ExceptionInInitializerError:
  pass
else:
  raise support.TestWarning, "Should raise an ExceptionInInitializerError"


#print dir(test247j)
#t = test247j()
