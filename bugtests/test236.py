
import support, os

support.compileJava("classes/test236j1.java")
os.remove("classes/test236j2.class")

import java
try:
   import test236j1
except ImportError:
   raise support.TestError, "Should not raise a simple ImportError"
except java.lang.NoClassDefFoundError:
   pass
else:
   raise support.TestError, "Should raise some exception"



