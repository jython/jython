"""
[ #473676 ] cStringIO bug
"""

import support

import cStringIO 

s = cStringIO.StringIO() 
r = s.read(1) 

if len(r) != 0:
    raise support.TestError('EOF must be the empty string')


s = cStringIO.StringIO("abc") 
r = s.read(2) 
assert len(r) == 2
r = s.read(1) 
assert len(r) == 1
r = s.read(1) 
if len(r) != 0:
    raise support.TestError('EOF must be the empty string #2')

