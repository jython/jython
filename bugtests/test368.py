"""
[ 529242 ] Python singletons deserialization bug
"""

import support

from java.io import *
from org.python.util import *


SINGL= None
#SINGL= Ellipsis

class Test(Serializable):
    def __init__(self):
        self.attr = SINGL
    def test(self):
        if self.attr is not SINGL:
            raise support.TestError("Singleton not unique")
        if self.attr != SINGL:
            raise support.TestError("Singleton not unique")

def load(path):
    file = File(path)
    fileIn = FileInputStream(file)
    pyIn = PythonObjectInputStream(fileIn)
    pyObj = pyIn.readObject()
    pyIn.close()
    return pyObj

def save(obj, path):
    fileOut = FileOutputStream(path)
    objOut = ObjectOutputStream(fileOut)
    objOut.writeObject(obj)
    objOut.flush()
    objOut.close()

#print "Testing initial object..."
a = Test()
a.test()
save(a, "test368.out")
b = load("test368.out")
#print "Testing deserialized object..."
b.test()

