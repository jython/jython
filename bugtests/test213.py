"""
Test %r on strings
"""

import support




r = "%s %r" % ("abc", "abc")

if r != "abc 'abc'":
   raise support.TestError, "wrong output %s" % r


