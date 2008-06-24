# CAU: Adaptation of the cpython 2.2 test_array.py for jython 2.2
#      Formerly test_jarray.py, now test_array.py so that this 
#      test completely supercedes the cpthyhon test.  It would
#      be better to simply complement the cpython test, but that
#      test bombs out too early due to basic incompatibilities.
#
#      The jarray module is being phased out, with all functionality
#      now available in the array module.

from test_support import *
from array import array, zeros
import sys
from java.lang import String
from java.lang.reflect import Array
from java.util import Arrays

print 'array module (test_array.py)'

def main():
   
    test_jarray() # while it's still supported
   
    test_java_compat()
    test_java_object_arrays()

    testtype('c', 'c')
    for type in (['b', 'h', 'i', 'l', 'f', 'd']):
        testtype(type, 1)
      
    #test a mix of known success and failure cases
    init_tests()
    extend_tests()
    fromlist_tests()

    unlink(TESTFN)

def test_jarray(): # until it is fully formally removed

   # While jarray is still being phased out, just flex the initilaizers.
   # The rest of the test for array will catch all the big problems.
   import jarray
   jarray.array(range(5), 'i')
   jarray.array([String("a"), String("b"), String("c")], String)
   jarray.zeros(5, 'i')
   jarray.zeros(5, String)


def test_java_object_arrays():
   jStringArr = array(String, [String("a"), String("b"), String("c")])
   verify(Arrays.equals(jStringArr.typecode, str(String)), 
         "String array typecode of wrong type, expected %s, found %s" % 
         (jStringArr.typecode, str(String)))
   verify(zeros(String, 5) == Array.newInstance(String, 5))

   import java # require for eval to work
   if jStringArr != eval(str(jStringArr)):
      raise TestFailed, "eval(str(%s)) <> %s" % (jStringArr,)*2
   

def test_java_compat():
   print 'array'
   from java import awt
   hsb = awt.Color.RGBtoHSB(0,255,255, None)
   #print hsb
   verify(hsb == array('f', [0.5,1,1]), 
         "output hsb float array does not correspond to input rgb values")
   
   rgb = apply(awt.Color.HSBtoRGB, tuple(hsb))
   #print hex(rgb)
   verify(rgb == 0xff00ffff, "output rgb bytes don't match input hsb floats")
   
   print 'zeros'
   hsb1 = zeros('f', 3)
   awt.Color.RGBtoHSB(0,255,255, hsb1)
   #print hsb, hsb1
   verify(hsb == hsb1, "hsb float arrays were not equal")

def testoverflow(type, lowerLimit, upperLimit):
        # should not overflow assigning lower limit
    if verbose:
        print "test overflow: array(%s, [%s])" % (lowerLimit, type)
    try:
        a = array(type, [lowerLimit])
    except:
        raise TestFailed("array(%s) overflowed assigning %s" %
                (lowerLimit, type))
    # should overflow assigning less than lower limit
    if verbose:
        print "test overflow: array(%s, [%s])" % (lowerLimit-1, type)
    try:
        a = array(type, [lowerLimit-1])
        raise TestFailed, "array(%s) did not overflow assigning %s" %\
                (lowerLimit-1, type)
    except OverflowError:
        pass
    # should not overflow assigning upper limit
    if verbose:
        print "test verflow: array(%s, [%s])" % (upperLimit, type)
    try:
        a = array(type, [upperLimit])
    except:
        raise TestFailed, "array(%s) overflowed assigning %s" %\
                (upperLimit, type)
    # should overflow assigning more than upper limit
    if verbose:
        print "test overflow: array(%s, [%s])" % (upperLimit+1, type)
    try:
        a = array(type, [upperLimit+1])
        raise TestFailed, "array(%s) did not overflow assigning %s" %\
                (upperLimit+1, type)
    except OverflowError:
        pass


