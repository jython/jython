"""
#unitcheck
"""

import support

import sys, org
from java import io
from java.awt import Panel

class MyPanel (Panel):
    pass

def serialize_object (obj, filename):
    #print "serializing", obj, "to", filename
    file = io.FileOutputStream (filename)
    objstream = io.ObjectOutputStream (file)
    objstream.writeObject (obj)
    objstream.close() ; file.close()
    #print "done"

def unserialize_object (filename):
    #print "reading serialized object from", filename
    file = io.FileInputStream (filename)
    objstream = org.python.util.PythonObjectInputStream (file)
    obj = objstream.readObject ()
    objstream.close() ; file.close()
    #print "unserialized", obj

filename = 'test156.ser'

p = MyPanel ()
serialize_object (p, filename)
unserialize_object (filename)

#raise support.TestError("" + `x`)
