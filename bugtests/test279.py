"""
Test access to public methods in a package private superclass.
"""

import java, support

support.compileJava("test279p/Foo.java")

from test279p import Foo 
f = Foo() 
try:
   f.hi() 
except java.lang.IllegalAccessException:
   raise support.TestWarning, "This should work. It works in pure java"

