# Python test set -- part 7, bound and unbound methods

from test_support import *

print_test('Bound and unbound methods (test_methods.py)', 1)

class A:
    def one(self): return 'one'

class B(A):
    def two(self): return 'two'

class C(A):
    def one(self): return 'another one'

a = A()
b = B()
c = C()

print_test('unbound method equality', 2)
assert A.one == B.one
assert A.one <> C.one

print_test('method attributes', 2)
assert A.one.im_func == a.one.im_func
assert a.one.im_self == a
assert a.one.im_class == A
assert b.one.im_self == b
assert b.one.im_class == A

print_test('unbound method invocation w/ explicit self', 2)
assert A.one(b) == 'one'
assert B.two(b) == 'two'
assert B.one(b) == 'one'

assert A.one(c) == 'one'
assert C.one(c) == 'another one'

assert A.one(a) == 'one'
assert B.one(a) == 'one'
try:
    C.one(a)
    assert 0
except TypeError:
    pass

print_test('"unbound" methods of builtin types', 2)
w = [1,2,3].append
x = [4,5,6].append
assert w <> x
assert w.__self__ <> x.__self__

y = w.__self__[:]
z = x.__self__[:]

assert y.append.__self__ <> w
z.append(7)
assert z == (x.__self__+[7])
