"""
Test the cp1542 encoding (euro-centric console)
In bug #439688 the value 0x99 does not survive the JavaCC parser.
"""

import support

f = open("test302s.py", "wb")
f.write('v = "\x99"\n')
f.close()

try:
    import test302s
except SyntaxError:
    raise support.TestWarning('Should not raise a Syntaxerror')

f = open("test302.out", "wb")
f.write("\x99")
f.close();

import java
readerval = java.io.FileReader("test302.out").read()

if ord(test302s.v) != readerval:
    raise support.TestError("Module source was not decoded correctly %x %x" % 
                (ord(test302s.v), ord(readerval)))
