"""

"""

import support

try:
   raise KeyError, "abc"
except KeyError, value:
   support.compare(value, "abc")
   support.compare(value.__class__, "exceptions.KeyError")


