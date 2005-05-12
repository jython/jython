# CAU: adaptation of the cpython 2.2 test_array.py for jython 2.2
#      combined with the jarray test from 2.1

from test_support import *


print_test('jarray module (test_jarray.py)', 1)

from jarray import array, zeros
import sys

print_test('array', 2)
from java import awt
hsb = awt.Color.RGBtoHSB(0,255,255, None)
#print hsb
assert hsb == array([0.5,1,1], 'f')

rgb = apply(awt.Color.HSBtoRGB, tuple(hsb))
#print hex(rgb)
assert rgb == 0xff00ffff

print_test('zeros', 2)
hsb1 = zeros(3, 'f')
awt.Color.RGBtoHSB(0,255,255, hsb1)
#print hsb, hsb1
assert hsb == hsb1

def main():
    testtype('c', 'c')

    for type in (['b', 'h', 'i', 'l', 'f', 'd']):
        testtype(type, 1)
      
    #test a mix of known success and failure cases
    init_tests();
    extend_tests();
    fromlist_tests();

    unlink(TESTFN)


def testoverflow(type, lowerLimit, upperLimit):
        # should not overflow assigning lower limit
    if verbose:
        print "overflow test: array(%s, [%s])" % (`lowerLimit`, `type`)
    try:
        a = array([lowerLimit], type)
    except:
        raise TestFailed, "array(%s) overflowed assigning %s" %\
                (`lowerLimit`, `type`)
    # should overflow assigning less than lower limit
    if verbose:
        print "overflow test: array(%s, [%s])" % (`lowerLimit-1`, `type`)
    try:
        a = array([lowerLimit-1], type)
        raise TestFailed, "array(%s) did not overflow assigning %s" %\
                (`lowerLimit-1`, `type`)
    except OverflowError:
        pass
    # should not overflow assigning upper limit
    if verbose:
        print "overflow test: array(%s, [%s])" % (`upperLimit`, `type`)
    try:
        a = array([upperLimit], type)
    except:
        raise TestFailed, "array(%s) overflowed assigning %s" %\
                (`upperLimit`, `type`)
    # should overflow assigning more than upper limit
    if verbose:
        print "overflow test: array(%s, [%s])" % (`upperLimit+1`, `type`)
    try:
        a = array([upperLimit+1], type)
        raise TestFailed, "array(%s) did not overflow assigning %s" %\
                (`upperLimit+1`, `type`)
    except OverflowError:
        pass



