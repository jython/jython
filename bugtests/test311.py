"""
Test DST for time.mktime
"""

import support
import time

def check(tup, facit):
   #print tup, long(time.mktime(tup)), facit, (long(time.mktime(tup))-facit)
   assert time.mktime(tup) == facit

# These tests fail for CPython also.

'''
check((1998, 6, 13, 0, 0, 0, 0, 0, 0), 897692400)
check((1998, 6, 13, 0, 0, 0, 0, 0, -1), 897688800)
check((1998, 6, 13, 0, 0, 0, 0, 0, 0), 897692400)
check((1998, 6, 13, 0, 0, 0, 0, 0, 1), 897688800)
check((1998, 1, 13, 0, 0, 0, 0, 0, -1), 884646000)
check((1998, 1, 13, 0, 0, 0, 0, 0, 0), 884646000)
check((1998, 1, 13, 0, 0, 0, 0, 0, 1), 884642400)
'''
