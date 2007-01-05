'''
From bug #1284344
import a compiled module moved to a new location and check that its __file__
matches its new spot.
'''

fname = 'test392m.py'

open(fname, 'w').close()#touch!

import test392m
del test392m
import os
compiledName = 'test392m$py.class'
os.rename(compiledName, 'test392LibDir/%s' % compiledName)
os.remove(fname)

import support
ret = support.runJython('test392importer.py', expectError=True)
if ret == 1:
    raise support.TestError, '__file__ on test392m reflected where it was compiled, not where it was imported.'
elif ret != 0:
    raise support.TestError, 'running test392importer.py exited with an unexpected code'

