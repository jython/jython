"""

"""

import support

import test200p1

s = test200p1.cls().toString()

if s[-10:] != "overridden":
   raise support.TestError("overridden method not working")