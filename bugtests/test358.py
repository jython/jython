"""
[ 515894 ] Behaviour of "+=" stm. is different from
"""

import support

a = [1, 2, 3] 
a += "456" 
if a != [1, 2, 3, '4', '5', '6']:
    raise support.TestError('list += not working')
