def one(*args):
	pass
	
def two(args):
	print args

myArgs = ('foo','bar')
one(myArgs)
one(*myArgs)
#two(*myArgs)
