"""

"""

import support

support.compileJava("classes/test232p/Foo.java")

import java

from test232p import Foo
try:
   f = Foo()
   f.hi()
except java.lang.IllegalAccessException:
   pass
else:
   raise support.TestError("expected an access exception")

