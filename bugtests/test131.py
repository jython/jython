"""
"""

import java 
import support

try:
  stream = java.io.FileInputStream("missing")
except java.io.IOException, ioexc:
  #support.compare(ioexc.message, "cannot find")
  # this message is not portable across underlying operating systems
  pass
else:
  raise support.TestError("Should raise a IOException")
