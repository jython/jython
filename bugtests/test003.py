"""
Check that a OutputStream can be assigned to sys.stdout.
"""

import support
from java import io
import sys

o = io.FileOutputStream("test003.out")
sys.stdout = o
print "hello"

f = open("test003.out", "r")
s = f.read(-1)
f.close()

if s != "hello\n":
    raise support.TestError('Wrong redirected stdout ' + `s`)
