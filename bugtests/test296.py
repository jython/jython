"""
[ #438108 ] __getitem__ called when it shouldn't?
"""

import support
import java

import test296p
try:
   print test296p.abc
except java.lang.StackOverflowError:
   raise support.TestWarning("Shouldn't raise a Stack overflow")

