"""
Check serialization of PyJavaInstance subclasses. (from Lib).
#unitcheck
"""

import support

support.compileJava("test117j.java")

import java, org
pi = java.io.PipedInputStream()
po = java.io.PipedOutputStream(pi)
oo = java.io.ObjectOutputStream(po)
oi = org.python.util.PythonObjectInputStream(pi)

import test117j

class B:
  b = 2

class C(test117j):
  c = 3

foo = test117j()
oo.writeObject(foo)
bar = oi.readObject()
#print type(foo), type(bar)
#print foo.__class__, bar.__class__
if bar.a != 1:
    raise support.TestError("Restored attrib should be 1")

oo.writeObject(B())
x = oi.readObject()
if x.b != 2:
    raise support.TestError("Restored attrib should be 2")

oo.writeObject(C())
x = oi.readObject()
if x.c != 3:
    raise support.TestError("Restored attrib should be 3")

