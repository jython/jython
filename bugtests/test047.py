"""
"TypeError: can't instantiate abstract class" thrown for class with no public constructors
"""

import support

import java

try:
   java.lang.Math() 
except TypeError, e:
   support.compare(e, "no public constructors for")