def testtype(type, example):

    print "testing type ", type

    a = array([], type)
    a.append(example)
    if verbose:
        print 40*'*'
        print 'array after append: ', a
    a.typecode
    a.itemsize

    if a.typecode in ('i', 'b', 'h', 'l'):
        a.byteswap()

    if a.typecode == 'c':
        f = open(TESTFN, "w")
        f.write("The quick brown fox jumps over the lazy dog.\n")
        f.close()
        f = open(TESTFN, 'r')
        a.fromfile(f, 10)
        f.close()
        if verbose:
            print 'char array with 10 bytes of TESTFN appended: ', a
        a.fromlist(['a', 'b', 'c'])
        if verbose:
            print 'char array with list appended: ', a

    a.insert(0, example)
    if verbose:
        print 'array of %s after inserting another:' % a.typecode, a
    f = open(TESTFN, 'w')
    a.tofile(f)
    f.close()

    # This block is just to verify that the operations don't blow up.
    a.tolist()
    a.tostring()
    repr(a)
    str(a)

    if verbose:
        print 'array of %s converted to a list: ' % a.typecode, a.tolist()
    if verbose:
        print 'array of %s converted to a string: ' \
               % a.typecode, `a.tostring()`

    if type == 'c':
        a = array("abcde", type)
        a[:-1] = a
        if a != array("abcdee", type):
            raise TestFailed, "array(%s) self-slice-assign (head)" % `type`
        a = array("abcde", type)
        a[1:] = a
        if a != array("aabcde", type):
            raise TestFailed, "array(%s) self-slice-assign (tail)" % `type`
        a = array("abcde", type)
        a[1:-1] = a
        if a != array("aabcdee", type):
            raise TestFailed, "array(%s) self-slice-assign (cntr)" % `type`
        if a.index("e") != 5:
            raise TestFailed, "array(%s) index-test" % `type`
        if a.count("a") != 2:
            raise TestFailed, "array(%s) count-test" % `type`
        a.remove("e")
        if a != array("aabcde", type):
            raise TestFailed, "array(%s) remove-test" % `type`
        if a.pop(0) != "a":
            raise TestFailed, "array(%s) pop-test" % `type`
        if a.pop(1) != "b":
            raise TestFailed, "array(%s) pop-test" % `type`
        a.extend(array("xyz", type))
        if a != array("acdexyz", type):
            raise TestFailed, "array(%s) extend-test" % `type`
        a.pop()
        a.pop()
        a.pop()
        x = a.pop()
        if x != 'e':
            raise TestFailed, "array(%s) pop-test" % `type`
        if a != array("acd", type):
            raise TestFailed, "array(%s) pop-test" % `type`
        a.reverse()
        if a != array("dca", type):
            raise TestFailed, "array(%s) reverse-test" % `type`
    else:
        a = array([1, 2, 3, 4, 5], type)
        a[:-1] = a
        if a != array([1, 2, 3, 4, 5, 5], type):
            raise TestFailed, "array(%s) self-slice-assign (head)" % `type`
        a = array([1, 2, 3, 4, 5], type)
        a[1:] = a
        if a != array([1, 1, 2, 3, 4, 5], type):
            raise TestFailed, "array(%s) self-slice-assign (tail)" % `type`
        a = array([1, 2, 3, 4, 5], type)
        a[1:-1] = a
        if a != array([1, 1, 2, 3, 4, 5, 5], type):
            raise TestFailed, "array(%s) self-slice-assign (cntr)" % `type`
        if a.index(5) != 5:
            raise TestFailed, "array(%s) index-test" % `type`
        if a.count(1) != 2:
            raise TestFailed, "array(%s) count-test" % `type`
        a.remove(5)
        if a != array([1, 1, 2, 3, 4, 5], type):
            raise TestFailed, "array(%s) remove-test" % `type`
        if a.pop(0) != 1:
            raise TestFailed, "array(%s) pop-test" % `type`
        if a.pop(1) != 2:
            raise TestFailed, "array(%s) pop-test" % `type`
        a.extend(array([7, 8, 9], type))
        if a != array([1, 3, 4, 5, 7, 8, 9], type):
            raise TestFailed, "array(%s) extend-test" % `type`
        a.pop()
        a.pop()
        a.pop()
        x = a.pop()
        if x != 5:
            raise TestFailed, "array(%s) pop-test" % `type`
        if a != array([1, 3, 4], type):
            raise TestFailed, "array(%s) pop-test" % `type`
        a.reverse()
        if a != array([4, 3, 1], type):
            raise TestFailed, "array(%s) reverse-test" % `type`

    # test that overflow exceptions are raised as expected for assignment
    # to array of specific integral types
    from math import pow

    #check using long inputs
    if type in ('b', 'h', 'i', 'l'):
        a = array([], type)
        signedLowerLimit = -1 * long(pow(2, a.itemsize * 8 - 1))
        signedUpperLimit = long(pow(2, a.itemsize * 8 - 1)) - 1L
        unsignedLowerLimit = 0
        unsignedUpperLimit = long(pow(2, a.itemsize * 8)) - 1L
        testoverflow(type, signedLowerLimit, signedUpperLimit)

    #check using integer inputs - int cannot hold MAXINT+1 nor MININT-1
    # so only valid test types are byte and short for this test
    if type in ('b', 'h'):
        a = array([], type)
        signedLowerLimit =   -1 * int(pow(2, a.itemsize * 8 - 1))
        signedUpperLimit =   int(pow(2, a.itemsize * 8 - 1)) - 1
        unsignedLowerLimit = 0
        unsignedUpperLimit = int(pow(2, a.itemsize * 8)) - 1
        testoverflow(type, signedLowerLimit, signedUpperLimit)


