"""
raise with no arguments throws SyntaxError
"""

import support

r = None
try:
    try:
        raise support.TestError("dummy")
    except support.TestError:
	raise
except support.TestError, e:
    r = str(e)


if r != "dummy":
    raise support.TestError("Error not reraised")


