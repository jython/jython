"""
Check looping over a dict.
"""

import support

L = []

for x in {3:'d', 2:'c', 1:'b', 0:'a'}:
    L.append(x)
L.sort()
if L != [0, 1, 2, 3]:
    support.TestError("Looping over dict should work")
