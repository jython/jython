# Python test set -- part 4a, built-in functions a-m

from test_support import *

print_test('__import__', 2)
__import__('sys')
__import__('math')
__import__('string')
try: __import__('spamspam')
except ImportError: pass
else: raise TestFailed, "__import__('spamspam') should fail"

print_test('abs')
assert abs(0) == 0
assert abs(1234) == 1234
assert abs(-1234) == 1234

assert abs(0.0) == 0.0
assert abs(3.14) == 3.14
assert abs(-3.14) == 3.14

assert abs(0L) == 0L
assert abs(1234L) == 1234L
assert abs(-1234L) == 1234L

print_test('apply')
def f0(*args):
	assert args == ()

def f1(a1):
	assert a1 == 1

def f2(a1, a2):
	assert a1 == 1 and a2 == 2

def f3(a1, a2, a3):
	assert a1 == 1 and a2 == 2 and a3 == 3

apply(f0, ())
apply(f1, (1,))
apply(f2, (1, 2))
apply(f3, (1, 2, 3))

print_test('callable')

assert callable(len)

def f(): pass
assert callable(f)

class C:
	def meth(self): pass
assert callable(C)

x = C()
assert callable(x.meth)
assert not callable(x)

class D(C):
	def __call__(self): pass
y = D()
assert callable(y)

print_test('chr')
assert chr(32) == ' '
assert chr(65) == 'A'
assert chr(97) == 'a'

print_test('cmp')
assert cmp(-1, 1) == -1
assert cmp(1, -1) == 1
assert cmp(1, 1) == 0

print_test('coerce')
assert fcmp(coerce(1, 1.1), (1.0, 1.1)) == 0
assert coerce(1, 1L) == (1L, 1L)
assert fcmp(coerce(1L, 1.1), (1.0, 1.1)) == 0

print_test('compile')
compile('print 1\n', '', 'exec')

print_test('complex')
if complex(1,10) <> 1+10j: raise TestFailed, 'complex(1,10)'
if complex(1,10L) <> 1+10j: raise TestFailed, 'complex(1,10L)'
if complex(1,10.0) <> 1+10j: raise TestFailed, 'complex(1,10.0)'
if complex(1L,10) <> 1+10j: raise TestFailed, 'complex(1L,10)'
if complex(1L,10L) <> 1+10j: raise TestFailed, 'complex(1L,10L)'
if complex(1L,10.0) <> 1+10j: raise TestFailed, 'complex(1L,10.0)'
if complex(1.0,10) <> 1+10j: raise TestFailed, 'complex(1.0,10)'
if complex(1.0,10L) <> 1+10j: raise TestFailed, 'complex(1.0,10L)'
if complex(1.0,10.0) <> 1+10j: raise TestFailed, 'complex(1.0,10.0)'
if complex(3.14+0j) <> 3.14+0j: raise TestFailed, 'complex(3.14)'
if complex(3.14) <> 3.14+0j: raise TestFailed, 'complex(3.14)'
if complex(314) <> 314.0+0j: raise TestFailed, 'complex(314)'
if complex(314L) <> 314.0+0j: raise TestFailed, 'complex(314L)'
if complex(3.14+0j, 0j) <> 3.14+0j: raise TestFailed, 'complex(3.14, 0j)'
if complex(3.14, 0.0) <> 3.14+0j: raise TestFailed, 'complex(3.14, 0.0)'
if complex(314, 0) <> 314.0+0j: raise TestFailed, 'complex(314, 0)'
if complex(314L, 0L) <> 314.0+0j: raise TestFailed, 'complex(314L, 0L)'
if complex(0j, 3.14j) <> -3.14+0j: raise TestFailed, 'complex(0j, 3.14j)'
if complex(0.0, 3.14j) <> -3.14+0j: raise TestFailed, 'complex(0.0, 3.14j)'
if complex(0j, 3.14) <> 3.14j: raise TestFailed, 'complex(0j, 3.14)'
if complex(0.0, 3.14) <> 3.14j: raise TestFailed, 'complex(0.0, 3.14)'
class Z:
    def __complex__(self): return 3.14j
z = Z()
if complex(z) <> 3.14j: raise TestFailed, 'complex(classinstance)'

print_test('delattr')
import sys
sys.spam = 1
delattr(sys, 'spam')

