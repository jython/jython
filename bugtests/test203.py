"""
Check serialization of PyJavaInstance subclasses. (from classpath).
#unitcheck
"""

import support


import java, org

pi = java.io.PipedInputStream()
po = java.io.PipedOutputStream(pi)
oo = java.io.ObjectOutputStream(po)
oi = org.python.util.PythonObjectInputStream(pi)

class C(java.util.Date):
  c = 3

c = C()
#print c
oo.writeObject(c)

c = oi.readObject()
#print c

support.compare(c.c, "3")

