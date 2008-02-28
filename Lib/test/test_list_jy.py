import unittest
import test.test_support

class ListTest(unittest.TestCase):
        
        def test_recursive_list_slices(self):
                x = [1,2,3,4,5]
                x[1:] = x

                self.assertEquals(x, [1, 1, 2, 3, 4, 5],
                        "Recursive assignment to list slices failed")

def test_main():
        test.test_support.run_unittest(ListTest)

if __name__ == "__main__":
        test_main()