print_test('dir')
x = 1
assert 'x' in dir()

import sys
assert 'modules' in dir(sys)

print_test('divmod')
if divmod(12, 7) <> (1, 5): raise TestFailed, 'divmod(12, 7)'
if divmod(-12, 7) <> (-2, 2): raise TestFailed, 'divmod(-12, 7)'
if divmod(12, -7) <> (-2, -2): raise TestFailed, 'divmod(12, -7)'
if divmod(-12, -7) <> (1, -5): raise TestFailed, 'divmod(-12, -7)'
#
if divmod(12L, 7L) <> (1L, 5L): raise TestFailed, 'divmod(12L, 7L)'
if divmod(-12L, 7L) <> (-2L, 2L): raise TestFailed, 'divmod(-12L, 7L)'
if divmod(12L, -7L) <> (-2L, -2L): raise TestFailed, 'divmod(12L, -7L)'
if divmod(-12L, -7L) <> (1L, -5L): raise TestFailed, 'divmod(-12L, -7L)'
#
if divmod(12, 7L) <> (1, 5L): raise TestFailed, 'divmod(12, 7L)'
if divmod(-12, 7L) <> (-2, 2L): raise TestFailed, 'divmod(-12, 7L)'
if divmod(12L, -7) <> (-2L, -2): raise TestFailed, 'divmod(12L, -7)'
if divmod(-12L, -7) <> (1L, -5): raise TestFailed, 'divmod(-12L, -7)'
#
if fcmp(divmod(3.25, 1.0), (3.0, 0.25)):
	raise TestFailed, 'divmod(3.25, 1.0)'
if fcmp(divmod(-3.25, 1.0), (-4.0, 0.75)):
	raise TestFailed, 'divmod(-3.25, 1.0)'
if fcmp(divmod(3.25, -1.0), (-4.0, -0.75)):
	raise TestFailed, 'divmod(3.25, -1.0)'
if fcmp(divmod(-3.25, -1.0), (3.0, -0.25)):
	raise TestFailed, 'divmod(-3.25, -1.0)'

print_test('eval')
assert eval('1+1') == 2
assert eval(' 1+1\n') == 2

globals = {'a': 1, 'b': 2}
locals = {'b': 200, 'c': 300}
if eval('a', globals) <> 1: raise TestFailed, "eval(1)"
if eval('a', globals, locals) <> 1: raise TestFailed, "eval(2)"
if eval('b', globals, locals) <> 200: raise TestFailed, "eval(3)"
if eval('c', globals, locals) <> 300: raise TestFailed, "eval(4)"

print_test('execfile')
z = 0
f = open(TESTFN, 'w')
f.write('z = z+1\n')
f.write('z = z*2\n')
f.close()
execfile(TESTFN)
if z <> 2: raise TestFailed, "execfile(1)"
globals['z'] = 0
execfile(TESTFN, globals)
if globals['z'] <> 2: raise TestFailed, "execfile(1)"
locals['z'] = 0
execfile(TESTFN, globals, locals)
if locals['z'] <> 2: raise TestFailed, "execfile(1)"
unlink(TESTFN)

print_test('filter')
if filter(lambda c: 'a' <= c <= 'z', 'Hello World') <> 'elloorld':
	raise TestFailed, 'filter (filter a string)'
if filter(None, [1, 'hello', [], [3], '', None, 9, 0]) <> [1, 'hello', [3], 9]:
	raise TestFailed, 'filter (remove false values)'
if filter(lambda x: x > 0, [1, -3, 9, 0, 2]) <> [1, 9, 2]:
	raise TestFailed, 'filter (keep positives)'
class Squares:
	def __init__(self, max):
		self.max = max
		self.sofar = []
	def __len__(self): return len(self.sofar)
	def __getitem__(self, i):
		if not 0 <= i < self.max: raise IndexError
		n = len(self.sofar)
		while n <= i:
			self.sofar.append(n*n)
			n = n+1
		return self.sofar[i]
if filter(None, Squares(10)) != [1, 4, 9, 16, 25, 36, 49, 64, 81]:
	raise TestFailed, 'filter(None, Squares(10))'
if filter(lambda x: x%2, Squares(10)) != [1, 9, 25, 49, 81]:
	raise TestFailed, 'filter(oddp, Squares(10))'

