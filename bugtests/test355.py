"""
[ 522558 ] list() is broken 
"""

import support

L = [1, 2, 3] 
L2 = list(L) 
L2.insert(0, 4) 
if L == L2:
    raise support.TestError('list() should create a copy')


