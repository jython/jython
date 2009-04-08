# Check every path through every method of UserList

from org.python.core import PyNewList as newlist
import unittest
from test import test_support, list_tests

class NewListTest(list_tests.CommonTest):
    type2test = newlist

def test_main():
    test_support.run_unittest(NewListTest)

if __name__ == "__main__":
    test_main()
