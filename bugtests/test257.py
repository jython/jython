"""

"""

import support

f = open("test257s1.py", "w")
f.write("""
import java, sys
class test257s1(java.lang.Object):
   pass

foo = 1
""")
f.close()

support.compileJPythonc("test257s1.py", output="test255s1.err")

import os
os.remove("test257s1.py")

import sys, types
sys.path[:0] = ['jpywork']

import test257s1

if type(test257s1) != types.ModuleType:
    raise support.TestWarning, "a compiled module should still be a module"


if type(test257s1.test257s1) != types.ClassType:
    raise support.TestError, "a compiled class should still be a class"

del sys.path[0]


