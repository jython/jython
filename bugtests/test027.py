"""
Check equallity of methods with itself.
"""

import support

def f():
   pass

class A:
    def f(self):
        pass

if f != f:
    raise support.TestError("Function compare was false")

if A.f != A.f:
    raise support.TestError("Unbound method compare was false")

a = A()

if a.f != a.f:
    raise support.TestError("Bound method compare was false")

