"""
Check subclassing strange thingies.
"""

import support

try:
  class foo(12): pass
except TypeError, e:
  pass
else:
  raise support.TestError("expecting a TypeError for attempting to subclass integer instance")

