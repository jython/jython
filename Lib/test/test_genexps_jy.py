"""Misc generator expression tests

Made for Jython.
"""
import unittest
from test import test_support

locals_test = list(local for local in locals() if not local.startswith('_'))

class GeneratorExpressionsTestCase(unittest.TestCase):

    def test_module_level_locals(self):
        # NOTE: The locals_test genexp used to cause a 'dictionary
        # changed size during iteration' RuntimeError. If we've gotten
        # this far we've already passed
        self.assert_(sorted(locals_test) == ['test_support', 'unittest'])

    #http://bugs.jython.org/issue1205 applied to genexps.
    def test_long_genexp(self):
        r = 2
        g = ((x1**3+x2**3,(x1,x2),(y1,y2)) for x1 in range(4) for x2 in range(4)
                if x1 < x2 for y1 in range(r) for y2 in range(r) if y1 < y2
                if x1**3+x2**3 == y1**3+y2**3 )
        self.assertEquals(g.next(), (1, (0, 1), (0, 1)))

def test_main():
    test_support.run_unittest(GeneratorExpressionsTestCase)

if __name__ == '__main__':
    test_main()
