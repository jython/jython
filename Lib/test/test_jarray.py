# CAU: adaptation of the cpython 2.2 test_array.py for jython 2.2
#      combined with the jarray test from 2.1

from test_support import *

print_test('jarray module (test_jarray.py)', 1)

from jarray import array, zeros

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

#CAU: nyi    unlink(TESTFN)


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
#CAU: nyi    a.itemsize
    
#CAU: nyi     if a.typecode in ('i', 'b', 'h', 'l'):
#CAU: nyi         a.byteswap()

#CAU: nyi     if a.typecode == 'c':
#CAU: nyi         f = open(TESTFN, "w")
#CAU: nyi         f.write("The quick brown fox jumps over the lazy dog.\n")
#CAU: nyi         f.close()
#CAU: nyi         f = open(TESTFN, 'r')
#CAU: nyi         a.fromfile(f, 10)
#CAU: nyi         f.close()
#CAU: nyi         if verbose:
#CAU: nyi             print 'char array with 10 bytes of TESTFN appended: ', a
#CAU: nyi         a.fromlist(['a', 'b', 'c'])
#CAU: nyi         if verbose:
#CAU: nyi             print 'char array with list appended: ', a

#CAU: nyi     a.insert(0, example)
#CAU: nyi     if verbose:
#CAU: nyi         print 'array of %s after inserting another:' % a.typecode, a
#CAU: nyi     f = open(TESTFN, 'w')
#CAU: nyi     a.tofile(f)
#CAU: nyi     f.close()

#CAU: nyi     # This block is just to verify that the operations don't blow up.
#CAU: nyi     a.tolist()
#CAU: nyi     a.tostring()
    repr(a)
    str(a)

#CAU: nyi     if verbose:
#CAU: nyi         print 'array of %s converted to a list: ' % a.typecode, a.tolist()
#CAU: nyi     if verbose:
#CAU: jython 2.1 PyArray only supported tostring() on byte and char arrays
#CAU: nyi         print 'array of %s converted to a string: ' \
#CAU: nyi                % a.typecode, `a.tostring()`

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
#CAU: nyi    from math import pow
#CAU: nyi    if type in ('b', 'h', 'i', 'l'):
#CAU: nyi        # check signed and unsigned versions
#CAU: nyi        a = array(type)
#CAU: nyi        signedLowerLimit = -1 * long(pow(2, a.itemsize * 8 - 1))
#CAU: nyi        signedUpperLimit = long(pow(2, a.itemsize * 8 - 1)) - 1L
#CAU: nyi        unsignedLowerLimit = 0
#CAU: nyi        unsignedUpperLimit = long(pow(2, a.itemsize * 8)) - 1L
#CAU: nyi        testoverflow(type, signedLowerLimit, signedUpperLimit)
#CAU: nyi        testoverflow(type.upper(), unsignedLowerLimit, unsignedUpperLimit)



main()
