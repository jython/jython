"""
Check strange indexes/slices.
"""

import support
import java

class A(java.lang.Object):
    def toString(self):
       return "myname"

a = A()

n = java.lang.String.valueOf(a)

if n != "myname":
    raise support.TestError("python toString() did not override java toString():" + `n`)
