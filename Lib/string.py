""" A much simpler string module than that included in the standard
Python 1.5 distribution.  Relies on the presence of strop."""

from strop import *

def capwords(s, sep=None):
	if sep is None:
		return join(map(capitalize, split(s)))
	else:
		return join(map(capitalize, split(s, sep)), sep)
		
def maketrans(fromstr, tostr):
	raise NameError, 'maketrans not yet implemented in JPython'
	
def translate(s, table, deletions=""):
	raise NameError, 'translate not yet implemented in JPython'