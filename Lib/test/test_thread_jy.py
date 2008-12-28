import thread
import synchronize
import unittest
import test.test_support
from java.lang import Thread
from java.util.concurrent import CountDownLatch

class AllocateLockTest(unittest.TestCase):
    
    def test_lock_type(self):
        "thread.LockType should exist"
        t = thread.LockType
        self.assertEquals(t, type(thread.allocate_lock()), 
            "thread.LockType has wrong value")

class SynchronizeTest(unittest.TestCase):
    def test_make_synchronized(self):
        self.doneSignal = CountDownLatch(10)
        self.i = 0
        class SynchedRun(Thread):
            def run(synchself):
                self.i = self.i + 1
                self.doneSignal.countDown()
            run = synchronize.make_synchronized(run)
        for _ in xrange(10):
            SynchedRun().start()
        self.doneSignal.await()
        self.assertEquals(10, self.i)


def test_main():
    test.test_support.run_unittest(AllocateLockTest, SynchronizeTest)

if __name__ == "__main__":
    test_main()
