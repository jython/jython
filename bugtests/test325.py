"""
Verify a bug in pickle reported by mailling list.
"""

import support

# This program fails with pickle due to the id() problem.
#import pickle
import cPickle as pickle

pfile=open("test325.out","wb")
p=pickle.Pickler(pfile)
for l in range (1,10000):
    row=[str(l),str(l)]
    p.dump(row)
pfile.close()

#print "reading"
n=1
try:
    pfile=open("test325.out","rb")
    l=pickle.load(pfile)
    while l:
        comp = [str(n),str(n)]
        if l != comp:
            print "Pickle error"
            print str(l) + " should be " + str(comp)
            raise support.TestError("pickle is not working")
        n=n+1
        l=pickle.load(pfile)
    pfile.close()

except EOFError:
    #print "End reached, well done"
    pfile.close()

