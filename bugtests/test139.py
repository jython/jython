"""

"""

import support

support.compileJava("classes/test139j2.java")
support.compileJava("classes/test139j3.java")

import test139j1, test139j2

class myclass(test139j1, test139j2):
        def fooMethod(self):
                pass

import test139j3

if not test139j3.baz(myclass()):
    raise support.TestError("myclass should implement test139j2")
