"""
test of double seek
"""

import support

s1 = "abcdefghijklmnopqrstuvwxyz"
s2 = "0123456789"
f = open("test299.out", "wb")
f.write(s1)
f.close()

def verify(pos, res):
    print pos, res
    if pos != res:
        raise support.TestError, "Wrong filepos #1 (%d, %d)" % (pos, res)
    

f = open("test299.out", "rb")
f.read()
verify(f.tell(), 26)
f.seek(-10, 1)
verify(f.tell(), 16)
f.seek(-10, 1)
verify(f.tell(), 6)
f.seek(-1, 1)
verify(f.tell(), 5)
f.seek(-1, 1)
verify(f.tell(), 4)
f.close()

#raise support.TestWarning('A test of TestWarning. It is not an error')







