"""
Check creation of a abstract javaclass.
"""

import support
from java import awt
import sys

try:
    a = awt.Component()
except TypeError:
    support.compare(sys.exc_info()[1], "abstract")
   
