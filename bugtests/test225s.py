
import support 

def foo(a=1, b=2+4):
   return b

v = foo()

if v != 6:
   raise support.TestError, "Wrong return value %d" % d

