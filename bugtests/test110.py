"""
Check "in" on a dict.
"""

import support

if 1 in {2:2, 3:3}:
    raise support.TestError("in keyword an a dict should work")
if 2 not in {2:2, 3:3}:
    raise support.TestError("in keyword an a dict should work")
