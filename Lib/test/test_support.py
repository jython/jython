# Python test set -- supporting definitions.

TestFailed = 'test_support -- test failed'	# Exception

verbose = 1				# Flag set to 0 by regrtest.py

def unload(name):
	import sys
	try:
		del sys.modules[name]
	except KeyError:
		pass

def forget(modname):
	unload(modname)
	import sys, os
	for dirname in sys.path:
		try:
			os.unlink(os.path.join(dirname, modname + '.pyc'))
		except os.error:
			pass

FUZZ = 1e-6

def fcmp(x, y): # fuzzy comparison function
	if type(x) == type(0.0) or type(y) == type(0.0):
		try:
			x, y = coerce(x, y)
			fuzz = (abs(x) + abs(y)) * FUZZ
			if abs(x-y) <= fuzz:
				return 0
		except:
			pass
	elif type(x) == type(y) and type(x) in (type(()), type([])):
		for i in range(min(len(x), len(y))):
			outcome = fcmp(x[i], y[i])
			if outcome <> 0:
				return outcome
		return cmp(len(x), len(y))
	return cmp(x, y)

TESTFN = '@test' # Filename used for testing
from os import unlink
import string

roman = ['i', 'ii', 'iii', 'iv', 'v', 'vi', 'vii', 'viii', 'ix', 'x', 'xi', 'xii']
symbols = [
	map(string.upper, roman),
	string.uppercase,
	range(1, 50),
	string.lowercase,
	roman,
	]
	
def symbol(n, level):
	return str(symbols[level][n-1])

levels = [0]*20
currentLevel = 0
def print_test(txt, level=-1):
	global currentLevel
	if level == -1:
		level = currentLevel
	else:
		n = currentLevel - level
		if n > 0:
			levels[level+1:level+n+1] = [0]*n
		currentLevel = level
	levels[level] = levels[level]+1

	number = "  "*(level)+symbol(levels[level], level)+'.'

	#number = string.join(map(str, levels[:level]), '.')
	print number, txt
	
	return level
	
oldStdout = None

import sys
from StringIO import StringIO
def beginCapture():
	global oldStdout
	
	if oldStdout is not None:
		raise TestError, "Internal error"
		
	oldStdout = sys.stdout
	sys.stdout = StringIO()
	
def endCapture():
	global oldStdout
	
	txt = sys.stdout.getvalue()
	sys.stdout = oldStdout
	oldStdout = None
	
	return txt