import java
from test246p import test246j

class testStatic(test246j):
    def __init__(self):
        #return "testStatic.__init__ is called"
        pass

    def notStaticMethode(self,group):
        return "notStaticMethode is called in testStatic"

def staticMethode(group):
    return "staticMethode is called in test246c"
