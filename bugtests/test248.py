import support

support.compileJava("classes/test248j.java")

import java

onimport = 1

try:
  import test248j
  onimport = 0
  test248j()
except java.lang.ExceptionInInitializerError:
  if not onimport:
    raise support.TestWarning, "import itself does not imply class initialization"
else:
  raise support.TestError, "Expected an ExceptionInInitializerError"
