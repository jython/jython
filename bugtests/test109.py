"""
Check looping over a dict.
"""

import support


try:
    for x in {3:'d', 2:'c', 1:'b', 0:'a'}:
	print x,
except TypeError, e:
    pass
else:
    raise support.TestError("Looping over dict should fail")
