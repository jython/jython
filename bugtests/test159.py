"""

"""

import support

try:
   raise RuntimeError
except RuntimeError:
   pass
else:
   raise support.TestError("Should raise")
