"""

"""

import support

class A:
    def __init__(self):
        self.__stop("A")
        self.__x = 1
        self.__y = 1
        del self.__x

    def __stop(self, s):
        pass

    __c = 1


if dir(A) != ['_A__c', '_A__stop', '__doc__', '__init__', '__module__']:
    raise support.TestError("bug in private class var mangling %s" % dir(A))
if dir(A()) != ['_A__y']:
    raise support.TestError("bug in private var mangling %s" % dir(A()))

