# -*- coding: utf-8 -*-
# unicode speed tests for access operations and find
# This is part of an effort to supply the Jython implementation of
# unicode with efficient, correct translations between the visible index
# of a character and and the index in the implementation array of the
# UTF-16 code unit(s) that represent it. See also test.test_unicode_jy,
# and Jython issue #2100. The presence of characters with Unicode
# code points > 0xffff (supplementary characters), that it takes two
# code units to represent, makes this non-trivial.
#
# The program defines a variety of test strings of differing lengths
# and distribution of supplementary characters, then times some basic
# operations involving index translation: of retrieval, slicing and
# find, to provide an average time per operation. It runs several trials
# of each test case (a combination of an operation and the test material)
# and reports the shortest trial, using the same strategy as the timeit
# module).
#
# It was difficult to get repeatable times from the test under Jython,
# because the JIT compiler (?) is non-deterministic. It proved
# impossible using a strategy that would run the same test case multiple
# times in succession. The approach eventually taken was to run the all
# the test cases once, then repeat this sequence several times, and
# report the minimum time of each case in these widely separated trials.
# This strategy provides stable results with the default JIT behaviour.
#
# Two interesting options are to run with the # JIT compiler disabled:
#   $ jython -J-Xint tests/python/unicode_index_times.py
#
# or with it continuously enabled:
#   $ jython -J-Xcomp tests/python/unicode_index_times.py
#
from __future__ import print_function
import itertools
import random
import sys
import time

if sys.platform == "win32" or sys.platform.startswith("java"):
    # On Windows and Jython, the best timer is time.clock()
    timer = time.clock
else:
    # On most other platforms the best timer is time.time()
    timer = time.time

if sys.version_info[0] > 2:
    unichr = chr
else:
    def next(it) : return it.next()

# Proportion of characters that are supplementary (if not explicit)
DEFAULT_RATE = 0.2  # 20%

# We will test performance with these sizes
SHORT = 10
MEDIUM = 100
LONG = 1000
HUGE = 10000


