"""
Check some internal frame info
"""

import support


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
	support.compare(g[0], "(__main__|test107)")
	support.compare(g[1], "None")
	support.compare(g[2], "foo")
        
class Bar:
	def baz(self):
        	g = getinfo()
		support.compare(g[0], "(__main__|test107)")
		support.compare(g[1], "Bar")
		support.compare(g[2], "baz")
        
g = getinfo()
support.compare(g[0], "(__main__|test107)")
support.compare(g[1], "None")
support.compare(g[2], "<module>")

foo()
Bar().baz()
