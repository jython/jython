"""
__file__ should not be defined when eval.
"""

import support

try:
   eval('__file__')
except NameError, e:
   support.compare(e, "__file__")

