
import support

try:
   from test233s import foobar
except ImportError:
   pass
else:
   raise support.TestError("Should raise ImportError")