def testtype(type, example):

    if verbose:
       print "testing type ", type

    a = array(type)
    a.append(example)
    if verbose:
        print 40*'*'
        print 'array after append: ', a
    a.typecode
    a.itemsize
    
    if a <> eval(str(a)):
       raise TestFailed, "eval(str(%s)) <> %s" % (a,a)

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
               % a.typecode, a.tostring()

    if type == 'c':
        a = array(type, "abcde")
        a[:-1] = a
        if a != array(type, "abcdee"):
            raise TestFailed, "array(%s) self-slice-assign (head)" % type
        a = array(type, "abcde")
        a[1:] = a
        if a != array(type, "aabcde"):
            raise TestFailed, "array(%s) self-slice-assign (tail)" % type
        a = array(type, "abcde")
        a[1:-1] = a
        if a != array(type, "aabcdee"):
            raise TestFailed, "array(%s) self-slice-assign (cntr)" % type
        if a.index("e") != 5:
            raise TestFailed, "array(%s) index-test" % type
        if a.count("a") != 2:
            raise TestFailed, "array(%s) count-test" % type
        a.remove("e")
        if a != array(type, "aabcde"):
            raise TestFailed, "array(%s) remove-test" % type
        if a.pop(0) != "a":
            raise TestFailed, "array(%s) pop-test" % type
        if a.pop(1) != "b":
            raise TestFailed, "array(%s) pop-test" % type
        a.extend(array(type, "xyz"))
        if a != array(type, "acdexyz"):
            raise TestFailed, "array(%s) extend-test" % type
        a.pop()
        a.pop()
        a.pop()
        x = a.pop()
        if x != 'e':
            raise TestFailed, "array(%s) pop-test" % type
        if a != array(type, "acd"):
            raise TestFailed, "array(%s) pop-test" % type
        a.reverse()
        if a != array(type, "dca"):
            raise TestFailed, "array(%s) reverse-test" % type
    else:
        a = array(type, [1, 2, 3, 4, 5])
        a[:-1] = a
        if a != array(type, [1, 2, 3, 4, 5, 5]):
            raise TestFailed, "array(%s) self-slice-assign (head)" % type
        a = array(type, [1, 2, 3, 4, 5])
        a[1:] = a
        if a != array(type, [1, 1, 2, 3, 4, 5]):
            raise TestFailed, "array(%s) self-slice-assign (tail)" % type
        a = array(type, [1, 2, 3, 4, 5])
        a[1:-1] = a
        if a != array(type, [1, 1, 2, 3, 4, 5, 5]):
            raise TestFailed, "array(%s) self-slice-assign (cntr)" % type
        if a.index(5) != 5:
            raise TestFailed, "array(%s) index-test" % type
        if a.count(1) != 2:
            raise TestFailed, "array(%s) count-test" % type
        a.remove(5)
        if a != array(type, [1, 1, 2, 3, 4, 5]):
            raise TestFailed, "array(%s) remove-test" % type
        if a.pop(0) != 1:
            raise TestFailed, "array(%s) pop-test" % type
        if a.pop(1) != 2:
            raise TestFailed, "array(%s) pop-test" % type
        a.extend(array(type, [7, 8, 9]))
        if a != array(type, [1, 3, 4, 5, 7, 8, 9]):
            raise TestFailed, "array(%s) extend-test" % type
        a.pop()
        a.pop()
        a.pop()
        x = a.pop()
        if x != 5:
            raise TestFailed, "array(%s) pop-test" % type
        if a != array(type, [1, 3, 4]):
            raise TestFailed, "array(%s) pop-test" % type
        a.reverse()
        if a != array(type, [4, 3, 1]):
            raise TestFailed, "array(%s) reverse-test" % type

    # test that overflow exceptions are raised as expected for assignment
    # to array of specific integral types
    from math import pow

    #check using long inputs
    if type in ('b', 'h', 'i', 'l'):
        a = array(type)
        signedLowerLimit = -1 * long(pow(2, a.itemsize * 8 - 1))
        signedUpperLimit = long(pow(2, a.itemsize * 8 - 1)) - 1L
        unsignedLowerLimit = 0
        unsignedUpperLimit = long(pow(2, a.itemsize * 8)) - 1L
        testoverflow(type, signedLowerLimit, signedUpperLimit)

    #check using integer inputs - int cannot hold MAXINT+1 nor MININT-1
    # so only valid test types are byte and short for this test
    if type in ('b', 'h'):
        a = array(type)
        signedLowerLimit =   -1 * int(pow(2, a.itemsize * 8 - 1))
        signedUpperLimit =   int(pow(2, a.itemsize * 8 - 1)) - 1
        unsignedLowerLimit = 0
        unsignedUpperLimit = int(pow(2, a.itemsize * 8)) - 1
        testoverflow(type, signedLowerLimit, signedUpperLimit)


