"""
"%012d" % -4 displays '0000000000-4'.
"""

import support

s = "%012d" % -4
support.compare(s, "-00000000004")


