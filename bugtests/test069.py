"""
time.localtime() returns incorrect result
"""

import support

import time
t = time.localtime(time.time())

if t[0] == "1970":
    raise support.TestError("Epoch was reset")

