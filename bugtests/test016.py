"""
Check using self as a % mapping.
"""

import support

class A:
    def __init__(self):
	self.a = "a"
	b = "using self.__dict as %(a)s mapping" % self.__dict__

A()

