
import sys
import pickle
tmpfile = "test210.tmp"

try:
  import test118
except:
  pass

def saveValue( value):
	f = open(tmpfile,"wb")
	p = pickle.Pickler(f,1)
	p.dump(value)
	f.close()
	
def loadValue():
	f=open(tmpfile,"rb")
	retVal =  pickle.Unpickler(f).load()
	f.close()
	return retVal

for x in range(256):
	saveValue(x)
	y = loadValue()
	if x != y:
		print "saved: ", x,
		print "loaded: ", y
		
