"""

"""

import support

support.compileJava("classes/test206j1.java")

import java

#import test206j0
import test206j1

x = test206j1()
support.compare(x.getInt2(), "4")
support.compare(x.getInt(), "3")

