"""

"""

import support

assert(1)

try:
    assert(0)
except AssertionError, e:
    pass
else:
    raise support.TestError("Should raise an assert")
