"""
Calling constructor on PyObject subclasses from python.
"""

import support

support.compileJava("classes/test142j.java")

import test142j

a = test142j([1.1, 1.2, 1.3])
support.compare(a.__repr__(), "1.1, 1.2, 1.3")

b = test142j.new([1.1, 1.2, 1.3, 1.4])
support.compare(b.__repr__(), "1.1, 1.2, 1.3, 1.4")


