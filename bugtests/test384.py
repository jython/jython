"""
test for patch [ 1153003 ]
"""

import support
import jarray
import java
from org.python.core import ArgParser, PyObject

try:
    # test(1, arg1=2)
    args = jarray.array([1,2], PyObject)
    kwds = jarray.array(['arg1'], java.lang.String)
    ArgParser('test', args, kwds, 'arg1', 'arg2')
except TypeError:
    pass
else:
    raise support.TestError('Should raise a TypeError')
