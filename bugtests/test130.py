"""
Comparing ints and strings
"""

import support

if -1 > 'a':
    raise support.TestError("-1 > 'a'")
if not -1 < 'a':
    raise support.TestError("-1 < 'a'")
if 4 > 'a':
    raise support.TestError("4 > 'a'")
if not 4 < 'a':
    raise support.TestError("4 < 'a'")
if -2 > 'a':
    raise support.TestError("-2 > 'a'")
if not -2 < 'a':
    raise support.TestError("-2 < 'a'")
if -1 == 'a':
    raise support.TestError("-1 == 'a'")


