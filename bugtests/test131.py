"""

"""

import support

import java 

try:
        stream = java.io.FileInputStream("missing")
except java.io.IOException, ioexc:
        support.compare(ioexc.message, "cannot find")
else:
	raise support.TestError("Should raise a IOException")

