"""
Check reload of module.
"""

import support

src = """
def fun2():
    return %s
"""

def mk(v):
    f = open("test125m.py", "w")
    f.write(src % v)
    f.close()


mk("1")

import test125m
from test125m import fun2
support.compare(fun2(), "1")

import time
time.sleep(2)

mk("2")

reload(test125m)

support.compare(fun2(), "1")

from test125m import fun2

support.compare(fun2(), "2")

support.compare(test125m.fun2(), "2")

