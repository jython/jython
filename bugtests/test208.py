"""

"""

import support

support.compileJava("classes/test208j.java")

import test208j
import test208j1
import test208j2

p=[test208j2()]
support.compare(test208j.foo(p), "test208j2\[\]")

p=[test208j1()]
support.compare(test208j.foo(p), "test208j1\[\]")
