"""
Subclassing PyInteger
"""

import support

IntegerType = type(1)
class SpecialInt (IntegerType):
    def __init__(self, n):  IntegerType.__init__(self, n)
    def double(self):  return self*2

si = SpecialInt(12)

#print str(si)
support.compare(si, '12')
support.compare(si + 1, '13')
support.compare(si + si, '24')
support.compare(3 + si, '15')

