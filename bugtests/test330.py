"""
[ #477793 ] os.utime() is missing
"""

import support

import os
f = open("test330.out", "w")
f.close()

m = os.stat("test330.out")[8]
os.utime("test330.out", (0, 0))
if os.stat("test330.out")[8] != 0:
    raise support.TestWarning("Modification time not changed #1")

os.utime("test330.out", (m, m))
if os.stat("test330.out")[8] != m:
    raise support.TestError("Modification time not changed #2")

