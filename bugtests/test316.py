"""
[ #449316 ] ArrayList()[0] should raise IndexError
"""

import support

from java.util import ArrayList 
al = ArrayList() 
try:
    foo = al[0] 
except KeyError:
    pass
else:
    raise support.TestError("Should raise a KeyError")
