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
#sys.add_package("org.python.core")
from org.python.core.PyString import *

splitfields = split
joinfields = join

def capwords(s, sep=None):
	if sep is None:
		return join(map(capitalize, split(s)))
	else:
		return join(map(capitalize, split(s, sep)), sep)
		
#maketrans and translate contributed by Lars Marius Garshold
def maketrans(fromstr, tostr):
    if len(fromstr)!=len(tostr):
        raise ValueError, "Arguments of different lengths!"

    table=[" "]*256
    for ix in range(256):
        table[ix]=chr(ix)

    for ix in range(len(fromstr)):
        table[ord(fromstr[ix])]=tostr[ix]

    return table
        
def translate(s, table, deletions=""):
    ns=""
    for char in s:
        if not char in deletions:
            ns=ns+table[ord(char)]

    return ns       