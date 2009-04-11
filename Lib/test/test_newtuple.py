import unittest
from test import test_support, seq_tests
from org.python.core import PyNewTuple as newtuple

class TupleTest(seq_tests.CommonTest):
    type2test = newtuple

def test_main():
    test_support.run_unittest(TupleTest)

if __name__=="__main__":
    test_main()
