"""
Check autocreation when java superclass misses a default constructor.
"""

import support

import java

class A(java.lang.ThreadGroup):
    def __init__(self):
	#java.lang.ThreadGroup.__init__(self, "spam")
	print self.name

try:
    A()
except TypeError:
    pass
