"""
[ 531256 ] Constructor problem using newInstance()
"""

import support

support.compileJava("test364p/X.java")
support.compileJava("test364p/Y.java")

from test364p import X,Y
class PyX(X): pass
class PyY(Y): pass
PyX.useClass(PyY)
X() # OK
try:
    PyX() # Not OK prints 'TypeError: Proxy instance reused'
except TypeError:
    raise support.TestWarning('Class ctor should mix with newInstance()')
