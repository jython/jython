"""

"""

import support

alfa = {}
alfa["test"] = "test1"
try:
   del alfa["beta"] 
except KeyError, e:
   pass
else:
   raise support.TestError("Should raise")
