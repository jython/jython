
import unittest
from test_support import run_suite

class C:
  def __str__(self):
    raise Exception("E")
  def __repr__(self):
    raise Exception("S")

class ExceptionHandling(unittest.TestCase):
  def testBugFix1149372(self):
    try:
      c = C()
      str(c)
    except Exception, e:
      assert e.args[0] == "E"
      return
    unittest.fail("if __str__ raises an exception, re-raise")

def test_main():
  test_suite = unittest.TestSuite()
  test_loader = unittest.TestLoader()
  def suite_add(case):
    test_suite.addTest(test_loader.loadTestsFromTestCase(case))
  suite_add(ExceptionHandling)
  run_suite(test_suite)

if __name__ == "__main__":
  test_main()

