"""

"""

import support

support.compileJava("test157j.java")

import test157j

from jarray import *
f = zeros(2,'d');
support.compare(test157j.test(f), "double"); 
if f[0] == 0:
    raise support.TestError("array not changed")


f = zeros(2,'f'); 
support.compare(test157j.test(f), "float"); 
if f[0] == 0:
    raise support.TestError("array not changed")

