"""
Check mixing write & print
"""


import support
from java import io
import sys

o = io.FileOutputStream("test009.out")
sys.stdout = o

print "p1,",
sys.stdout.write("p2")
print "p3,",

f = open("test009.out", "r")
s = f.read(-1)
f.close()

if s != "p1,p2p3,":
    raise support.TestError('Error mixing write and print ' + `s`)
