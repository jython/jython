"""
Java exception thrown for non-keyword argument following keyword
"""

import support

try:
   import test045s
except SyntaxError, e:
   support.compare(e, "non-keyword arg")


