import sys
print 'Testing JPython Version', sys.version

from test_support import *

print_test('Core Python Language', 0)

import test_grammar
import test_opcodes
import test_operations
import test_unpack
import test_pow
import test_builtin
import test_exceptions
import test_types

print_test('Standard Extension Modules', 0)

import test_string
import test_math
import test_thread
import test_time
#import test_re

print_test('Integration with Java', 0)

import test_jbasic
import test_jsubclass
import test_jser
import test_jarray

print 'All tests completed successfully!'
