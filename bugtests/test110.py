"""
Check "in" on a dict.
"""

import support

try:
    print 1 in {2:2, 3:3}
except TypeError, e:
    pass
else:
    raise support.TestError("in keyword an a dict should fail")
