

import support

support.compileJava("test235p/javaParent.java")

from test235p import javaParent

class pythonClass(javaParent) :
    def __init__(self,*args) :
         apply(javaParent.__init__,(self,)+args)

p = pythonClass(1)
