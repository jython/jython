"""
Check an array can be created from a string.
"""

import support

from jarray import array
s = "abcdef"
try:
    a = array(s, 'b')
except TypeError, e:
    raise support.TestWarning("I think an ascii string should be auto converted to a byte array")

