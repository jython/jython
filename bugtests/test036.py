"""
Check that IOException isnøt mangled into an IOError
"""

import support
import java

try:
   x = java.io.OutputStreamWriter(java.lang.System.out, "garbage")
except java.io.UnsupportedEncodingException:
   pass
else:
   raise support.TestError("Should have raised java.io.UnsupportedEncodingException")
