"""
Check exception from re when unknown groupname is used.
"""

import support

import re
try:
   re.match(r"(?P<int>\d+)\.(\d*)", '3.14').group("misspelled")
except IndexError:
   pass
