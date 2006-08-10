'''
Checks that exceptions imported in import * will catch thrown subclass excetions
in an except statement.

Reported in bugs 1531644 and 1269872.
'''
import support
import sys

from java.net import Socket
from java.io import *

try:
    # Do a connection that will yield a ECONNREFUSED -> ConnectException.
    conn = Socket('localhost', 8342)
except IOException, e:
    pass
except:
    raise support.TestError, "A %s was raised which is an IOExcption but except IOException above didn't catch it" % sys.exc_info()[0]
