"""
[ 522423 ] cStringIO has no reset() method
"""

import support

import cStringIO
s = cStringIO.StringIO("abcdef")
s.read(3)
s.reset()
support.compare(s.read(3), "abc")
support.compare(s.read(3), "def")

