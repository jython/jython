"""

"""

import support

from java.util import Hashtable
class Test(Hashtable):
    def isEmpty(self):
         return Hashtable.isEmpty(self)

t = Test()
if not t.isEmpty():
    raise support.TestError("Should be empty")