print_test('float')
if float(3.14) <> 3.14: raise TestFailed, 'float(3.14)'
if float(314) <> 314.0: raise TestFailed, 'float(314)'
if float(314L) <> 314.0: raise TestFailed, 'float(314L)'

print_test('getattr')
import sys
if getattr(sys, 'stdout') is not sys.stdout: raise TestFailed, 'getattr'

print_test('hasattr')
import sys
if not hasattr(sys, 'stdout'): raise TestFailed, 'hasattr'

print_test('hash')
hash(None)
if not hash(1) == hash(1L) == hash(1.0): raise TestFailed, 'numeric hash()'
hash('spam')
hash((0,1,2,3))
def f(): pass

print_test('hex')
if hex(16) != '0x10': raise TestFailed, 'hex(16)'
if hex(16L) != '0x10L': raise TestFailed, 'hex(16L)'
if len(hex(-1)) != len(hex(sys.maxint)): raise TestFailed, 'len(hex(-1))'
if hex(-16) not in ('0xfffffff0', '0xfffffffffffffff0'):
    raise TestFailed, 'hex(-16)'
if hex(-16L) != '-0x10L': raise TestFailed, 'hex(-16L)'

print_test('id')
assert id(None) == id(None)
id(1)
id(1L)
id(1.0)
id('spam')
id((0,1,2,3))
id([0,1,2,3])
id({'spam': 1, 'eggs': 2, 'ham': 3})

# Test input() later, together with raw_input

print_test('int')
if int(314) <> 314: raise TestFailed, 'int(314)'
if int(3.14) <> 3: raise TestFailed, 'int(3.14)'
if int(314L) <> 314: raise TestFailed, 'int(314L)'
# Check that conversion from float truncates towards zero
if int(-3.14) <> -3: raise TestFailed, 'int(-3.14)'
if int(3.9) <> 3: raise TestFailed, 'int(3.9)'
if int(-3.9) <> -3: raise TestFailed, 'int(-3.9)'
if int(3.5) <> 3: raise TestFailed, 'int(3.5)'
if int(-3.5) <> -3: raise TestFailed, 'int(-3.5)'

print_test('isinstance')
class C:
    pass
class D(C):
    pass
class E:
    pass
c = C()
d = D()
e = E()
if not isinstance(c, C): raise TestFailed, 'isinstance(c, C)'
if not isinstance(d, C): raise TestFailed, 'isinstance(d, C)'
if isinstance(e, C): raise TestFailed, 'isinstance(e, C)'
if isinstance(c, D): raise TestFailed, 'isinstance(c, D)'
if isinstance('foo', E): raise TestFailed, 'isinstance("Foo", E)'
try:
    isinstance(E, 'foo')
    raise TestFailed, 'isinstance(E, "foo")'
except TypeError:
    pass

print_test('issubclass')
if not issubclass(D, C): raise TestFailed, 'issubclass(D, C)'
if not issubclass(C, C): raise TestFailed, 'issubclass(C, C)'
if issubclass(C, D): raise TestFailed, 'issubclass(C, D)'
try:
    issubclass('foo', E)
    raise TestFailed, 'issubclass("foo", E)'
except TypeError:
    pass
try:
    issubclass(E, 'foo')
    raise TestFailed, 'issubclass(E, "foo")'
except TypeError:
    pass

print_test('len')
if len('123') <> 3: raise TestFailed, 'len(\'123\')'
if len(()) <> 0: raise TestFailed, 'len(())'
if len((1, 2, 3, 4)) <> 4: raise TestFailed, 'len((1, 2, 3, 4))'
if len([1, 2, 3, 4]) <> 4: raise TestFailed, 'len([1, 2, 3, 4])'
if len({}) <> 0: raise TestFailed, 'len({})'
if len({'a':1, 'b': 2}) <> 2: raise TestFailed, 'len({\'a\':1, \'b\': 2})'

print_test('long')
if long(314) <> 314L: raise TestFailed, 'long(314)'
if long(3.14) <> 3L: raise TestFailed, 'long(3.14)'
if long(314L) <> 314L: raise TestFailed, 'long(314L)'
# Check that conversion from float truncates towards zero
if long(-3.14) <> -3L: raise TestFailed, 'long(-3.14)'
if long(3.9) <> 3L: raise TestFailed, 'long(3.9)'
if long(-3.9) <> -3L: raise TestFailed, 'long(-3.9)'
if long(3.5) <> 3L: raise TestFailed, 'long(3.5)'
if long(-3.5) <> -3L: raise TestFailed, 'long(-3.5)'

