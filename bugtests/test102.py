"""
Check stack frame locals.
"""

import support


def vars():
    import sys
    tb = sys.exc_info()[2]
    while tb.tb_next is not None:
        tb = tb.tb_next
    return tb.tb_frame.getf_locals()


def h():
    a = 1
    b = 2
    raise AttributeError("spam")

try:
    h()
except:
    if vars() != {'a':1, 'b':2 }:
	raise support.TestError("Unexpected contents of stackframe locals %s" % vars())



