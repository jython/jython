import support

support.compileJava("classes/test248j.java")

import java

try:
  import test248j
except java.lang.ExceptionInInitializerError:
  pass
else:
  raise support.TestError, "Expected an ExceptionInInitializerError"
