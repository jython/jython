"""
[ 610576 ] Impl of abstract method not found
"""

import support

support.compileJava("test366i.java");
support.compileJava("test366j.java");

import test366i, test366j

class MyCls(test366j, test366i):
    def __init__(self):
        self.foo();

MyCls()

