"""
Serialization test.
#unitcheck
"""

import support


class Data:
        data = "Hello World"

class Test:
        text = Data()

class Factory:
        def createTest(x):
                return Test()

factory = Factory()
foo = factory.createTest()


from java import io
import sys

filename = "test101.out"

fout = io.ObjectOutputStream(io.FileOutputStream(filename))
fout.writeObject(foo)
fout.close()

fin = io.ObjectInputStream(io.FileInputStream(filename))
foo = fin.readObject()
fin.close()


support.compare(foo, "<(__main__|test101).Test instance at")
support.compare(foo.text, "<(__main__|test101).Data instance at")
support.compare(foo.text.data, "Hello World")


