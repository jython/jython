
import support

support.compileJava("classes/test219j.java")

import test219i, test219e, test219j

class test219(test219i):
    def foo(self):
         raise test219e("raised in foo()")
    def foo2(self, i):
         if i == 0:
             raise test219e("test219e raised in foo()")
         else:
             raise java.lang.Throwable("Throwable raised in foo()")


a = test219()

test219j.checkFoo(a)
test219j.checkFoo2(a, 0)
test219j.checkFoo2(a, 1)
