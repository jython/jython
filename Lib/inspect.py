#
# Very simple version of inspect, just enough is supported for
# doctest to work.
#

import org.python.core as _core

def isclass(cls):
    return isinstance(cls, _core.PyClass)

def isfunction(func):
    return isinstance(func, _core.PyFunction)

def ismodule(mod):
    return isinstance(mod, _core.PyModule)

def ismethod(meth):
    return isinstance(meth, _core.PyMethod)

def classify_class_attrs(obj):
    return []


