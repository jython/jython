"""
Bad input to __import__ raises a Java exception.
"""

import support

try:
    __import__('')
except ValueError, e:
    support.compare(e, "Empty module name")



