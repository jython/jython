"""
Check strange indexes/slices.
"""

import support

bits = ['1','1','0','0']

try:
   bits[2:1] = '2'
except TypeError:
   pass
else:
   pass
   # BW says this will be allowed in python1.6
   #raise support.TestError("Assignment to slice should fail, but didn't " + `bits`)



