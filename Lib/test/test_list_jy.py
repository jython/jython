import unittest
import test.test_support

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


def test_main():
    test.test_support.run_unittest(ListTestCase)


if __name__ == "__main__":
    test_main()
