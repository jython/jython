"""
namespace (PyStringMap) deletion-confused insert bug
"""

#==============================================
# we need some stuff to find a pair of keys
# with the same initial index in hash table


from java.lang.System import identityHashCode
from java.lang import String


def hashCode(key):
    return identityHashCode(String.intern(key))


def scanKeys(base, r, sz, deep):
    for i in xrange(65,91): #chars 'A'-'Z'
        key = base+chr(i)
        #sz is hash table size
        if hashCode(key)%sz == r:
            break
        if deep:
            key = scanKeys(base, r, sz, deep-1)
            if key is not None:
                break
    return key


# find a key with the same hash index as key1
def findPairKey(key1, sz=7):
    #in empty PyStringMap hash table has size 7
    r=hashCode(key1)%sz
    base=""
    for deep in xrange(0,15):
        key = scanKeys(base, r, sz, deep)
        if key is not None: return key


        
class AA: pass
d = AA().__dict__


# now d is an empty PyStringMap dict


key1="key1"
#find a pair key for key1
key2 = findPairKey(key1)


# key2 consists of upper case characters (by construction)
# and always differs from key1
#print "key1=",repr(key1),"  key2=",repr(key2)


d[key2] = "foo"        #key2 occupies initial slot
d[key1] = "value1"     #key1 occupies next slot
del d[key2]            #initial slot is marked by "<deleted key>"
d[key1] = "value2"     #key1 replaces "<deleted key>" in the first
                       #slot but not old key1 value!
del d[key1]
#we hope key1 is not in the dict any more...
try:
    v=d[key1]
    #print "Oops! d[key1]=",repr(v)   #Oops! Magically ressurected!
    raise support.TestError,"namespace deletion-confused insert bug"
except KeyError:
    #print "OK"
    pass