"""
Make sure that ImportAll and ExecStmt can modify the locals
"""

import support


def f1():
    from stat import *
    v1 = ST_ATIME
    assert v1 == 7
    exec "foo=22"
    v2 = foo
    assert v2 == 22

f1()
