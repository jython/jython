""" A much simpler string module than that included in the standard
Python 1.5 distribution.  Relies on the presence of strop."""


# Some strings for ctype-style character classification
whitespace = ' \t\n\r\v\f'
lowercase = 'abcdefghijklmnopqrstuvwxyz'
uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
letters = lowercase + uppercase
digits = '0123456789'
hexdigits = digits + 'abcdef' + 'ABCDEF'
octdigits = '01234567'

#from strop import *
import sys
sys.add_package("org.python.core")
from org.python.core.PyString import *


def capwords(s, sep=None):
	if sep is None:
		return join(map(capitalize, split(s)))
	else:
		return join(map(capitalize, split(s, sep)), sep)
		
def maketrans(fromstr, tostr):
	raise NameError, 'maketrans not yet implemented in JPython'
	
def translate(s, table, deletions=""):
	raise NameError, 'translate not yet implemented in JPython'