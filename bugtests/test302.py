"""
In bug #439688 the value 0x99 does not survive the JavaCC parser.
"""

import support

f = open("test302s.py", "wb")
f.write('v = "\x99"\n')
f.close()

try:
    import test302s
except SyntaxError:
    raise support.TestError('Importing a file with str byte > 128 should not raise a Syntaxerror')

f = open("test302.out", "w")
f.write("\x99")
f.close();

from java.io import FileInputStream, InputStreamReader
readval = InputStreamReader(FileInputStream("test302.out"), 'ISO-8859-1').read()

if ord(test302s.v) != readval:
    raise support.TestError("Module source was not decoded correctly %x %x" % 
                (ord(test302s.v), readval))
