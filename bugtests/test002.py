"""
Check that calling __call__ on a class creates a new instance.
"""

import support

class A:
    def __init__(self, arg1, arg2):
	pass

inst1 = A("aa", "bb")
inst2 = A.__call__("aa", "bb")

if type(inst1) != type(inst2):
    raise support.TestError("class.__call__ did not return a instance");