def init_tests():
    test = array(['t','e','s','t'], 'c')
    assert init_test( "String initialisation", "test", 'c') == test, "string initialisation failed"

    test = array([41,42,43,44], 'i')
    s = test.tostring();
    assert init_test( "String2 initialisation", s, 'i') == test, "string 2 initialisation failed"

    init_test( "List initialisation", [1,2,3,4], 'i')

    init_test( "Tuple initialisation", (1,2,3,4), 'i')

    test = array([1,2,3,4], 'i')
    assert init_test( "array initialisation", test, 'i') == test, "array init failed"

    try:
        init_test( "Broken list initialisation", [1,2,3,4, 'fred'], 'i')
        raise TestFailed, "Broken list initialisation"
    except TypeError:
        pass

    test = array([1,2,3,4], 'i')
    try:
        init_test( "Broken PyArray initialisation", test, 'd')
        raise TestFailed, "Broken PyArray initialisation"
    except TypeError:
        pass

    f = open(TESTFN, "w")
    #f.write("\x00\x00\x00\x01")
    f.write("test message\nline2\nline3");
    f.close();

    f = open(TESTFN, "r")
    try:
        init_test( "Invalid initialisation object (file)", f, 'i')
        raise TestFailed, "Invalid initialisation object (file)"
    except TypeError:
        pass
    f.close()

    try:
        init_test( "Invalid initialisation object (module)", sys, 'i')
        raise TestFailed, "Invalid initialisation object (module)"
    except TypeError:
        pass
        
def extend_tests():
    test = array('testextend', 'c')
    assert extend_test("String extend", "test", "extend", 'c') == test, "String extend failed"

    test = array([1,2,3,4,51,52,53,54], 'i');
    assert extend_test("List extend", [1,2,3,4], [51,52,53,54], 'i') == test, "List extend failed"

    test = array((1,2,3,4,51,52,53,54), 'i');
    assert extend_test("Tuple extend", (1,2,3,4), (51,52,53,54), 'i') == test, "Tuple extend failed"

    try:
        extend_test("Broken list extend", [1,2,3,4], [51,52,53,"fred"], 'i')
        raise TestFailed, "Broken list extend"
    except TypeError:
        pass

    a = array([123.45, 567.89], 'd')
    test = array([1,2,3,4], 'i')
    try:
        assert extend_test("Array type mismatch test", [1,2,3,4], a, 'i') == test, "Array mismatch test failed"
        raise TestFailed, "Array type mismatch test"
    except TypeError:
        pass
    del a

    f = open(TESTFN, "r")
    try:
        extend_test("incorrect type extend (file)", [1,2,3,4], f, 'i')
        raise TestFailed, "incorrect type extend (file)"
    except TypeError:
        pass
    f.close()

    try:
        extend_test("incorrect type extend (module)", (1,2,3,4), sys, 'i')
        raise TestFailed, "incorrect type extend (module)"
    except TypeError:
        pass

    try:
        extend_test("incorrect type extend (integer)", [], 456, 'i')
        raise TestFailed, "incorrect type extend (integer)"
    except TypeError:
        pass

def fromlist_tests():
    test = array(['t','e','s','t','h','e','l','l','o'], 'c')
    assert fromlist_test("String fromlist", "test", ['h','e','l','l','o'], 'c') == test, "String fromlist failed"

    test = array([1,2,3,4,51,52,53,54], 'i')
    assert fromlist_test("integer fromlist", [1,2,3,4], [51,52,53,54], 'i') == test, "integer fromlist failed"

    try:
        fromlist_test("broken fromlist (integer)", [1,2,3,4], [51,52,53,"fred"], 'i')
        raise TestFailed, "broken fromlist (integer)"
    except TypeError:
        pass

    try:
        fromlist_test("invalid fromlist (tuple)", [1,2,3,4], (51,52,53,54), 'i')
        raise TestFailed, "invalid fromlist (tuple)"
    except TypeError:
        pass

def init_test(name, init, typecode):
    if verbose:
        print 40*'*'
        print name, "- type:", typecode
        print "initialiser:", init
    
    a = array(init, typecode)
    
    if verbose:
        print a
        
    return a

def extend_test(name, init, extend, typecode):
    if verbose:
        print 40*'*'
        print name, "- type:", typecode

    a = array(init, typecode)
        
    if verbose:
        print "initial:", a
        print "extended by:", extend

    a.extend(extend)

    #if no exceptions then
    if verbose:
        print "final:", a
        
    return a

def fromlist_test(name, init, listdata, typecode):
    if verbose:
        print 40*'*'
        print name   , "- type:", typecode

    a = array(init, typecode)
    
    if verbose:
        print "initial:", a
        print "fromlist source:", listdata

    a.fromlist(listdata)

    #if no exceptions then
    if verbose:
        print "final:", a

    return a

main()
