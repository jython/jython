"""
Check that __builtin__ exists in sys.modules.
"""

import support
import sys

if not sys.modules.has_key('__builtin__'):
   raise support.TestError("__builtin__ should exist in sys.modules")
