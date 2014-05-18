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

class GetVariantsTestCase(unittest.TestCase):

    #http://bugs.jython.org/issue2133

    def test_get_does_not_vivify(self):
        d = defaultdict(list)
        self.assertEquals(d.get("foo"), None)
        self.assertEquals(d.items(), [])

    def test_get_default_does_not_vivify(self):
        d = defaultdict(list)
        self.assertEquals(d.get("foo", 42), 42)
        self.assertEquals(d.items(), [])

    def test_getitem_does_vivify(self):
        d = defaultdict(list)
        self.assertEquals(d["vivify"], [])
        self.assertEquals(d.items(), [("vivify", [])]) 


class KeyDefaultDict(defaultdict):
    """defaultdict to pass the requested key to factory function."""
    def __missing__(self, key):
        if self.default_factory is None:
            raise KeyError("Invalid key '{0}' and no default factory was set")
        else:
            val = self.default_factory(key)

        self[key] = val
        return val

    @classmethod
    def double(cls, k):
        return k + k

class OverrideMissingTestCase(unittest.TestCase):
    def test_dont_call_derived_missing(self):
        kdd = KeyDefaultDict(KeyDefaultDict.double)
        kdd[3] = 5
        self.assertEquals(kdd[3], 5)

    #http://bugs.jython.org/issue2088
    def test_override_missing(self):

        kdd = KeyDefaultDict(KeyDefaultDict.double)
        # line below causes KeyError in Jython, ignoring overridden __missing__ method
        self.assertEquals(kdd[3], 6)
        self.assertEquals(kdd['ab'], 'abab')


def test_main():
    test_support.run_unittest(PickleTestCase, ThreadSafetyTestCase, GetVariantsTestCase, OverrideMissingTestCase)


if __name__ == '__main__':
    test_main()
