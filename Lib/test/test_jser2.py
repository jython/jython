from test import test_support
import unittest
import java
from org.python.util import PythonObjectInputStream

def serialize(o, special=0):
    b = java.io.ByteArrayOutputStream()
    objs = java.io.ObjectOutputStream(b)
    objs.writeObject(o)
    if not special:
        OIS = java.io.ObjectInputStream
    else:
        OIS = PythonObjectInputStream
    objs = OIS(java.io.ByteArrayInputStream(b.toByteArray()))
    return objs.readObject()

from jser2_classes import A, AJ, N, NL, NT

class TestJavaSerialisation(unittest.TestCase):

    def test_list(self):
        l = [1,"a", 3.0]
        l1 = serialize(l)
        self.assertEqual(l, l1)

    def test_dict(self):
        d = {'a': 3.0}
        d1 = serialize(d)
        self.assertEqual(d, d1)

    def test_tuple(self):
        t = (1, 'a')
        t1 = serialize(t)
        self.assertEqual(t, t1)

    def test_oldstyle(self):
        a = A('x')
        a1 = serialize(a)
        self.assertEqual(a, a1)

    # wasn't working in 2.1 either
    #def test_oldstyle_cls(self):
    #    A1 = serialize(A)
    #    self.assert_(A is A1)

    def test_jsubcl(self):
        aj = AJ('x')
        aj1 = serialize(aj, special=1)
        self.assertEqual(aj, aj1)

    def test_singletons(self):
        for v in (None, Ellipsis):
            v1 = serialize(v)
            self.assert_(v is v1)
            v1 = serialize((v,))[0]
            self.assert_(v is v1)

    def test_NotImplemented(self):
        # XXX serialize(NotImplemented) is None because of __tojava__
        v1 = serialize((NotImplemented,))[0]
        self.assert_(v1 is NotImplemented)

    def test_type(self):
        list1 = serialize(list)
        self.assert_(list1 is list)
        list1 = serialize((list,))[0]
        self.assert_(list1 is list)

    def test_user_type(self):
        N1 = serialize(N)
        self.assert_(N1 is N)
        N1 = serialize((N,))[0]
        self.assert_(N1 is N)

    def test_newstyle(self):
        n = N('x')
        n1 = serialize(n)
        self.assertEqual(n, n1)

    def test_newstyle_list(self):
        nl = NL('x',1,2,3)
        nl1 = serialize(nl)
        self.assertEqual(nl, nl1)

    def test_newstyle_tuple(self):
        nt = NT('x',1,2,3)
        nt1 = serialize(nt)
        self.assertEqual(nt, nt1)

def test_main():
    test_support.run_unittest(TestJavaSerialisation)

if __name__ == "__main__":
    test_main()
