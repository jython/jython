"""
Check subclassing into python and back into java. (does not work)
"""

import support
"""
#support.compileJava("classes\\test105j1.java"):
#support.compileJava("classes\\test105j2.java"):
#support.compileJPythonc("-deep", "-workdir classes", "test105p3.py")
#support.compileJava("classes\\test105j4.java"):

import test105j1
import test105j2
import test105p3
import test105j4

j1 = test105j1()
support.compare(j1.fooify('hi'), "hifoo")

p3 = test105p3.test105p3()
print dir(p3)
print p3.__class__
print dir(p3.__class__)
print p3.__class__.__dict__['barify']

support.compare(p3.barify('hi'), "hifoobar")

j4 = test105j4()
support.compare(j4.bazify("aaa"), "hifoobarbaz")
"""