"""

"""

import support
import java, jarray

support.compileJava("test129j.java")

arr = jarray.zeros(10, java.lang.Class.forName('[I'))
for x in range(10): arr[x] = jarray.zeros(10, 'i')
 
import test129j

test129j.chk(arr)

if arr[0][0] != 47:
    raise support.TestError("Array[0][0] should be 47: %d" % arr[0][0])
