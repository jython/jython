"""
In bug #439688 the value 0x99 does not survive the JavaCC parser.
"""

import sys
print sys.defaultencoding
f = open("test302s.py", "wb")
f.write('v = "\x99"\n')
f.close()

import test302s

f = open("test302.out", "w")
f.write("\x99")
f.close();

from java.io import FileInputStream, InputStreamReader
readval = InputStreamReader(FileInputStream("test302.out"), 'ISO-8859-1').read()

print ord(test302s.v) == readval
