"""
PySequence.slice_indices() returns results that will be confusing to people trying to subclass PySequence
"""

import support
import org

class SeqTest(org.python.core.PySequence):
    def __len__(self):  return 100
    def get(self, i):  return i
    def getslice(self, b,e,s):  return b,e,s
    def repeat(self, x):  return x

s = SeqTest()

s[99]

try:
   s[101]
except IndexError, e:
   pass
else:
   raise support.TestError("Should raise KeyError")

if s[1:3:2] != (1,3,2):
   raise support.TestError("Strange return")