class UnicodeMaterial(object):
    ''' Object holding a list of single characters and a unicode string
        that is their concatenation. The sequence is created from a
        background sequence of basic plane characters and random
        replacement with supplementary plane characters (those with
        point code>0xffff).
    '''

    base = tuple(u'abcdefghijklmnopqrstuvwxyz')
    if sys.maxunicode < 0x10000:
        print("Narrow build: all characters from BMP", file=sys.stderr)
        supp = tuple(map(unichr, range(0x20, 0x2c)))
    else:
        # Wide build: we have real supplementary characters
        supp = tuple(map(unichr, range(0x10000, 0x1000c)))
    used = sorted(set(base+supp))

    def __init__(self, size=20, pred=None, ran=None):
        ''' Create size chars choosing an SP char at i where
            pred(ran, i)==True where ran is an instance of
            random.Random supplied in the constructor or created
            locally (if ran==None).
        '''

        # Generators for the BMP and SP characters
        base = itertools.cycle(UnicodeMaterial.base)
        supp = itertools.cycle(UnicodeMaterial.supp)

        # Each instance gets a random generator
        if ran is None:
            ran = random.Random()
        self.random = ran

        if pred is None:
            pred = lambda ran, j : ran.random() < DEFAULT_RATE

        # Generate the list
        r = list()
        for i in range(size):
            if pred(self.random, i):
                c = next(supp)
            else:
                c = next(base)
            r.append(c)

        # The list and its concatenation are our material
        self.ref = r
        self.size = len(r)
        self.text = u''.join(r)
        self.target = u''

    def __len__(self):
        return self.size

    def insert(self, target, p=None):
        ''' Insert target string at position p (or middle), truncating if
            that would make the material any longer
        '''
        if p is None:
            p = max(0, (self.size-len(target)) // 2)

        n = 0
        for t in target:
            if p+n >= self.size:
                break;
            self.ref[p+n] = t
            n += 1

        self.target = target[:n]
        self.text = u''.join(self.ref)


class UnicodeActions(UnicodeMaterial):
    ''' Provides test material with loops for timing.'''

    def __init__(self, size=20, pred=None, ran=None):
        super(UnicodeActions, self).__init__(size, pred, ran)
        if self.size <= 0:
            raise ValueError("The timings don't work for zero length")
        self.used =  UnicodeMaterial.used
        self.trash = None
        # String to find (note 'abcde' in base: nice for false alarms)
        self.insert(u"abide")


    def repeat_getitem(self, mincount):
        ''' Access each code point in sequence repeatedly so that at
            least mincount operations have been peformed, and return the
            actual number of operations.
        '''
        n = self.size
        t = self.text
        opcount = 0
        dummy = None
        while opcount < mincount:
            # Access each point code
            i = 0
            while i < n:
                # Avoid optimising away the call
                dummy = t[i]
                i += 1
            opcount += n
        self.trash = dummy
        return opcount

    def repeat_slice(self, mincount):
        ''' Extract a slice at each feasible position and length,
            repeating enough times to do mincount operations, and
            return the actual number of operations.
        '''
        n = self.size
        t = self.text
        opcount = 0
        dummy = None

        while opcount < mincount:
            m = 1
            while m <= n:
                starts = n - m + 1
                for i in range(starts):
                    dummy = t[i:i+m]
                    #print(i, i+m, step, dummy)
                opcount += starts
                m *= 5

        return opcount

    def repeat_slice_step(self, mincount):
        ''' Extract a slice at each feasible position and length,
            and using different sizes for the step,
            repeating enough times to do mincount operations, and
            return the actual number of operations.
        '''
        n = self.size
        t = self.text
        opcount = 0
        dummy = None
        steps = [3, -1, 10]

        while opcount < mincount:
            for step in steps:
                if step > 0:
                    m = 1
                    while m <= n:
                        starts = n - m + 1
                        for i in range(starts):
                            dummy = t[i:i+m:step]
                            #print(i, i+m, step, dummy)
                        opcount += starts
                        m *= 5
                else:
                    m = 1
                    while m <= n:
                        starts = n - m + 1
                        for i in range(starts):
                            dummy = t[i+m:i:step]
                            #print(i+m, i, step, dummy)
                        opcount += starts
                        m *= 5
                    
        return opcount

    def repeat_find_char(self, mincount):
        ''' Do an incremental find of each code point, repeating
            enough times to do mincount finds, and return the actual
            number of operations.
        '''
        opcount = 0
        n = self.size
        findop = self.text.find
        dummy = 0

        while opcount < mincount:
            # The text is searched for every code c.
            for c in self.used:
                # ... and every occurrence is found.
                start = 0
                while start < n:
                    i = findop(c, start)
                    if i < 0: break
                    # Avoid optimising away the call
                    dummy += i
                    start = i + 1

            # Every character in the text was a hit exactly once.
            # And every character was also a miss, except for
            # the character that ends the text. So we did:
            opcount += n + len(self.used) - 1

        self.trash = dummy
        return opcount

    def repeat_find_str(self, mincount):
        ''' Find the target string within the material, repeating
            enough times to do mincount finds, and return the actual
            number of operations.
        '''
        opcount = 0
        s = self.target
        findop = self.text.find
        dummy = 0

        while opcount < mincount:
            dummy += findop(s)
            opcount += 1

        self.trash = dummy
        return opcount

    def repeat_rfind_char(self, mincount):
        ''' Do an incremental rfind of each code point, repeating
            enough times to do mincount finds, and return the actual
            number of operations.
        '''
        opcount = 0
        n = self.size
        findop = self.text.rfind

        while opcount < mincount:
            # The text is searched for every code c.
            for c in self.used:
                # ... and every occurrence is found.
                end = n
                while end >= 0:
                    end = findop(c, 0, end)

            # Every character in the text was a hit exactly once.
            # And every character was also a miss. So we did:
            opcount += n + len(self.used)

        return opcount

    def repeat_rfind_str(self, mincount):
        ''' Find the target string within the material, repeating
            enough times to do mincount finds, and return the actual
            number of operations.
        '''
        opcount = 0
        s = self.target
        findop = self.text.rfind
        dummy = 0

        while opcount < mincount:
            dummy += findop(s)
            opcount += 1

        self.trash = dummy
        return opcount


def time_per_op(op, mincount):
    ''' Repeat the given operation at least mincount times and
        return the time per operation in microseconds.
    '''
    t = timer()
    opcount = op(mincount)
    return (timer() - t) * 1e6 / opcount

# Functions defining particular distributions of SP codes
#
def evenly(rate=DEFAULT_RATE):
    'Evenly distributed at given rate'
    def f(ran, i):
        return ran.random() < rate
    return f

def evenly_before(k, rate=DEFAULT_RATE):
    'Evenly distributed on i<k at given rate'
    def f(ran, i):
        return i < k and ran.random() < rate
    return f

def evenly_from(k, rate=DEFAULT_RATE):
    'Evenly distributed on i>=k at given rate'
    def f(ran, i):
        return i >= k and ran.random() < rate
    return f

def time_all():

    setups = [
        #("empty", UnicodeActions(0)),
        ("single bmp", UnicodeActions(1, (lambda ran, i : False))),
        ("single", UnicodeActions(1, (lambda ran, i : True))),
        ("short bmp", UnicodeActions(SHORT, (lambda ran, i : False))),
        ("short 50%", UnicodeActions(SHORT, evenly(0.5))),
        ("medium bmp", UnicodeActions(MEDIUM, (lambda ran, i : False))),
        ("medium 10%", UnicodeActions(MEDIUM, evenly(0.1))),
        ("long bmp", UnicodeActions(LONG, (lambda ran, i : False))),
        ("long 1%", UnicodeActions(LONG, evenly(0.01))),
        ("long 10%", UnicodeActions(LONG, evenly(0.1))),
        ("long 10% L", UnicodeActions(LONG, evenly_before(LONG/4, 0.4))),
        ("long 10% H", UnicodeActions(LONG, evenly_from(LONG-(LONG/4), 0.4))),
        ("long 50%", UnicodeActions(LONG, evenly(0.5))),
        ("huge bmp", UnicodeActions(HUGE, (lambda ran, i : False))),
        ("huge 10%", UnicodeActions(HUGE, evenly(0.1))),
    ]

    ops = [
        ("[i]", "repeat_getitem"),
        ("[i:i+n]", "repeat_slice"),
        ("[i:i+n:k]", "repeat_slice_step"),
        ("find(c)", "repeat_find_char"),
        ("find(s)", "repeat_find_str"),
        ("rfind(c)", "repeat_rfind_char"),
        ("rfind(s)", "repeat_rfind_str"),
    ]


    print("{:12s}{:>6s}".format("time (us)", "len"), end='')
    for title, _ in ops:
        print("{:>10s}".format(title), end='')
    print()

    mintime = dict()
    def save_mintime(k, t):
        mintime[k] = min(t, mintime.get(k, 1e9))

    trashcan = []

    # Cycle through the cases repeatedly.
    for k in range(5):
        for name, material in setups:
            for _, opname in ops:
                # For each case, keep the minimum time
                op = material.__getattribute__(opname)
                t = time_per_op(op, 1000)
                save_mintime((name, opname), t)

            if k == 0:
                trashcan.append(material.trash)

    # Cycle through the cases again to print them.
    for name, material in setups:
        print("{:12s}{:6d}".format(name, material.size), end='')
        for _, opname in ops:
            t = mintime[(name, opname)]
            print("{:10.2f}".format(t), end='')
        print()

    print("y =", trashcan)

if __name__ == "__main__":

    time_all()
