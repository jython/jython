"""
Check using null as explit self.
"""

import support

import string

try:
   string.split(None, " ")
except TypeError:
   pass
except AttributeError:
   pass # BW says this should be correct in python1.6