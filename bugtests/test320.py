"""
[ #452526 ] traceback lineno is the except line
"""

import support
import sys, traceback

def test():
    print noname

def foo():
    try:


        test()


    except ValueError:
        print "shouldn't happen."

try:
    foo()
except:
    tb = sys.exc_info()[2]
    #print tb.tb_lineno
    #traceback.print_tb(tb)
    assert tb.tb_lineno == 15

