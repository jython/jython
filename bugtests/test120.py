"""
Check serialization of java subclasses.
#unitcheck
"""

import support

import java, org

class V(java.util.Vector):
    foo = 1

#print java.io.ObjectStreamClass.lookup(V);

oo = java.io.ObjectOutputStream(java.io.FileOutputStream("test120.out"))
oo.writeObject(V())

oi = org.python.util.PythonObjectInputStream(java.io.FileInputStream("test120.out"))
o = oi.readObject()
#print o
