"""
return in finally clause causes java.lang.VerifyError at compile time
"""

import support

def timeit(f):
    t0 = time.clock()
    try:
        f()
    finally:
        t1 = time.clock()
        return t1 - t0


