"""

"""

import support

class A:
   def __getitem__(self, key):
       raise IndexError, "hello"

try:
   A()['b']
except IndexError:
   pass
else:
   raise support.TestError("Should raise IndexError")
