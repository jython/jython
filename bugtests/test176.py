"""
Check that a list based on a tuple can not change the tuple
"""

import support

t1 = (1,2,3)
t2 = (1,2,3)
list(t1).reverse()

if t1 != t2:
   raise support.TestError('tuple was modified.')

