# test of PR#201

import sys
from test_support import *

print_test('Java Anonymous Inner Classes (test_janoninner.py)', 1)

print_test('importing', 2)
import javatests.AnonInner

print_test('instantiating', 2)
x = javatests.AnonInner()

print_test('invoking', 2)
assert x.doit() == 2000
