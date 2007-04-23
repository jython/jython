'''
Checks that files are closed in three situations:
1. Garbage collection/finalization close
2. Regular close
3. Shutdown time, close out open PyFiles
'''

import os
import support

from java.io import File
from java.lang import System, Thread

def check(fn='test.txt'):
    f = File(fn)
    if not f.exists():
        raise support.TestError('"%s" should exist' % fn)
    if not f.length():
        raise support.TestError('"%s" should have contents' % fn)
    os.remove(fn)


open("garbagecollected", "w").write("test")

#Wait up to 2 seconds for garbage collected to disappear
System.gc()
for i in range(10):
    if not os.path.exists('garbagecollected'):
        break
    Thread.sleep(200)

check("garbagecollected")

f = open("normalclose", "w")
f.write("test")
f.close()
check("normalclose")

#test397m writes to "shutdown" and exits
support.runJython('test397m.py')
check('shutdown')
