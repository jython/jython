"""
Test against AIOOB exceptions.
"""

import support

def go2( a, b ):
    pass

try:
   go2( 1, 2, 3 )
except TypeError:
    pass

