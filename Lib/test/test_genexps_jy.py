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


def test_main():
    test_support.run_unittest(GeneratorExpressionsTestCase)

if __name__ == '__main__':
    test_main()
