"""
chr(None) throws NullPointerException
"""

import support

try:
   chr(None)
except TypeError:
   pass
