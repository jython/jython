"""
Check handling of badly formed beans. 
Vector.size() function must override the Vector.size writeonly property.
"""

import support
from java import util
from types import *

v = util.Vector

support.compare(str(v.size), "<java function size")

if not callable(v.size):
   raise support.TestError('v.size should be callable')
