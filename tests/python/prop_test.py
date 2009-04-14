from org.python.tests.identity import IdentityObject

#This test is for http://bugs.jython.org/issue1271
from org.python.tests.props import PropShadow

a = PropShadow.Derived()
assert a.foo() == 1, 'a'
assert a.bar() == 2, 'b'

from org.python.tests.props import PropShadow
b = PropShadow.Derived()
assert b.getBaz() == 4, 'c'
assert b.baz == 4, 'e'
assert b.getFoo() == 3, 'd'
assert b.foo() == 1, 'f'

