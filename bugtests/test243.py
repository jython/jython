
import java, support

import test243p
#print test243p
try:
   import test243p.P
except (ImportError, java.lang.NoClassDefFoundError):
   pass
else:
   raise support.TestError, "Should raise an exception"

