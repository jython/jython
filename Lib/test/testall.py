import sys
print 'Testing Jython Version', sys.version,'on',sys.platform

from test_support import *

print_test('Core Python Language', 0)

#import test_grammar
import test_opcodes
import test_operations
import test_unpack
import test_pow
#import test_builtin
#import test_exceptions
import test_types
import test_methods

print_test('Standard Extension Modules', 0)

import test_string
import test_math
import test_thread
import test_time
import test_re

print_test('Integration with Java', 0)

import test_jbasic
import test_jsubclass
import test_jser
import test_jarray
import test_janoninner

print 'All old-style tests completed successfully!'

print "Executing unittest tests"
from test_jreload import *
from test_javashell import *
from test_jy_compile import *

import unittest
unittest.main() # calls SystemExit
