"""
xrange implementation is broken for almost any complex case
"""

import support

def cmp(s1, s2):
    if len(s1) != len(s2):
	return 0
    for i in range(len(s1)):
	if s1[i] != s2[i]:
	    return 0
    return 1


if not cmp(xrange(1, 10, 2), (1, 3, 5, 7, 9)):
    raise support.TestError("xrange error #1")
if not cmp(range(1, 10, 2), [1, 3, 5, 7, 9]):
    raise support.TestError("range error #1")

if not cmp(xrange(5, 10) * 3,  (5, 6, 7, 8, 9, 5, 6, 7, 8, 9, 5, 6, 7, 8, 9)):
    raise support.TestError("xrange error #2")
if not cmp(range(5, 10) * 3,  [5, 6, 7, 8, 9, 5, 6, 7, 8, 9, 5, 6, 7, 8, 9]):
    raise support.TestError("range error #2")

if not cmp(xrange(10, 5, -1), (10, 9, 8, 7, 6)):
    raise support.TestError("xrange error #3")
if not cmp(range(10, 5, -1), [10, 9, 8, 7, 6]):
    raise support.TestError("range error #3")

if len(xrange(1,5,-1)) != 0:
    raise support.TestError("xrange error #4")
if len(range(1,5,-1)) != 0:
    raise support.TestError("range error #4")

if not cmp(xrange(10)[9:1:-1],  (9, 8, 7, 6, 5, 4, 3, 2)):
    raise support.TestError("xrange error #5")
if not cmp(range(10)[9:1:-1], [9, 8, 7, 6, 5, 4, 3, 2]):
    raise support.TestError("range error #5")
