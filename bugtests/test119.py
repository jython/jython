"""
Check serialization of PyJavaInstance subclasses. (from classpath).
#unitcheck
"""

import support

support.compileJava("classes/test119j.java")

import java, org
pi = java.io.PipedInputStream()
po = java.io.PipedOutputStream(pi)
oo = java.io.ObjectOutputStream(po)
oi = org.python.util.PythonObjectInputStream(pi)

import test119j

class B:
  b = 2

class C(test119j):
  c = 3

oo.writeObject(test119j())
if oi.readObject().a != 1:
    raise support.TestError("Deser of java class failed")


oo.writeObject(B())
if oi.readObject().b != 2:
    raise support.TestError("Deser of python class failed")

oo.writeObject(C())
if oi.readObject().c != 3:
    raise support.TestError("Deser of java subclass class failed")
