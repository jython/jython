"""
Check subclassing a javaclass with protected constructor.
"""

import support
from java import awt

class A(awt.Component):
   pass

a = A()
