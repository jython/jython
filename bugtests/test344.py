"""
[ #485968 ] cStringIO.softspace is not assignable.
"""

import support

import sys, cStringIO

h = cStringIO.StringIO()

sys.stdout = h
print "line1"
print "line2",
print "line3",
sys.stdout = sys.__stdout__

if h.getvalue() != "line1\nline2 line3":
    raise support.TestError('Wrong softspace handling in cStringIO"')
