
import sys

def myfunc():
    print "myfunc"

sys.exitfunc = myfunc

sys.exit(42)
