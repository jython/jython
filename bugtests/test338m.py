#import test338j1

class test338m(test338j1):
    def getDescription(self):
        desc = test338j1.getDescription(self) # Superclass call
        return "Foo_" + desc


