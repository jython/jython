
import support, os

support.compileJava("classes/test236j1.java")
os.remove("classes/test236j2.class")

onimport = 1

import java
try:
   import test236j1
   onimport = 0
   test236j1()
except ImportError:
   raise support.TestError, "Should not raise a simple ImportError"
except java.lang.NoClassDefFoundError:
   if not onimport:
      raise support.TestWarning, "import itself does not imply class initialization"   
else:
   raise support.TestError, "Should raise some exception"



