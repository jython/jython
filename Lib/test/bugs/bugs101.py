import sys
sys.path.append('c:\\jpython\\cpd\\jpython\\scripts')
from test_support import *

def test931():
	'exec and eval are not thread safe'
	
	from java.lang import Thread

	class TestThread(Thread):
	         def run(self):
	                 for i in range(30):
	                         exec("x=2+2")
	
	testers = []
	for i in range(10):
	         testers.append(TestThread())
	
	for tester in testers:
	         tester.start()
	   

	for tester in testers:
		tester.join()
		
code32 = """\
def foo():
	try:
		2+2
	finally:
		return 4
"""

def test32():
	'return in finally clause causes java.lang.VerifyError at compile time'
	exec code32

def test


print_test('Bug Fixes', 0)
print_test('From 1.0.1 to 1.0.2', 1)

items = locals().items()
items.sort()

errors = 0
for name, value in items:
	if name[:4] == 'test':
		print_test(value.__doc__+' #'+name[4:], 2)
		try:
			value()
		except:
			print 'Error!', sys.exc_type, sys.exc_value
			errors = errors+1

print errors, 'errors'