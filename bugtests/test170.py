"""

"""

import support

import cPickle, cStringIO

#print cPickle.__version__

import test170p.Stack

s = test170p.Stack.Stack()

#print s

cs = cStringIO.StringIO()

cPickle.dump(s, cs)
#print cs.getvalue()
cs.seek(0)
cPickle.load(cs)

#raise support.TestError("" + `x`)
