"""
Check handling of badly formed beans. 
Vector.size() function must override the Vector.size writeonly property.
"""

import support
from java import util
from types import *

v = util.Vector

support.compare(str(v.size), "<java function size")

if type(v.size) != BuiltinFunctionType:
   raise support.TestError('Wrong type for v.size' + `type(v.size)`)
