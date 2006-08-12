'''
Checks that all methods on a class are found, not just the first interface that has a method of the same name.  

Reported in bug 628315.
'''
import support
import java

synchList = java.util.Collections.synchronizedList(java.util.ArrayList())
synchList.add("a string")

if not synchList.remove(0) == 'a string':
    raise support.TestError, "'a string' should've been returned by the call to remove.  The object version of remove was probably called instead of the int version"

