"""

"""

import support


f = open("test260s1.py", "w")
f.write("""

import java
class test260s1(java.util.Vector):
   def foo(self):
       pass

class P(java.awt.Panel):
   pass

class foo:
   pass

""")
f.close()

support.compileJPythonc("test260s1.py", output="test260.err")



import os
os.remove("test260s1.py")

import sys, types
sys.path[:0] = ['jpywork']

import test260s1

#print dir(test260s1)
#print test260s1.P, type(test260s1.P)
#print test260s1.foo, type(test260s1.foo)

del sys.path[0]

if not hasattr(test260s1, "foo"):
    raise support.TestWarning("the python class should also be visible as a module attribute");


