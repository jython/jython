"""
Check for lists and dicts as key in a dict.
"""

import support

try:
    a = {[1]:2}
    a = {{1:2}:3}
except TypeError, e:
    pass
else:
    raise support.TestError("Should fail, lists and dicts not allowed as key")
