"""

"""

import support



l = []
try:
    l.insert('a', 1)
except TypeError:
    pass
else:
    raise support.TestError("Should raise TypeError")
    


l = []
l.insert(1, 'a')
l.insert(6, 'b')
l.insert(-1, 'c')
l.insert(3, 'd')

