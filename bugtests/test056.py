"""
Exceptions raised unpacking tuple args have wrong line number
"""

import support
import sys

def foo( (x,y) ): pass
try:
    foo(10)
except TypeError:
    tb = sys.exc_info()[2]
    if tb.tb_lineno == 0:
	raise support.TestError("Traceback lineno was zero")

