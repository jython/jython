import unittest
import threading
import time
from test import test_support

if test_support.is_jython:
    from java.util import ArrayList
    from java.lang import String

class ListTestCase(unittest.TestCase):

    def test_recursive_list_slices(self):
        x = [1,2,3,4,5]
        x[1:] = x
        self.assertEquals(x, [1, 1, 2, 3, 4, 5],
                          "Recursive assignment to list slices failed")

    def test_subclass_richcmp(self):
        # http://bugs.jython.org/issue1115
        class Foo(list):
            def __init__(self, dotstring):
                list.__init__(self, map(int, dotstring.split(".")))
        bar1 = Foo('1.2.3')
        bar2 = Foo('1.2.4')
        self.assert_(bar1 < bar2)
        self.assert_(bar1 <= bar2)
        self.assert_(bar2 > bar1)
        self.assert_(bar2 >= bar1)

    def test_setget_override(self):
        if not test_support.is_jython:
            return

        # http://bugs.jython.org/issue600790
        class GoofyListMapThing(ArrayList):
            def __init__(self):
                self.silly = "Nothing"

            def __setitem__(self, key, element):
                self.silly = "spam"

            def __getitem__(self, key):
                self.silly = "eggs"

        glmt = GoofyListMapThing()
        glmt['my-key'] = String('el1')
        self.assertEquals(glmt.silly, "spam")
        glmt['my-key']
        self.assertEquals(glmt.silly, "eggs")

    def test_tuple_equality(self):
        self.assertEqual([(1,), [1]].count([1]), 1) # http://bugs.jython.org/issue1317

class ThreadSafetyTestCase(unittest.TestCase):
    
    def setUp(self):
        self.globalList = []
        self.threads = []

    def test_append_remove(self):
        # derived from Itamar Shtull-Trauring's test for issue 521701
        def tester():
            ct = threading.currentThread()
            for i in range(1000):
                self.globalList.append(ct)
                time.sleep(0.0001)
                self.globalList.remove(ct)
        for i in range(10):
            t = threading.Thread(target=tester)
            t.start()
            self.threads.append(t)
        
        for t in self.threads:
            t.join(1.)
        for t in self.threads:
            self.assertFalse(t.isAlive())
        self.assertEqual(self.globalList, [])


def test_main():
    test_support.run_unittest(ListTestCase, ThreadSafetyTestCase)


if __name__ == "__main__":
    test_main()
