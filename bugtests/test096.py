"""
Test the imp module (unfinished)
"""

import support
import sys

""" Does not work, will never work.

import imp

i = imp.find_module("test096j")


r = imp.load_module("test096j", i[0], i[1], i[2])

print r
print dir(r)
print sys.modules['test096j']


i = imp.find_module("test096j")
r = imp.load_module("test096j", i[0], i[1], i[2])

print r
print dir(r)
print sys.modules['test096j']

"""