def init_tests():
    test = array('c', ['t','e','s','t'])
    verify(init_test( "test: String initialisation", "test", 'c') == test, 
          "string initialisation failed")

    test = array('i', [41,42,43,44])
    s = test.tostring();
    verify(init_test( "test: String2 initialisation", s, 'i') == test, 
          "string 2 initialisation failed")

    init_test( "test: List initialisation", [1,2,3,4], 'i')

    init_test( "test: Tuple initialisation", (1,2,3,4), 'i')

    test = array('i', [1,2,3,4])
    verify(init_test( "test: array initialisation", test, 'i') == test, 
          "array init failed")

    try:
        init_test('test: "broken" list initialisation', [1,2,3,4, 'fred'], 'i')
        raise TestFailed, '"broken" list initialisation'
    except TypeError:
        pass

    test = array('i', [1,2,3,4])
    try:
        init_test('test: "broken" PyArray initialisation', test, 'd')
        raise TestFailed, '"broken" PyArray initialisation'
    except TypeError:
        pass

    f = open(TESTFN, "w")
    #f.write("\x00\x00\x00\x01")
    f.write("test message\nline2\nline3");
    f.close();

    f = open(TESTFN, "r")
    try:
        init_test( "test: Invalid initialisation object (file)", f, 'i')
        raise TestFailed, "Invalid initialisation object (file)"
    except TypeError:
        pass
    f.close()

    try:
        init_test( "test: Invalid initialisation object (module)", sys, 'i')
        raise TestFailed, "Invalid initialisation object (module)"
    except TypeError:
        pass
        
def extend_tests():
    test = array('c', 'testextend')
    verify(extend_test("test: String extend", "test", "extend", 'c') == test, 
          "String extend failed")

    test = array('i', [1,2,3,4,51,52,53,54]);
    verify( extend_test("test: List extend", [1,2,3,4], [51,52,53,54], 'i') == test, 
          "List extend failed")

    test = array('i', (1,2,3,4,51,52,53,54));
    verify( extend_test("test: Tuple extend", (1,2,3,4), (51,52,53,54), 'i') == test, 
          "Tuple extend failed")

    try:
        extend_test('test: "broken" list extend', [1,2,3,4], [51,52,53,"fred"], 'i')
        raise TestFailed, 'test: "broken" list extend'
    except TypeError:
        pass

    a = array('d', [123.45, 567.89])
    test = array('i', [1,2,3,4])
    try:
        assert extend_test("test: Array type mismatch", [1,2,3,4], a, 'i') == test, \
              "Array mismatch test failed"
        raise TestFailed, "test: Array type mismatch"
    except TypeError:
        pass
    del a

    f = open(TESTFN, "r")
    try:
        extend_test("test: incorrect type extend (file)", [1,2,3,4], f, 'i')
        raise TestFailed, "test: incorrect type extend (file)"
    except TypeError:
        pass
    f.close()

    try:
        extend_test("test: incorrect type extend (module)", (1,2,3,4), sys, 'i')
        raise TestFailed, "test: incorrect type extend (module)"
    except TypeError:
        pass

    try:
        extend_test("test: incorrect type extend (integer)", [], 456, 'i')
        raise TestFailed, "test: incorrect type extend (integer)"
    except TypeError:
        pass

def fromlist_tests():
    test = array('c', ['t','e','s','t','h','e','l','l','o'])
    verify(fromlist_test("test: String fromlist", "test", ['h','e','l','l','o'], 'c') == test, 
          "String fromlist failed")

    test = array('i', [1,2,3,4,51,52,53,54])
    verify(fromlist_test("test: integer fromlist", [1,2,3,4], [51,52,53,54], 'i') == test, 
          "integer fromlist failed")

    try:
        fromlist_test('test: "broken" fromlist (integer)', [1,2,3,4], [51,52,53,"fred"], 'i')
        raise TestFailed, 'test: "broken" fromlist (integer)'
    except TypeError:
        pass

    try:
        fromlist_test("test: invalid fromlist (tuple)", [1,2,3,4], (51,52,53,54), 'i')
        raise TestFailed, "test: invalid fromlist (tuple)"
    except TypeError:
        pass

def init_test(name, init, typecode):
    if verbose:
        print 40*'*'
        print name, "- type:", typecode
        print "initialiser:", init
    
    a = array(typecode, init)
    
    if verbose:
        print a
        
    return a

def extend_test(name, init, extend, typecode):
    if verbose:
        print 40*'*'
        print name, "- type:", typecode

    a = array(typecode, init)
        
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

    a = array(typecode, init)
    
    if verbose:
        print "initial:", a
        print "fromlist source:", listdata

    a.fromlist(listdata)

    #if no exceptions then
    if verbose:
        print "final:", a

    return a

main()
