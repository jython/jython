"""
[ 631047 ] %e formatting of float fails.
"""

import support

assert '%.*e' % (0, float(1000)) == '1e+003'

