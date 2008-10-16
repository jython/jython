"""
Check stack frame locals.
"""

import sys


def vars():
    tb = sys.exc_info()[2]
    while tb.tb_next is not None:
        tb = tb.tb_next
    return tb.tb_frame.f_locals

def h():
    a = 1
    b = 2
    raise AttributeError("spam")

try:
    h()
except:
    assert(vars() == {'a':1, 'b':2 })

