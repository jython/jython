"""
Check that __module__ is not defined for functions.
"""

import support

def f():
    pass 

try:
   getattr(f, '__module__')
except AttributeError:
   pass
else:
   raise support.TestError("__module__ should not be defined for a function")
