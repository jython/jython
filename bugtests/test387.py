import support

import test387p.test387m

import sys

if not 'test387p.difflib' in sys.modules:
   raise support.TestError, 'Cached module for sibling module import miss should exist in sys.modules'
if not sys.modules['test387p.difflib'] is None:
   raise support.TestError, 'Cached module for sibling module import miss should be None in sys.modules'
