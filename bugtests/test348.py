"""
[ #490230 ] NotImplemented not implemented
"""

import support

class Z: 
    def __le__(self,o): 
        return NotImplemented 

z=Z() 
assert z<="a" 

