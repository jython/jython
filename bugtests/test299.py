"""
Test that open append position filepointer at the end
"""

import support

s1 = "abcdefghijklmnopqrstuvwxyz"
s2 = "0123456789"
f = open("test299.out", "wb")
f.write(s1)
f.close()
f = open("test299.out", "ab")
f.write(s2)
f.close()

f = open("test299.out", "rb")
res = f.read()
f.close()

if res != s1 + s2:
    raise support.TestError('File did not append correctly')



