"""
ValueError raised instead of TypeError when not enough arguments for format string
"""

import support

try:
    r = '%d%d' % (1,)
except TypeError, e:
    support.compare(e, "not enough arguments for format string")


