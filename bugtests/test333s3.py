
import sys

def myfunc():
    print "myfunc"

sys.exitfunc = myfunc
raise "Exc"
