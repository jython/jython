"""
Check that UEE also matches IOError.
"""

import support


import java

try:
    x = java.io.OutputStreamWriter(java.lang.System.out, "garbage")
except java.io.UnsupportedEncodingException, e:
    pass
else:
    raise support.TestError("Should raise an exception")


try:
    x = java.io.OutputStreamWriter(java.lang.System.out, "garbage")
except IOError, e:
    pass
else:
    raise support.TestError("Should raise an exception")
