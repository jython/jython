"""
Test a obviously corrupt $py.class file.
"""

import support
import java

f = open("test298m1.py", "w")
f.close()

f = open("test298m1$py.class", "w")
f.close()

try:
    import test298m1
except ImportError, e:
    pass
except java.lang.ArrayIndexOutOfBoundsException:
    raise support.TestWarning('Should not throw an ArratIndexOutOfBound')
else:
    raise support.TestError('Should throw an import error')

