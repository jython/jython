"""
Check if id() returns unique values for different objects.
"""

import support

d = {}
cnt = 0

for i in xrange(100000):
    s = "test" + `i`
    j = id(s)
    if d.has_key(j):
        cnt = cnt + 1
    d[j] = s

if cnt != 0:
   raise support.TestWarning("%d id() value conflicts" % cnt)
