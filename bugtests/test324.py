"""
[ #467826 ] SHA digest() method doesn't work
"""

import support

import sha
s = sha.sha()
s.update("foo")
r = s.digest()

support.compare(len(r), "20")


