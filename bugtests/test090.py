"""
Access to innerclasses
"""

import support

support.compileJava("classes/test090j.java")

import test090j

foo = test090j()
bar = test090j.Bar()
bar.n = 10
r = foo.barTimesTwo(bar)

support.compare(r, "20")
