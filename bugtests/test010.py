"""
Check negative ranges.
"""

import support
from java import io
import sys

r = range(5,-1,-1)

if len(r) != 6:
    raise support.TestError('Length of range wrong ' + `len(r)`)
