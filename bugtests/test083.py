"""
Test proxy maker from sys.path. CLASSPATH must *not* include "." in this test.
"""

import support

support.compileJava("test083j1.java")

import test083j1

class A(test083j1):
        pass

