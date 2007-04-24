"""
Classes with greater a protected constructor can be subclassed and instantiated
Tests bug #649582
"""

import support

support.compileJava("classes/test395j1.java")
support.compileJava("classes/test395j2.java")

import test395j2

class Subclass(test395j2):
  def __init__(self):
    test395j2.__init__(self)

try:
  Subclass()
except AttributeError:
    raise support.TestWarning('Unable to access protected superclass constructor')
