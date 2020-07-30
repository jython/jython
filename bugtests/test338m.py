#import test338j1

class test338m(test338j1):  # noqa: F821 # without import is ok (apparently)
    def getDescription(self):
        desc = test338j1.getDescription(self)  # noqa: F821 # Superclass call
        return "Foo_" + desc


