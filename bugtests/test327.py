"""
[ #458945 ] Missing 'lastindex' on match objects
"""

import support

import re

m = re.match(r"(\w*) (\w*) (\w*)", "word1 word2 word3")
if m.lastindex != 3:
    raise support.TestError('Wrong lastindex value#1 : %d' % m.lastindex)

m = re.match(r"((\w*) )+", "word1 word2 word3 ")
if m.lastindex != 2:
    raise support.TestError('Wrong lastindex value#2 : %d' % m.lastindex)

m = re.match(r"abc", "abc")
if m.lastindex != None:
    raise support.TestError('Wrong lastindex value#3 : %d' % m.lastindex)

