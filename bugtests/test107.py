"""
Check some internal frame info
"""
import sys
from types import ClassType

def getinfo():
    """ Returns a tuple consisting of:
        the name of the current module
        the name of the current class or None
        the name of the current function
        the current line number
    """
    try:
        1/0
    except:
        tb = sys.exc_info()[-1]
        frame = tb.tb_frame.f_back
        modulename = frame.f_globals['__name__']
        funcname = frame.f_code.co_name
        lineno = frame.f_lineno

        if len(frame.f_code.co_varnames) == 0:
            classname = None
        else:
            self = frame.f_locals[frame.f_code.co_varnames[0]]
            myclass = self.__class__
            if type(myclass) == ClassType:
               classname = myclass.__name__
            else:
                classname = None

        return modulename, classname, funcname, lineno

def foo():
    x = 99
    g = getinfo()
    assert (g[0] == "__main__" or g[0] == "test107")
    assert (g[1] == None)
    assert (g[2] == "foo")
        
class Bar:
    def baz(self):
        g = getinfo()
        assert (g[0] == "__main__" or g[0] == "test107")
        assert (g[1] == "Bar")
        assert (g[2] == "baz")
        
g = getinfo()
assert (g[0] == "__main__" or g[0] == "test107")
assert (g[1] == None)
assert (g[2] == "<module>")

foo()
Bar().baz()
