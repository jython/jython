"""

"""

import support

import string
a = "From: user@host"
b = string.find(a, ":")

if b != 4:
    raise support.TestError("string.find error" + `b`)
