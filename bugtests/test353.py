"""
[ #495604 ] imp.find_module fails when None is 2 arg
"""

import support

import imp
imp.find_module("re", None)

