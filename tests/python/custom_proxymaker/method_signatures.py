from java.lang import (Class, Object, Void, String, Long)
from java.lang import RuntimeException

from org.python.compiler.custom_proxymaker import MiniClampMaker

import sys
sys.path.append('tests/python/custom_proxymaker/')
from clamp import ClampMethod

class MethodSignatures(Object):
    __proxymaker__ = MiniClampMaker
    __java_package__ = 'custom_proxymaker.tests'

    def throwsException(self):
        pass
    throwsException._clamp = ClampMethod('throwsException', Void.TYPE, [], [RuntimeException], {}, [{}])

    def returnsLong(self):
        return 2
    returnsLong._clamp = ClampMethod('returnsLong', Long.TYPE, [], [], {}, [{}])

    def returnsObject(self):
        return Object()
    returnsObject._clamp = ClampMethod('returnsObject', Object, [], [], {}, [{}])

    def returnsArray(self):
        return [1,2,3]
    returnsArray._clamp = ClampMethod('returnsArray', Class.forName('[J'), [], [], {}, [{}])

    def returnsArrayObj(self):
        return [1,2,3]
    returnsArrayObj._clamp = ClampMethod('returnsArrayObj', Class.forName('[Ljava.lang.Object;'), [], [], {}, [{}])

    def acceptsString(self, arg):
        pass
    acceptsString._clamp = ClampMethod('acceptsString', Void.TYPE, [String], [], {}, [{}])

    def acceptsArray(self, arg):
        pass
    acceptsArray._clamp = ClampMethod('acceptsArray', Void.TYPE, [Class.forName('[J')], [], {}, [{}])
    