"""

"""

import support

import java 

x = java.lang.String('test')

try:
    list(x)
except AttributeError, e:
    support.compare(e, "__len__")
else:
    raise support.TestError("Should raise a AttributeError on __len__")
