"""
unexpected behavior for format strings
"""

import support

try:
    r = '%d%d' % 1
except TypeError, e:
    support.compare(e, "not enough arguments for format string")


