"""
Test default args to string functions.
"""

import support
import string

r2 = string.find("abcdef", "c", 0, 6)
r3 = string.find("abcdef", "d", 0)
r4 = string.find("abcdef", "e")

if r2 != 2:
    raise support.TestError('#1 Wrong string.find ' + `r2`)
if r3 != 3:
    raise support.TestError('#2 Wrong string.find ' + `r3`)
if r4 != 4:
    raise support.TestError('#3 Wrong string.find ' + `r4`)

try:
   r3 = string.find("abcdef", "f", 0, None)	#stmt 2
except TypeError, e:
   support.compare(e, "expected an integer")
