"""
Check that a Writer can be assigned to sys.stdout.
"""

import support
from java import io
import sys

o = io.FileWriter("test004.out")
sys.stdout = o
print "spam",

f = open("test004.out", "r")
s = f.read(-1)
f.close()

if s != "spam":
    raise support.TestError('Wrong redirected stdout ' + `s`)
