"""

"""

import support


class A: 
    def __init__(self):
        self.foo = 'bar'
	
def f(*args, **kw): 
   #print args, kw
   support.compare(args, "(1, 2, 3)")
   support.compare(kw, "{'foo': 'bar'}")

apply(f, (1,2,3), A().__dict__)

