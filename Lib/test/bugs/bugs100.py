import sys
sys.path.append('c:\\jpython\\cpd\\jpython\\scripts')
from test_support import *

print_test('Bug Fixes', 0)
print_test('From 1.0.0 to 1.0.1', 1)
print_test('Recursive assignment to list slices handled incorrectly #1', 2)

x = [1,2,3,4,5]
x[1:] = x
assert x == [1,1,2,3,4,5]

print_test('sys.platform should be javax.y.z not jdkx.y.z #4')

import sys
assert sys.platform[:4] == 'java'

print_test('java.io.IOExceptions are mangled into IOErrors #5')

from java import io, lang
try:
	io.FileInputStream("doesnotexist")
	raise TestFailed
except io.FileNotFoundException:
	pass
	
try:
	io.FileInputStream("doesnotexist")
	raise TestFailed
except IOError:
	pass
	

print_test('java.util.Vector\'s can\'t be used in for loops #7')

from java.util import Vector

vec = Vector()
vec.addElement(1)
vec.addElement(10)
vec.addElement(100)

sum = 0
for x in vec:
	sum = sum+x
assert sum == 111

print_test('Exception tuple contains nulls #8')

str(Exception)

print_test('0.001 comes out as 0.0010 #11')

assert str(0.001) == '0.001'

print_test("thread.LockType doesn't exist #12")

import thread
assert hasattr(thread, 'LockType')

print_test('sys.exit can only be called with an integer argument #13')

import sys
try:
	sys.exit("goodbye")
except SystemExit, exc:
	assert exc == "goodbye"
	
print_test('"%012d" % -4 displays "0000000000-4" #15')

assert "%012d" % -4 == "-00000000004"


print_test('Indexing a string with starting slice larger than string length throws StringIndexOutOfBoundsException #19')

assert "a"[10:] == ""

print_test('Java exception thrown for non-keyword argument following keyword #20')

def foo(x,y=10): pass

try:
	exec("foo(y=20, 30)")
	raise TestFailed
except SyntaxError:
	pass
	
print_test('Java field names which conflict with Python reserved words are not renamed #23')

assert hasattr(lang.System, 'in_')

print_test('Bad input to __import__ raises a Java exception #27')

try:
	__import__("")
	raise TestFailed
except ImportError:
	pass
	
print_test('xrange implementation is broken for almost any complex case #29')

assert list(xrange(10)[9:1:-1]) == [9, 8, 7, 6, 5, 4, 3, 2]

print_test('Trying to assign to a method of a Java instance throws a NullPointerException #30')

from java.awt import Button
b = Button()
try:
	b.setLabel = 4
	raise TestFailed
except TypeError:
	pass
	

print_test('From 1.0.1 to 1.0.2', 1)
print_test('Recursive assignment to list slices handled incorrectly #1', 2)

from java.lang import Thread

 class TestThread(Thread):
         def run(self):
                 for i in range(1000):
                         exec("x=2+2")
                 print 'finished'

 testers = []
 for i in range(10):
         testers.append(TestThread())

 for tester in testers:
         tester.start()