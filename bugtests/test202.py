"""
Check serialization of PyJavaInstance subclasses. (from classpath).
#unitcheck
"""

import support

support.compileJava("classes/test202j.java")

import java, org


oo = java.io.ObjectOutputStream(java.io.FileOutputStream("test202.out"))

import test202j

class B:
  b = 2

class C(test202j):
  c = 3


oo.writeObject(C())
oo.close()

oi = org.python.util.PythonObjectInputStream(java.io.FileInputStream("test202.out"))

if oi.readObject().c != 3:
    raise support.TestError("Deser of java subclass class failed")

#print "her4"
