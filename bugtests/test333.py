"""
[ #476772 ] shutdowns in jython / atexit
"""

import support
import os

def check(filename, result):
    f = open(filename)
    l = f.readlines()
    f.close()
    if l != result:
        raise support.TestError("Result was wrong: %s" % l)

# Different exit situations in the interpreter.

support.runJython("test333s1.py", output="test333s1.out")
check("test333s1.out", [ "myfunc\n" ])

ret = support.runJython("test333s2.py", output="test333s2.out", expectError=1)
if ret != 42:
    raise support.TestError("Return code was wrong: %d" % ret)
check("test333s2.out", [ "myfunc\n" ])

support.runJython("test333s3.py",
        output="test333s3.out", error="test333s3.err", expectError=1)
check("test333s3.out", [ "myfunc\n" ])
check("test333s3.err", [
    'Traceback (innermost last):\n',
    '  File "test333s3.py", line 8, in ?\n',
    'Exc\n',
])

# Different exit situations in compiled applications.

support.compileJPythonc("test333s1.py", output="test333s1.err")
support.runJava("test333s1", classpath="jpywork", output="test333s1.out")
check("test333s1.out", [ "myfunc\n" ])

support.compileJPythonc("test333s1.py", output="test333s3.err")
support.runJava("test333s1", classpath="jpywork", output="test333s1.out")
check("test333s1.out", [ "myfunc\n" ])

support.compileJPythonc("test333s3.py", output="test333s3.err")
support.runJava("test333s3", classpath="jpywork", output="test333s3.out",
                error="test333s3.err", expectError=1)
check("test333s3.out", [ "myfunc\n" ])
f = open("test333s3.err")
lines = f.readlines();
f.close()
if "Exc\n" not in lines:
    raise support.TestError("Should raise a 'Exc' exception")
