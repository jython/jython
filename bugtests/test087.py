"""
Test multiple inheritance of 2 java classes
"""

import support

try:
    import test087m
except TypeError, e:
    support.compare(e, "multiple inheritance")
else:
    raise support.TestError("multiple inheritance should fail")
