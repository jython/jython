"""
[ 577395 ] Outer finally not executed at return.
break/continue through finally.
"""
# Local name: bugtests/test371.py


import support

# Some glue to do all tests defined in this module,
# and fail only at end in finalTestReport()
totalTestFailures = 0
totalTests = 0

def testFail(mes):
    global totalTestFailures
    global totalTests
    print 'Fail:', mes
    totalTestFailures += 1
    totalTests += 1

def testPass(mes):
    global totalTests
    #print 'Ok:', mes
    totalTests += 1

def testEq(val, expected, mes):
    if val != expected:
	testFail('%s: expected %s, got %s' % (mes, repr(expected), repr(val)))
    else:
	testPass('%s: %s' % (mes, repr(val)))

def finalTestReport():
    global totalTestFailures
    global totalTests
    if totalTestFailures > 0:
	raise support.TestError('%d of %d test(s) failed in this module'
				% (totalTestFailures, totalTests))
    else:
        print 'All %d test(s) passed in this module.' % totalTests


retval = 'rql'


x = []
def tryfinallyreturn1(): 
    try:
	x.append(1) 
	return retval
    finally: 
	x.append(2)

r = tryfinallyreturn1()
testEq(x, [1,2], 'tryfinallyreturn1 side effect')
testEq(r, retval, 'tryfinallyreturn1 return value')

x = []
def tryfinallyreturn2(): # fails in jython 2.1, x == [1,2] afterwards 
    try:
	try: 
	    x.append(1)
	    return retval
	finally:
	    x.append(2) 
    finally:
	x.append(3) 

r = tryfinallyreturn2() 
testEq(x, [1,2,3], 'tryfinallyreturn2 side effect')
testEq(r, retval, 'tryfinallyreturn2 return value')

x = [] 
def tryfinallyreturn3(): # fails in jython 2.1, x == [1,2] afterwards
    try: 
	try:
	    try: 
		x.append(1)
		return retval
	    finally:
		x.append(2) 
	finally:
	    x.append(3) 
    finally:
	x.append(4) 

r = tryfinallyreturn3() 
testEq(x, [1,2,3,4], 'tryfinallyreturn3 side effect')
testEq(r, retval, 'tryfinallyreturn3 return value')


x = []
def tryfinallyraise1(): 
    try:
	x.append(1) 
	raise Exception
    finally: 
	x.append(2)

try:
    tryfinallyraise1() 
except Exception:
    testEq(x, [1,2], 'tryfinallyraise1 side effect')
else:
    testFail('tryfinallyraise1 did not trow Exception')


x = []
def tryfinallyraise2(): 
    try:
	try: 
	    x.append(1)
	    raise Exception
	finally:
	    x.append(2) 
    finally:
	x.append(3) 

try:
    tryfinallyraise2()
except Exception:
    testEq(x, [1,2,3], 'tryfinallyraise2 side effect')
else:
    testFail('tryfinallyraise2 did not trow Exception')

x = []
def tryfinallyraise3(): 
    try:
	try: 
	    try:
		x.append(1) 
		raise Exception
	    finally: 
		x.append(2)
	finally: 
	    x.append(3)
    finally: 
	x.append(4)

try:
    tryfinallyraise3() 
except Exception:
    testEq(x, [1,2,3,4], 'tryfinallyraise3 side effect')
else:
    testFail('tryfinallyraise3 did not trow Exception')


x = []
def fortryfinallycontinuereturn1(): 
    for i in range(3):
	try: 
	    x.append(2 * i)
	    if i == 0:
	        continue
	    return retval
	finally:
	    x.append(2 * i + 1) 

r = fortryfinallycontinuereturn1() 
testEq(x, [0,1,2,3], 'fortryfinallycontinuereturn1 side effect')
testEq(r, retval, 'fortryfinallycontinuereturn1 return value')

x = [] 
def fortryfinallycontinuereturn2():
    for i in range(3):
	try:
	    try: 
		x.append(3 * i)
		if i == 0:
		    continue
		return retval
	    finally:
		x.append(3 * i + 1) 
	finally:
	    x.append(3 * i + 2) 

