"""
[ #494514 ] Python object not gc()'d
"""

import support
import java, time, sys, cStringIO

class A:
    def __del__(self):
        raise KeyError, "dummy"

try:
    sys.stderr = cStringIO.StringIO()
    A()

    java.lang.System.gc()
    time.sleep(2)
finally:
    v = sys.stderr.getvalue()
    sys.stderr = sys.__stderr__

    support.compare(v, "Exception KeyError: .* ignored")

    
