
import support

class A:
    def foo(self):
	return "A.foo"

class B:
    def foo(self):
        return "B.foo"

class C(A, B):
    pass


list(C.__bases__).reverse()


c = C()
#print c.foo
support.compare(c.foo(), "A.foo")
