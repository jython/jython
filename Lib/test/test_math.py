# Python test set -- math module

from test_support import *

seps='1e-05'
eps = eval(seps)
print_test('math (test_math.py)', 1) # 'math module, testing with eps', seps
import math

def testit(name, value, expected):
	if abs(value-expected) > eps:
		raise TestFailed, '%s returned %f, expected %f'%\
		      (name, value, expected)

print_test('constants: e, pi', 2)
assert abs(math.pi-3.1415926) < eps
assert abs(math.e-2.7182818) < eps

print_test('acos')
testit('acos', math.acos(-1), math.pi)
testit('acos', math.acos(0), math.pi/2)
testit('acos', math.acos(1), 0)

print_test('asin')
testit('asin(-1)', math.asin(-1), -math.pi/2)
testit('asin(0)', math.asin(0), 0)
testit('asin(1)', math.asin(1), math.pi/2)

print_test('atan')
testit('atan(-1)', math.atan(-1), -math.pi/4)
testit('atan(0)', math.atan(0), 0)
testit('atan(1)', math.atan(1), math.pi/4)

print_test('atan2')
testit('atan2(-1, 0)', math.atan2(-1, 0), -math.pi/2)
testit('atan2(-1, 1)', math.atan2(-1, 1), -math.pi/4)
testit('atan2(0, 1)', math.atan2(0, 1), 0)
testit('atan2(1, 1)', math.atan2(1, 1), math.pi/4)
testit('atan2(1, 0)', math.atan2(1, 0), math.pi/2)

print_test('ceil')
testit('ceil(0.5)', math.ceil(0.5), 1)
testit('ceil(1.0)', math.ceil(1.0), 1)
testit('ceil(1.5)', math.ceil(1.5), 2)
testit('ceil(-0.5)', math.ceil(-0.5), 0)
testit('ceil(-1.0)', math.ceil(-1.0), -1)
testit('ceil(-1.5)', math.ceil(-1.5), -1)

print_test('cos')
testit('cos(-pi/2)', math.cos(-math.pi/2), 0)
testit('cos(0)', math.cos(0), 1)
testit('cos(pi/2)', math.cos(math.pi/2), 0)
testit('cos(pi)', math.cos(math.pi), -1)

print_test('cosh')
testit('cosh(0)', math.cosh(0), 1)
testit('cosh(2)-2*cosh(1)**2', math.cosh(2)-2*math.cosh(1)**2, -1) # Thanks to Lambert

print_test('exp')
testit('exp(-1)', math.exp(-1), 1/math.e)
testit('exp(0)', math.exp(0), 1)
testit('exp(1)', math.exp(1), math.e)

print_test('fabs')
testit('fabs(-1)', math.fabs(-1), 1)
testit('fabs(0)', math.fabs(0), 0)
testit('fabs(1)', math.fabs(1), 1)

print_test('floor')
testit('floor(0.5)', math.floor(0.5), 0)
testit('floor(1.0)', math.floor(1.0), 1)
testit('floor(1.5)', math.floor(1.5), 1)
testit('floor(-0.5)', math.floor(-0.5), -1)
testit('floor(-1.0)', math.floor(-1.0), -1)
testit('floor(-1.5)', math.floor(-1.5), -2)

print_test('fmod')
testit('fmod(10,1)', math.fmod(10,1), 0)
testit('fmod(10,0.5)', math.fmod(10,0.5), 0)
testit('fmod(10,1.5)', math.fmod(10,1.5), 1)
testit('fmod(-10,1)', math.fmod(-10,1), 0)
testit('fmod(-10,0.5)', math.fmod(-10,0.5), 0)
testit('fmod(-10,1.5)', math.fmod(-10,1.5), -1)

print_test('frexp')
def testfrexp(name, (mant, exp), (emant, eexp)):
	if abs(mant-emant) > eps or exp <> eexp:
		raise TestFailed, '%s returned %s, expected %s'%\
		      (name, `mant, exp`, `emant,eexp`)

testfrexp('frexp(-1)', math.frexp(-1), (-0.5, 1))
testfrexp('frexp(0)', math.frexp(0), (0, 0))
testfrexp('frexp(1)', math.frexp(1), (0.5, 1))
testfrexp('frexp(2)', math.frexp(2), (0.5, 2))

print_test('hypot')
testit('hypot(0,0)', math.hypot(0,0), 0)
testit('hypot(3,4)', math.hypot(3,4), 5)

print_test('ldexp')
testit('ldexp(0,1)', math.ldexp(0,1), 0)
testit('ldexp(1,1)', math.ldexp(1,1), 2)
testit('ldexp(1,-1)', math.ldexp(1,-1), 0.5)
testit('ldexp(-1,1)', math.ldexp(-1,1), -2)

print_test('log')
testit('log(1/e)', math.log(1/math.e), -1)
testit('log(1)', math.log(1), 0)
testit('log(e)', math.log(math.e), 1)

print_test('log10')
testit('log10(0.1)', math.log10(0.1), -1)
testit('log10(1)', math.log10(1), 0)
testit('log10(10)', math.log10(10), 1)

print_test('modf')
def testmodf(name, (v1, v2), (e1, e2)):
	if abs(v1-e1) > eps or abs(v2-e2):
		raise TestFailed, '%s returned %s, expected %s'%\
		      (name, `v1,v2`, `e1,e2`)

testmodf('modf(1.5)', math.modf(1.5), (0.5, 1.0))
testmodf('modf(-1.5)', math.modf(-1.5), (-0.5, -1.0))

print_test('pow')
testit('pow(0,1)', math.pow(0,1), 0)
testit('pow(1,0)', math.pow(1,0), 1)
testit('pow(2,1)', math.pow(2,1), 2)
testit('pow(2,-1)', math.pow(2,-1), 0.5)

print_test('sin')
testit('sin(0)', math.sin(0), 0)
testit('sin(pi/2)', math.sin(math.pi/2), 1)
testit('sin(-pi/2)', math.sin(-math.pi/2), -1)

print_test('sinh')
testit('sinh(0)', math.sinh(0), 0)
testit('sinh(1)**2-cosh(1)**2', math.sinh(1)**2-math.cosh(1)**2, -1)
testit('sinh(1)+sinh(-1)', math.sinh(1)+math.sinh(-1), 0)

print_test('sqrt')
testit('sqrt(0)', math.sqrt(0), 0)
testit('sqrt(1)', math.sqrt(1), 1)
testit('sqrt(4)', math.sqrt(4), 2)

print_test('tan')
testit('tan(0)', math.tan(0), 0)
testit('tan(pi/4)', math.tan(math.pi/4), 1)
testit('tan(-pi/4)', math.tan(-math.pi/4), -1)

print_test('tanh')
testit('tanh(0)', math.tanh(0), 0)
testit('tanh(1)+tanh(-1)', math.tanh(1)+math.tanh(-1), 0)
