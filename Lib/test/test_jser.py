from test_support import *

print_test('Java Serialization (test_jser.py)', 1)

from java import io, awt
import os, sys

object1 = 42
object2 = ['a', 1, 1.0]
class Foo:
        def bar(self):
                return 'bar'

object3 = Foo()
object3.baz     = 99

object4 = awt.Color(1,2,3)

print_test('writing', 2)

sername = os.path.join(sys.prefix, "test.ser")
fout = io.ObjectOutputStream(io.FileOutputStream(sername))
print_test('Python int', 3)
fout.writeObject(object1)
print_test('Python list', 3)
fout.writeObject(object2)
print_test('Python instance', 3)
fout.writeObject(object3)
print_test('Java instance', 3)
fout.writeObject(object4)
fout.close()

fin     = io.ObjectInputStream(io.FileInputStream(sername))
print_test('reading', 2)
iobject1 = fin.readObject()
iobject2 = fin.readObject()
iobject3 = fin.readObject()
iobject4 = fin.readObject()
fin.close()

#print iobject1, iobject2, iobject3, iobject3.__class__, iobject4

print_test('Python int', 3)
assert iobject1 == object1

print_test('Python list', 3)
assert iobject2 == object2

print_test('Python instance', 3)
assert iobject3.baz     == 99
assert iobject3.bar() == 'bar'
assert iobject3.__class__ == Foo

print_test('Java instance', 3)
assert iobject4 == object4

os.remove(sername)