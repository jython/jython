"""
Check id() contra cmp()
"""

import support

try:
    a={[1,2]:3}
    if not a.has_key([1,2]):
        raise support.TestError("Lists hash inconsistently")
except TypeError, e:
    pass
else:
    raise support.TestError("Should raise a TypeError")
    

