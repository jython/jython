"""
[ #476580 ] 'del obj.non_member' : wrong exception
"""

import support
class C : pass 

o = C() 
try:
    o.foo 
except AttributeError:
    pass

try:
    del o.foo 
except AttributeError:
    pass
