"""

"""

import support

from java.lang import Object

class A:
    pass

class B(A):
    pass

class C(Object):
    pass

class D:
    pass

support.compare(A.__module__, "__main__|test163")
support.compare(B.__module__, "__main__|test163")
support.compare(C.__module__, "__main__|test163")
support.compare(D.__module__, "__main__|test163")
if hasattr(Object, "__module__"):
    raise support.TestError("a java object should not have a __module__")

#raise support.TestError("" + `x`)
