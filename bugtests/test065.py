"""
Can't override java.lang.Object methods when subclassing an interface
"""

import support

import java
class Foo(java.lang.Runnable):
    def run(self): pass
    def toString(self): return "Foo!!!"

foo = Foo()
s = java.lang.String.valueOf(foo)

if s != "Foo!!!":
    raise support.TestError("toString not overridden in interface")
