from test_support import *

import time

print_test('time (test_time.py)', 1)

time.altzone
time.clock()

t = 1.0e9
print_test('gmtime', 2)
assert time.gmtime(t)[0] == 2001 #(2001, 9, 9, 1, 46, 40, 6, 252, 0)

print_test('asctime')
assert time.asctime((2001, 9, 9, 1, 46, 40, 6, 252, 0))[-4:] == '2001' #'Sun Sep 09 01:46:40 2001'

print_test('ctime')
print_test('localtime')
assert time.ctime(t) == time.asctime(time.localtime(t))

time.daylight
print_test('mktime')
assert time.mktime(time.localtime(t)) == t


print_test('time', 2)
print_test('sleep', 2)
t0 = time.time()
time.sleep(1.2)
t1 = time.time()
assert abs(t1-t0 - 1.2) < 0.5

time.timezone
time.tzname

# expected errors
try:
    time.asctime(0)
except TypeError:
    pass

try:
    time.mktime((999999, 999999, 999999, 999999,
                 999999, 999999, 999999, 999999,
                 999999))
except OverflowError:
    pass
