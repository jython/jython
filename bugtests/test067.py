"""
foo(Byte(10)) doesn't invoke method "foo(byte bar)" as claimed in JPython\docs\usejava.html
"""

import support

support.compileJava("test067j.java")

import java
import test067j

t = test067j()
r = t.foo1(java.lang.Byte(10))

if r != "foo1 with byte arg: 10":
    raise support.TestError("Wrong method called")

