"""
no exception when too many arguments for format string
"""

import support

try:
    r = '%d' % (1, 2)
except TypeError, e:
    support.compare(e, "not all arguments converted")