r = fortryfinallycontinuereturn2() 
testEq(x, [0,1,2,3,4,5], 'fortryfinallycontinuereturn2 side effect')
testEq(r, retval, 'fortryfinallycontinuereturn2 return value')

x = [] 
def fortryfinallycontinuereturn3(): # fails in jython 2.1, x == [1,2] afterwards
    for i in range(3):
	try:
	    try: 
		try:
		    x.append(4 * i) 
		    if i == 0:
			continue
		    return retval
		finally: 
		    x.append(4 * i + 1)
	    finally: 
		x.append(4 * i + 2)
	finally: 
	    x.append(4 * i + 3)


r = fortryfinallycontinuereturn3() 
testEq(x, [0,1,2,3,4,5,6,7], 'fortryfinallycontinuereturn3 side effect')
testEq(r, retval, 'fortryfinallycontinuereturn3 return value')


x = []
def fortryfinallybreak1(): 
    for i in range(3):
	try: 
	    x.append(2 * i)
	    if i == 1:
	        break
	finally: 
	    x.append(2 * i + 1)
    return retval

r = fortryfinallybreak1() 
testEq(x, [0,1,2,3], 'fortryfinallybreak1 side effect')
testEq(r, retval, 'fortryfinallybreak1 return value')

x = [] 
def fortryfinallybreak2():
    for i in range(3):
	try:
	    try: 
		x.append(3 * i)
		if i == 1:
		    break
	    finally: 
		x.append(3 * i + 1)
	finally: 
	    x.append(3 * i + 2)
    return retval

r = fortryfinallybreak2() 
testEq(x, [0,1,2,3,4,5], 'fortryfinallybreak2 side effect')
testEq(r, retval, 'fortryfinallybreak2 return value')


x = []
def fortryfinallycontinueraise1(): 
    for i in range(3):
	try: 
	    x.append(2 * i)
	    if i == 0:
	        continue
	    raise Exception
	finally:
	    x.append(2 * i + 1) 

try:
    fortryfinallycontinueraise1()
except Exception:
    testEq(x, [0,1,2,3], 'fortryfinallycontinueraise1 side effect')
else:
    testFail('fortryfinallycontinueraise1 did not trow Exception')

x = []
def fortryfinallycontinueraise2():
    for i in range(3):
	try: 
	    try:
		x.append(3 * i) 
		if i == 0:
		    continue
		raise Exception
	    finally: 
		x.append(3 * i + 1)
	finally: 
	    x.append(3 * i + 2)

try:
    fortryfinallycontinueraise2() 
except Exception:
    testEq(x, [0,1,2,3,4,5], 'fortryfinallycontinueraise2 side effect')
else:
    testFail('fortryfinallycontinueraise2 did not trow Exception')


x = []
def tryfortrycontinueraise1(): 
    try:
	for i in range(3):
	    try:
		x.append(2 * i) 
		if i == 0:
		    continue
		raise Exception
	    finally: 
		x.append(2 * i + 1)
    finally: 
	x.append('last')

try:
    tryfortrycontinueraise1() 
except Exception:
    testEq(x, [0,1,2,3,'last'], 'tryfortrycontinueraise1 side effect')
else:
    testFail('tryfortrycontinueraise1 did not trow Exception')


x = []
def tryfortrybreak1(): 
    try:
	for i in range(3):
	    try:
		x.append(2 * i) 
		if i == 1:
		    break
	    finally:
		x.append(2 * i + 1) 
	return retval
    finally: 
	x.append('last')

r = tryfortrybreak1()
testEq(x, [0,1,2,3,'last'], 'tryfortrybreak1 side effect')
testEq(r, retval, 'tryfortrybreak1 return value')


x = [] 
def tryfortrycontinuereturn1():
    try:
	for i in range(3):
	    try: 
		x.append(2 * i)
		if i == 0:
		    continue
		return retval
	    finally:
		x.append(2 * i + 1) 
    finally:
	x.append('last') 

r = tryfortrycontinuereturn1() 
testEq(x, [0,1,2,3,'last'], 'tryfortryfinallyreturn1 side effect')
testEq(r, retval, 'tryfortrycontinuereturn1 return value')

finalTestReport()

