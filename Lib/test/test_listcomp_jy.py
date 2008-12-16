import unittest
from test import test_support

class ListCompTestCase(unittest.TestCase):

    #http://bugs.jython.org/issue1205
    def test_long_listcomp(self):
        r = 2
        lc = [(x1**3+x2**3,(x1,x2),(y1,y2)) for x1 in range(4) for x2 in range(4)
                if x1 < x2 for y1 in range(r) for y2 in range(r) if y1 < y2
                if x1**3+x2**3 == y1**3+y2**3 ]
        self.assertEquals(len(lc), 1)
        self.assertEquals(lc, [(1, (0, 1), (0, 1))])

def test_main():
    test_support.run_unittest(ListCompTestCase)

if __name__ == '__main__':
    test_main()
