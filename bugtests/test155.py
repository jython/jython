"""

"""

import support

#raise support.TestError("Test removed, it destroyes the test environment")


from org.python.core import PyList
class Test2(PyList):
    def foo(self):
         return self[0]

y = Test2()
y.append('spam')
v = y.foo()

if v != "spam":
    raise support.TestError("Should return 'spam' : " + `v`)
