"""

"""

import support

support.compileJava("test137j.java")

import jarray  
a = jarray.array(range(5), 'i')

import test137j


test137j.zeroOutArray(a)
if a[-1] != 0:
    raise support.TestError("All elements should be 0")
