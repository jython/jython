"""
fixed broken id checks with pickle and copy that depends on id working correctly
"""

import support

d = {}

import java

clash_id = java.lang.System.identityHashCode

for i in xrange(100000):
  s = ['test',i]
  j = clash_id(s)
  if d.has_key(j):
    break
  d[j] = s

s1 = s
s0 = d[j]

data = [s0,s1,s0]

#print data

import pickle
import cPickle

def check(ctxt,data0,data1):
    if data0 != data1:
        raise support.TestError,"data corrupted in %s because of id clashes: %s != %s" % (ctxt.__name__,data0,data1)

def pik_test(pikmod,data):
    pik =pikmod.dumps(data,1)
    data1 = pikmod.loads(pik)
    check(pikmod,data,data1)

pik_test(cPickle,data)
pik_test(pickle,data)

import copy

check(copy.deepcopy,data,copy.deepcopy(data))
