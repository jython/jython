# tests some things about bound and unbound methods

class A:
    def one(self): print 'one'

class B(A):
    def two(self): print 'two'

class C(A):
    def one(self): print 'another one'

a = A()
b = B()
c = C()

print 'A.one ==? B.one', A.one == B.one
print 'C.one <>? A.one', A.one <> C.one

print 'A.one.im_func ==? a.one.im_func', A.one.im_func == a.one.im_func
print 'a.one.im_self ==? a', a.one.im_self == a
print 'a.one.im_class ==? A', a.one.im_class == A
print 'b.one.im_self ==? b', b.one.im_self == b
print 'b.one.im_class ==? A', b.one.im_class == A

A.one(b)
B.two(b)
B.one(b)

A.one(c)
C.one(c)

A.one(a)
print "im_class: ", B.one.im_class
B.one(a)
try:
    C.one(a)
    assert 0
except TypeError:
    pass
