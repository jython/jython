"""

"""

import support

import java 

x = java.lang.String('test')

try:
    list(x)
except AttributeError, e:
    support.compare(e, "__len__")
except TypeError, e:
    # Changed to a TypeError in 2.2
    support.compare(e, "iteration over non-sequence")
else:
    raise support.TestError("Should raise a AttributeError on __len__")
