"""
0.001 comes out as 0.0010
"""

import support
s = str(0.001)

if s != "0.001":
    raise support.TestError("0.001 comes out as " + `s`)
