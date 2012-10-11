from org.junit.Assert import assertEquals
from org.junit import Test
from java.lang import (Object, Void, Long)
from java.lang import Exception as JavaException
import sys
import time

from org.python.compiler.custom_proxymaker import MiniClampMaker

sys.path.append('tests/python/custom_proxymaker/')
from clamp import ClampMethod

class JUnitTest(Object):
    __proxymaker__ = MiniClampMaker
    __java_package__ = 'custom_proxymaker.tests'


    def testAddition(self):
        assertEquals(4, 1 + 3)
    testAddition._clamp = ClampMethod('testAddition',Void.TYPE,[],[],{Test:None},[{}])

    def testJavaException(self):
        raise JavaException()
    testJavaException._clamp = ClampMethod('testJavaException', Void.TYPE, [], [JavaException], {Test:{'expected':JavaException}}, [{}])

    def testTimeout(self):
        time.sleep(0.1)
    testTimeout._clamp = ClampMethod('testTimeout', Void.TYPE, [], [], {Test:{'timeout':Long(1000)}}, [{}])
