import support

support.compileJava("test241p/test241i.java")

import test241p
class A(test241p.test241i):
   def foo(self, i):
      return i

a = A()
v = a.foo(44)

if v != 44:
   raise support.TestError, "Wrong return value %d" % v
