"""
[ #477768 ] ord([123]) 21a3
"""

import support

try:
    ord([123])
except TypeError:
    pass

