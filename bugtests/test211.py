
import sys
import cPickle
pickle = cPickle

tmpfile = "test211.tmp"

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
	try:
		saveValue(x)
	except:
		print "Exception caught: cannot save ", x
	else:
		try:
			y = loadValue()
		except:
			print "Exception caught: cannot load previously saved value", x
		else:
			if x != y:
				print "saved: ", x,
				print "loaded: ", y
		