print_test('map')
if map(None, 'hello world') <> ['h','e','l','l','o',' ','w','o','r','l','d']:
	raise TestFailed, 'map(None, \'hello world\')'
if map(None, 'abcd', 'efg') <> \
	  [('a', 'e'), ('b', 'f'), ('c', 'g'), ('d', None)]:
	raise TestFailed, 'map(None, \'abcd\', \'efg\')'
if map(None, range(10)) <> [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]:
	raise TestFailed, 'map(None, range(10))'
if map(lambda x: x*x, range(1,4)) <> [1, 4, 9]:
	raise TestFailed, 'map(lambda x: x*x, range(1,4))'
try:
	from math import sqrt
except ImportError:
	def sqrt(x):
		return pow(x, 0.5)
if map(lambda x: map(sqrt,x), [[16, 4], [81, 9]]) <> [[4.0, 2.0], [9.0, 3.0]]:
	raise TestFailed, 'map(lambda x: map(sqrt,x), [[16, 4], [81, 9]])'
if map(lambda x, y: x+y, [1,3,2], [9,1,4]) <> [10, 4, 6]:
	raise TestFailed, 'map(lambda x,y: x+y, [1,3,2], [9,1,4])'
def plus(*v):
	accu = 0
	for i in v: accu = accu + i
	return accu
if map(plus, [1, 3, 7]) <> [1, 3, 7]:
	raise TestFailed, 'map(plus, [1, 3, 7])'
if map(plus, [1, 3, 7], [4, 9, 2]) <> [1+4, 3+9, 7+2]:
	raise TestFailed, 'map(plus, [1, 3, 7], [4, 9, 2])'
if map(plus, [1, 3, 7], [4, 9, 2], [1, 1, 0]) <> [1+4+1, 3+9+1, 7+2+0]:
	raise TestFailed, 'map(plus, [1, 3, 7], [4, 9, 2], [1, 1, 0])'
if map(None, Squares(10)) != [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]:
	raise TestFailed, 'map(None, Squares(10))'
if map(int, Squares(10)) != [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]:
	raise TestFailed, 'map(int, Squares(10))'
if map(None, Squares(3), Squares(2)) != [(0,0), (1,1), (4,None)]:
	raise TestFailed, 'map(None: x, Squares(3), Squares(2))'
	
lst = map(max, Squares(3), Squares(2)) 
assert lst == [0, 1, 4] or lst == [0, 1, None]

print_test('max')
if max('123123') <> '3': raise TestFailed, 'max(\'123123\')'
if max(1, 2, 3) <> 3: raise TestFailed, 'max(1, 2, 3)'
if max((1, 2, 3, 1, 2, 3)) <> 3: raise TestFailed, 'max((1, 2, 3, 1, 2, 3))'
if max([1, 2, 3, 1, 2, 3]) <> 3: raise TestFailed, 'max([1, 2, 3, 1, 2, 3])'
#
if max(1, 2L, 3.0) <> 3.0: raise TestFailed, 'max(1, 2L, 3.0)'
if max(1L, 2.0, 3) <> 3: raise TestFailed, 'max(1L, 2.0, 3)'
if max(1.0, 2, 3L) <> 3L: raise TestFailed, 'max(1.0, 2, 3L)'

print_test('min')
if min('123123') <> '1': raise TestFailed, 'min(\'123123\')'
if min(1, 2, 3) <> 1: raise TestFailed, 'min(1, 2, 3)'
if min((1, 2, 3, 1, 2, 3)) <> 1: raise TestFailed, 'min((1, 2, 3, 1, 2, 3))'
if min([1, 2, 3, 1, 2, 3]) <> 1: raise TestFailed, 'min([1, 2, 3, 1, 2, 3])'
#
if min(1, 2L, 3.0) <> 1: raise TestFailed, 'min(1, 2L, 3.0)'
if min(1L, 2.0, 3) <> 1L: raise TestFailed, 'min(1L, 2.0, 3)'
if min(1.0, 2, 3L) <> 1.0: raise TestFailed, 'min(1.0, 2, 3L)'
