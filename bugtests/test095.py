"""
check some imp module functions (unfinished)
"""

""" Does not work, will never work.

import sys
import imp

fm = imp.find_module("test095m")
print fm

r = imp.load_module("test095m", fm[0], fm[1], fm[2])
print r
print "her1"
print r.__dict__.keys()
print r.__dict__['__path__']

print sys.modules['test095m']

"""