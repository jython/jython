"""
Check subclassing strang thingies.
"""

import support

try:
   class foo(12): pass
except TypeError, e:
   support.compare(e, "base is not a class object")