"""
[ #462280 ] builtin method as a class variable
"""

import support

class Foo: 
 mylistIndex = ['a', 'b', 'c', 'd', 'e'].index 

a = Foo() 
if a.mylistIndex.__self__ != ['a', 'b', 'c', 'd', 'e']:
    raise support.TestError('Builtin func with wrong self')
assert a.mylistIndex('c') == 2
assert Foo.mylistIndex('c') == 2

