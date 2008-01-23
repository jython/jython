import thread
import unittest
import test.test_support

class AllocateLockTest(unittest.TestCase):
    
    def test_lock_type(self):
        "thread.LockType should exist"
        t = thread.LockType
        self.assertEquals(t, type(thread.allocate_lock()), 
            "thread.LockType has wrong value")

def test_main():
    test.test_support.run_unittest(AllocateLockTest)

if __name__ == "__main__":
    test_main()
