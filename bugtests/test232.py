"""
Bug #222847 - Can't access public member of package private base class
"""

import support

support.compileJava("classes/test232p/Foo.java")

from test232p import Foo
try:
   Foo().hi()
except IllegalAccessException:
   raise support.TestError('Should be able to call public method on package protected superclass')


