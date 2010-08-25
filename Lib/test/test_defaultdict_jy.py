"""Misc defaultdict tests.

Made for Jython.
"""
import pickle
import time
import threading
import unittest
from collections import defaultdict
from test import test_support
from random import randint

from java.util.concurrent.atomic import AtomicInteger

class PickleTestCase(unittest.TestCase):

    def test_pickle(self):
        d = defaultdict(str, a='foo', b='bar')
        for proto in range(pickle.HIGHEST_PROTOCOL + 1):
            self.assertEqual(pickle.loads(pickle.dumps(d, proto)), d)


# TODO push into test_support or some other common module - run_threads
# is originally from test_list_jy.py

class ThreadSafetyTestCase(unittest.TestCase):

    def run_threads(self, f, num=10):
        threads = []
        for i in xrange(num):
            t = threading.Thread(target=f)
            t.start()
            threads.append(t)
        timeout = 10. # be especially generous
        for t in threads:
            t.join(timeout)
            timeout = 0.
        for t in threads:
            self.assertFalse(t.isAlive())

    def test_inc_dec(self):

        class Counter(object):
            def __init__(self):
                self.atomic = AtomicInteger()
                 # waiting is important here to ensure that
                 # defaultdict factories can step on each other
                time.sleep(0.001)

            def decrementAndGet(self):
                return self.atomic.decrementAndGet()

            def incrementAndGet(self):
                return self.atomic.incrementAndGet()

            def get(self):
                return self.atomic.get()

            def __repr__(self):
                return "Counter<%s>" % (self.atomic.get())

        counters = defaultdict(Counter)
        size = 17
        
        def tester():
            for i in xrange(1000):
                j = (i + randint(0, size)) % size
                counters[j].decrementAndGet()
                time.sleep(0.0001)
                counters[j].incrementAndGet()

        self.run_threads(tester, 20)
        
        for i in xrange(size):
            self.assertEqual(counters[i].get(), 0, counters)


def test_main():
    test_support.run_unittest(PickleTestCase, ThreadSafetyTestCase)


if __name__ == '__main__':
    test_main()
