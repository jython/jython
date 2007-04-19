'''
Checks for bug 663592.  Used to be that if an abstract Java class called an
abstract method implemented by a Python subclass in its constructor, an
AttributeError would be thrown.
'''
import support

support.compileJava("classes/test396j.java")

import test396j

class Subclass(test396j):
    def __init__(self):
        test396j.__init__(self)
    
    def abstractMethod(self):
        pass

x = Subclass()
