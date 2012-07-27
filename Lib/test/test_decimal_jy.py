import unittest
from test import test_support

from decimal import Decimal

from java.math import BigDecimal


class TestJavaDecimal(unittest.TestCase):

    def test_decimal(self):
        self.assertTrue(hasattr(Decimal, "__tojava__"))
        x = Decimal("1.1")
        y = x.__tojava__(BigDecimal)
        self.assertTrue(isinstance(y, BigDecimal))


if __name__ == '__main__':
    unittest.main()
