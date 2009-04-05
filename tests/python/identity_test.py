from org.python.tests.identity import IdentityObject

class PyIdentityObject (IdentityObject):
    pass

x = IdentityObject()
y = x.getThis()
        
a = PyIdentityObject()
b = a.getThis()
        
assert x is y
assert not(x is not y)
assert (x is y)  !=  (x is not y)
assert a is b
assert not(a is not b)
assert (a is b)  !=  (a is not b)

