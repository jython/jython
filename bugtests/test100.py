"""
Iterate over the "keys" sequence.
"""

import support

support.compileJava("test100j.java")

import test100j

r = test100j().iterate({'a':'1', 'b':'2', 3:'c'})

if len(r) != 6:
    raise support.TestError("len should be 6, %d" % len(r))
