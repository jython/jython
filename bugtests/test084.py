"""
Calling static methods in subclass.
"""

import support

support.compileJava("classes/test084j3.java")

import test084j3

r = test084j3.main(["xxx"])
support.compare(r, "test084j3")

r = test084j3.ohMy()
support.compare(r, "test084j3")
