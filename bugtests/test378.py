"""
[ 631430 ] read(-1) uses wrong fileposition.
"""

import support

f = open("test378.out", "wb")
f.write("123456789")
f.close()

f = open("test378.out")
f.read(4)
s = f.read();
f.close();

assert s == "56789"


