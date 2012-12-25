from java.lang import (Void, String, Integer, Long)
from javax.swing import BoxLayout
from java.awt import Container

from org.python.compiler.custom_proxymaker import MiniClampMaker

import sys
sys.path.append('tests/python/custom_proxymaker/')
from clamp import ClampMethod

class ConstructorSignatures(BoxLayout):
    __proxymaker__ = MiniClampMaker
    __java_package__ = 'custom_proxymaker.tests'

#    def __init__(self, val):
#        super(ConstructorSignatures, self).__init__(Container(), BoxLayout.X_AXIS)
#        print val

    def __jinit__(self, one, two):
 #       super(ConstructorSignatures, self).__init__(Container(), BoxLayout.X_AXIS)
        print one, two

    __jinit__._clamp = ClampMethod('__init__', Void.TYPE, [Container, Integer.TYPE], [], {}, [{}])

    def test(self):
        return 1
    test._clamp = ClampMethod('test', Long.TYPE, [], [], {}, [{}])

    def toString(self):
        return self.__class__.__name__
    toString._clamp = ClampMethod('toString', String, [], [], {}, [{}])

