"""
Check strange indexes/slices.
"""

import support

import java

try:
   java.util.Date("99-01-01", 1, 1)
except TypeError, e:
   support.compare(e, "1st arg")




