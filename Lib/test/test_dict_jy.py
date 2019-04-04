from test import test_support
import unittest
import UserDict
from collections import defaultdict
import test_dict

from java.util import HashMap, LinkedHashMap, Hashtable
from java.util.concurrent import ConcurrentHashMap
from org.python.core import PyStringMap as stringmap


class DictInitTest(unittest.TestCase):

    def testInternalSetitemInInit(self):
        """Test for http://jython.org/bugs/1816134

        CPython's dict uses an internal setitem method to initialize itself
        rather than the one on its subclasses, and this tests that Jython does
        as well.
        """
        class Subdict(dict):
            def __init__(self):
                super(Subdict, self).__init__([('a',1)])
                self.createdInInit = 1

            def __setitem__(self, key, value):
                super(Subdict, self).__setitem__(key, value)
                assert hasattr(self, 'createdInInit')
                self.createdInInit = value

        s = Subdict()
        s[7] = 'called'
        self.assertEquals('called', s.createdInInit)

    def testUnhashableKeys(self):
        try:
            a = {[1]:2}
        except TypeError:
            pass
        else:
            self.fail("list as dict key should raise TypeError")

        try:
            a = {{1:2}:3}
        except TypeError:
            pass
        else:
            self.fail("dict as dict key should raise TypeError")


class DictCmpTest(unittest.TestCase):
    "Test for http://bugs.jython.org/issue1031"

    def testDictCmp(self):
        # 'Implicit' comparision of dicts against other types instances
        # shouldn't raise exception:
        self.assertNotEqual({}, '')
        # The same, but explicitly calling __cmp__ should raise TypeError:
        self.assertRaises(TypeError, {}.__cmp__, '')

    def testDictDerivedCmp(self):
        # With derived classes that doesn't override __cmp__, the behaviour
        # should be the same that with dicts:
        class derived_dict(dict): pass
        self.assertEqual(derived_dict(), {})
        self.assertNotEqual(derived_dict(), '')
        self.assertRaises(TypeError, derived_dict().__cmp__, '')
        # But, if they *override* __cmp__ and raise TypeError from there, we
        # have exception raised when checking for equality...
        class non_comparable_dict(dict):
            def __cmp__(self, other):
                raise TypeError, "I always raise TypeError"
        self.assertRaises(TypeError, lambda: non_comparable_dict() == '')
        self.assertRaises(TypeError, non_comparable_dict().__cmp__, '')
        # ...unless you compare it with other dicts:
        # self.assertEqual(non_comparable_dict(), {})

        # The same happens even if the overridden __cmp__ doesn't nothing apart
        # from calling super:
        class dummy_dict_with_cmp(dict):
            def __cmp__(self, other):
                return super(dummy_dict_with_cmp, self).__cmp__(other)

        self.assertEqual(dummy_dict_with_cmp(), {})
        # But TypeError is raised when comparing against other types
        self.assertRaises(TypeError, lambda: dummy_dict_with_cmp() == '')
        self.assertRaises(TypeError, dummy_dict_with_cmp().__cmp__, '')
        # Finally, the Python implementation shouldn't be tricked by not
        # implementing __cmp__ on the actual type of the dict-derived instance,
        # but implementing it on a superclass.
        class derived_dict_with_custom_cmp(dict):
            def __cmp__(self, other):
                return 0
        class yet_another_dict(derived_dict_with_custom_cmp): pass
        self.assertEqual(derived_dict_with_custom_cmp(), '')
        self.assertEqual(yet_another_dict(), '')


class DictMiscTest(unittest.TestCase):

    def test_pop_key_error(self):
        # tests http://bugs.jython.org/issue2247
        with self.assertRaisesRegexp(KeyError, r"^1$"):
            {}.pop(1)
        with self.assertRaisesRegexp(KeyError, r"^\(\)$"):
            {}.pop(())
        with self.assertRaisesRegexp(KeyError, r"^frozenset\(\[\]\)$"):
            {}.pop(frozenset())


class DerivedDictTest(unittest.TestCase):
    "Tests for derived dict behaviour"

    def test_raising_custom_key_error(self):
        class CustomKeyError(KeyError):
            pass
        class DerivedDict(dict):
            def __getitem__(self, key):
                raise CustomKeyError("custom message")
        self.assertRaises(CustomKeyError, lambda: DerivedDict()['foo'])

    def test_issue1676(self):
        #See http://bugs.jython.org/issue1676
        x=defaultdict()
        #This formerly caused an NPE.
        self.assertEqual(None, x.pop(None,None))

    def test_big_dict(self):
        """Verify that fairly large collection literals of primitives can be constructed."""
        # use \n to separate to avoid parser problems

        d = eval("{" + ",\n".join(("'key{}': {}".format(x, x) for x in xrange(16000))) +"}")
        self.assertEqual(len(d), 16000)
        self.assertEqual(sum(d.itervalues()), 127992000)


class JavaIntegrationTest(unittest.TestCase):
    "Tests for instantiating dicts from Java maps and hashtables"

    type2test = HashMap

    def test_map(self):
        x = self.type2test()
        x.put('a', 1)
        x.put('b', 2)
        x.put('c', 3)
        x.put((1,2), "xyz")
        y = dict(x)
        self.assertEqual(set(y.items()), set([('a', 1), ('b', 2), ('c', 3), ((1,2), "xyz")]))

    def test_map_builtin_pymethods(self):
        x = self.type2test()
        x['a'] = 1
        x[(1, 2)] = 'xyz'
        self.assertEqual({tup for tup in x.iteritems()}, {('a', 1), ((1, 2), 'xyz')})
        self.assertEqual({tup for tup in x.itervalues()}, {1, 'xyz'})
        self.assertEqual({tup for tup in x.iterkeys()}, {'a', (1, 2)})
        self.assertEqual(str(x), repr(x))
        self.assertEqual(type(str(x)), type(repr(x)))

    def test_equal(self):
        for d in ({}, {1:2}):
            x = self.type2test(d)
            self.assertEqual(x, d)
            self.assertEqual(d, x)
            self.assertEqual(x, HashMap(d))

    def test_remove(self):
        x = self.type2test({'a': 1})
        del x['a']
        self.assertEqual(x, {})

        x = self.type2test({})
        with self.assertRaises(KeyError):
            del x[0]

    def test_equality_empty_dict(self):
        jmap = self.type2test()
        self.assertTrue(jmap == {})
        self.assertTrue({} == jmap)

    def test_equality_simple_dict(self):
        jmap = self.type2test()
        self.assertFalse({'a': 1} == jmap)
        self.assertFalse(jmap == {'a': 1})

    def test_equality_mixed_types_dict(self):
        ref = {False:0, 'a':1, u'b':2L, 3:"3"}
        alt = {0:False, u'a':True, 'b':2, 3:"3"}
        self.assertEqual(ref, alt) # test assumption
        jref = self.type2test(ref)
        for v in [ref, alt, jref]:
            self.assertTrue(jref == v)
            self.assertTrue(v == jref)
            self.assertTrue(jref == self.type2test(v))
            self.assertTrue(self.type2test(v) == jref)

        alt1 = ref.copy(); alt1['a'] = 2;
        alt2 = ref.copy(); del alt2['a'];
        alt3 = ref.copy(); alt3['c'] = [];
        for v in [alt1, alt2, alt3, {}]:
            self.assertFalse(jref == v)
            self.assertFalse(v == jref)
            self.assertFalse(jref == self.type2test(v))
            self.assertFalse(self.type2test(v) == jref)

    # Test for http://bugs.jython.org/issue2639
    # This is to test the != comparisons between Java and Python maps/dict
    def test_inequality_empty_dict(self):
        jmap = self.type2test()
        self.assertFalse(jmap != {})
        self.assertFalse({} != jmap)

    def test_inequality_simple_dict(self):
        jmap = self.type2test()
        self.assertTrue(jmap != {'a': 1})
        self.assertTrue({'a': 1} != jmap)

    def test_inequality_mixed_types_dict(self):
        ref = {False:0, 'a':1, u'b':2L, 3:"3"}
        alt = {0:False, u'a':True, 'b':2, 3:"3"}
        self.assertEqual(ref, alt) # test assumption
        jref = self.type2test(ref)
        for v in [ref, alt, jref]:
            self.assertFalse(jref != v)
            self.assertFalse(v != jref)
            self.assertFalse(jref != self.type2test(v))
            self.assertFalse(self.type2test(v) != jref)

        alt1 = ref.copy(); alt1['a'] = 2;
        alt2 = ref.copy(); del alt2['a'];
        alt3 = ref.copy(); alt3['c'] = [];
        for v in [alt1, alt2, alt3, {}]:
            self.assertTrue(jref != v)
            self.assertTrue(v != jref)
            self.assertTrue(jref != self.type2test(v))
            self.assertTrue(self.type2test(v) != jref)


class JavaHashMapTest(JavaIntegrationTest):
    type2test = HashMap

class JavaLinkedHashMapTest(JavaIntegrationTest):
    type2test = LinkedHashMap

class JavaHashtableTest(JavaIntegrationTest):
    type2test = Hashtable

class JavaConcurrentHashMapTest(JavaIntegrationTest):
    type2test = ConcurrentHashMap


class JavaDictTest(test_dict.DictTest):
    # Extend Python standard tests for dict. (Also used for Map proxies.)

    type2test = dict

    def test_copy_java_hashtable(self):
        x = Hashtable()
        xc = x.copy()
        self.assertEqual(type(x), type(xc))

    def test_repr_value_None(self):
        x = self.type2test({1:None})
        self.assertEqual(repr(x), '{1: None}')

    def test_set_return_None(self):
        x = self.type2test({1:2})
        self.assertEqual(x.__setitem__(1, 3), None)
        self.assertEqual(x.__getitem__(1), 3)

    def test_del_return_None(self):
        x = self.type2test({1:2})
        self.assertEqual(x.__delitem__(1), None)
        self.assertEqual(len(x), 0)

    def assert_property(self, prop, a, b):
        prop(self._make_dict(a), self._make_dict(b))
        prop(a, self._make_dict(b))
        prop(self._make_dict(a), b)

    def assert_not_property(self, prop, a, b):
        with self.assertRaises(AssertionError):
            prop(self._make_dict(a), self._make_dict(b))
        with self.assertRaises(AssertionError):
            prop(a, self._make_dict(b))
        with self.assertRaises(AssertionError):
            prop(self._make_dict(a), b)

    def test_list_equality(self):
        class A(dict): pass
        d = {'a':1, u'\xe7':2, u'\U00010842':3, 42:None}
        for dtype in (dict, self.type2test, A):
            self.assertEquals([dtype()], [dict()])
            self.assertEquals([dtype(d)], [d])

    # Some variants with unicode keys

    def test_repr_unicode(self):
        d = self._make_dict({})
        d[u'3\uc6d4'] = 2
        self.assertEqual(repr(d), "{u'3\\uc6d4': 2}")

        d = self._make_dict({})
        d[2] = u'\u039c\u03ac\u03c1\u03c4\u03b9\u03bf\u03c2'
        self.assertEqual(repr(d), "{2: u'\\u039c\\u03ac\\u03c1\\u03c4\\u03b9\\u03bf\\u03c2'}")

        d = self._make_dict({})
        d[u'\uc6d4'] = d
        self.assertEqual(repr(d), "{u'\\uc6d4': {...}}")

    def test_fromkeys_unicode(self):
        self.assertEqual(self.type2test.fromkeys(u'\U00010840\U00010841\U00010842', u'\u1810'),
                {u'\U00010840':u'\u1810', u'\U00010841':u'\u1810', u'\U00010842':u'\u1810'})
        self.assertEqual(self.type2test.fromkeys(u'\U00010840\U00010841\U00010842'),
                {u'\U00010840':None, u'\U00010841':None, u'\U00010842':None})

    # NOTE: when comparing dictionaries below exclusively in Java
    # space, keys like 1 and 1L are different objects. Only when they
    # are brought into Python space by Py.java2py, as is needed when
    # comparing a Python dict with a Java Map, do we see them become
    # equal.

    def test_le(self):
        self.assert_property(self.assertLessEqual, {}, {})
        self.assert_property(self.assertLessEqual, {1: 2}, {1: 2})
        self.assert_not_property(self.assertLessEqual, {1: 2, 3: 4}, {1: 2})
        self.assert_property(self.assertLessEqual, {}, {1: 2})
        self.assertLessEqual(self._make_dict({1: 2}), {1L: 2L, 3L: 4L})
        self.assertLessEqual({1L: 2L}, self._make_dict({1: 2, 3L: 4L}))

    def test_lt(self):
        self.assert_not_property(self.assertLess, {}, {})
        self.assert_not_property(self.assertLess, {1: 2}, {1: 2})
        self.assert_not_property(self.assertLessEqual, {1: 2, 3: 4}, {1: 2})
        self.assert_property(self.assertLessEqual, {}, {1: 2})
        self.assertLess(self._make_dict({1: 2}), {1L: 2L, 3L: 4L})
        self.assertLess({1L: 2L}, self._make_dict({1: 2, 3L: 4L}))

    def test_ge(self):
        self.assert_property(self.assertGreaterEqual, {}, {})
        self.assert_property(self.assertGreaterEqual, {1: 2}, {1: 2})
        self.assert_not_property(self.assertLessEqual, {1: 2, 3: 4}, {1: 2})
        self.assert_property(self.assertLessEqual, {}, {1: 2})
        self.assertGreaterEqual(self._make_dict({1: 2, 3: 4}), {1L: 2L})
        self.assertGreaterEqual({1L: 2L, 3L: 4L}, self._make_dict({1: 2}))

    def test_gt(self):
        self.assert_not_property(self.assertGreater, {}, {})
        self.assert_not_property(self.assertGreater, {1: 2}, {1: 2})
        self.assert_not_property(self.assertLessEqual, {1: 2, 3: 4}, {1: 2})
        self.assert_property(self.assertLessEqual, {}, {1: 2})
        self.assertGreater(self._make_dict({1: 2, 3: 4}), {1L: 2L})
        self.assertGreater({1L: 2L, 3L: 4L}, self._make_dict({1: 2}))


class NullAcceptingDictTest(JavaDictTest):
    # Extension of Java Map proxy tests to cases where the underlying
    # container is able to accept nulls. Same tests as for dict (mostly).

    def test_missing(self):
        # Proxy map types are not expected to support __missing__.
        self.assertFalse(hasattr(self.type2test, "__missing__"))
        self.assertFalse(hasattr(self._make_dict({}), "__missing__"))

    def test_fromkeys(self):
        # Adapted from test_dict.DictTest.test_fromkeys by removal of test
        # sub-classes since this does not work with proxy types.
        Dict = self.type2test

        self.assertEqual(Dict.fromkeys('abc'), {'a':None, 'b':None, 'c':None})

        d = self._make_dict({})
        self.assertIsNot(d.fromkeys('abc'), d)
        self.assertEqual(d.fromkeys('abc'), {'a':None, 'b':None, 'c':None})
        self.assertEqual(d.fromkeys((4,5),0), {4:0, 5:0})
        self.assertEqual(d.fromkeys([]), {})
        def g():
            yield 1
        self.assertEqual(d.fromkeys(g()), {1:None})
        self.assertRaises(TypeError, self._make_dict({}).fromkeys, 3)

        class Exc(Exception): pass
        class BadSeq(object):
            def __iter__(self):
                return self
            def next(self):
                raise Exc()
        self.assertRaises(Exc, Dict.fromkeys, BadSeq())

class NullRejectingDictTest(NullAcceptingDictTest):
    # Adaptation of Java Map proxy tests to cases where the underlying
    # container cannot accept nulls, therefore None cannot be stored.

    def test_reject_none(self):
        d = self._make_dict({'a': 1})
        with self.assertRaises(ValueError):
            d['a'] = None
        with self.assertRaises(ValueError):
            d['b'] = None
        # There is no __init__ or __new__ we can customise, so raises NullPointerException.
        # self.assertRaises(ValueError, self._make_dict, {'c': None})
        self.assertRaises(ValueError, d.update, {'c': None})
        with self.assertRaises(ValueError):
            d.update(c=None)
        self.assertRaises(ValueError, d.fromkeys, 'cde')
        self.assertRaises(ValueError, d.fromkeys, 'cde', None)

    def test_list_equality(self):
        class A(dict): pass
        d = {'a':1, u'\xe7':2, u'\U00010842':3, 42:True}
        for dtype in (dict, self.type2test, A):
            self.assertEquals([dtype()], [dict()])
            self.assertEquals([dtype(d)], [d])

    @unittest.skip("not relevant since cannot hold None.")
    def test_repr_value_None(self): pass

    def test_fromkeys(self):
        # Adapted from test_dict.DictTest.test_fromkeys avoiding None
        # (except as test) and by removal of test sub-classing.
        Dict = self.type2test

        self.assertEqual(Dict.fromkeys('abc', 42), {'a':42, 'b':42, 'c':42})
        self.assertRaises(TypeError, self._make_dict({}).fromkeys, 3, 42)
        self.assertRaises(ValueError, self._make_dict({}).fromkeys, 'abc', None)

        d = self._make_dict({})
        self.assertIsNot(d.fromkeys('abc', 42), d)
        self.assertEqual(d.fromkeys('abc', 42), {'a':42, 'b':42, 'c':42})
        self.assertEqual(d.fromkeys((4,5),0), {4:0, 5:0})
        self.assertEqual(d.fromkeys([], 42), {})
        def g():
            yield 1
        self.assertEqual(d.fromkeys(g(), 42), {1:42})
        self.assertRaises(TypeError, self._make_dict({}).fromkeys, 3)
        self.assertRaises(TypeError, self._make_dict({}).fromkeys, 3, 42)

        class Exc(Exception): pass
        class BadSeq(object):
            def __iter__(self):
                return self
            def next(self):
                raise Exc()
        self.assertRaises(Exc, Dict.fromkeys, BadSeq())

    def test_fromkeys_unicode(self):
        self.assertEqual(self.type2test.fromkeys(u'\U00010840\U00010841\U00010842', u'\u1810'),
                {u'\U00010840':u'\u1810', u'\U00010841':u'\u1810', u'\U00010842':u'\u1810'})

    def test_setdefault(self):
        # Adapted from test_dict.DictTest.test_setdefault avoiding None
        d = self._make_dict({'key0': False})
        d.setdefault('key0', [])
        self.assertIs(d.setdefault('key0'), False)
        d.setdefault('key', []).append(3)
        self.assertEqual(d['key'][0], 3)
        d.setdefault('key', []).append(4)
        self.assertEqual(len(d['key']), 2)
        self.assertRaises(TypeError, d.setdefault)

        class Exc(Exception): pass

        class BadHash(object):
            fail = False
            def __hash__(self):
                if self.fail:
                    raise Exc()
                else:
                    return 42

        x = BadHash()
        d[x] = 42
        x.fail = True
        self.assertRaises(Exc, d.setdefault, x, [])

    @unittest.skip("See bjo #2746. Java keys() returns an Enumerator.")
    def test_has_key(self): pass # defining here only so we can skip it

    @unittest.skip("See bjo #2746. Java keys() returns an Enumerator.")
    def test_keys(self): pass # defining here only so we can skip it


class PyStringMapDictTest(test_dict.DictTest):
    # __dict__ for objects uses PyStringMap for historical reasons, so
    # we have to test separately

    type2test = stringmap

    def test_missing(self):
        Dict = self.type2test
        # Make sure dict doesn't have a __missing__ method
        self.assertFalse(hasattr(Dict, "__missing__"))
        self.assertFalse(hasattr(self._make_dict({}), "__missing__"))
        # PyStringMap is not expected to support __missing__ as it cannot be sub-classed.
        # At least, it wasn't added when it was added to PyDictionary.

    def test_fromkeys(self):
        # Based on test_dict.DictTest.test_fromkeys, without sub-classing stringmap
        Dict = self.type2test

        self.assertEqual(Dict.fromkeys('abc'), {'a':None, 'b':None, 'c':None})

        d = self._make_dict({})
        self.assertIsNot(d.fromkeys('abc'), d)
        self.assertEqual(d.fromkeys('abc'), {'a':None, 'b':None, 'c':None})
        self.assertEqual(d.fromkeys((4,5),0), {4:0, 5:0})
        self.assertEqual(d.fromkeys([]), {})
        def g():
            yield 1
        self.assertEqual(d.fromkeys(g()), {1:None})
        self.assertRaises(TypeError, self._make_dict({}).fromkeys, 3)

        class Exc(Exception): pass

        class BadSeq(object):
            def __iter__(self):
                return self
            def next(self):
                raise Exc()

        self.assertRaises(Exc, Dict.fromkeys, BadSeq())

        # test fast path for dictionary inputs
        d = Dict(zip(range(6), range(6)))
        self.assertEqual(Dict.fromkeys(d, 0), Dict(zip(range(6), [0]*6)))



class JavaHashMapDictTest(NullAcceptingDictTest):
    type2test = HashMap

class JavaLinkedHashMapDictTest(NullAcceptingDictTest):
    type2test = LinkedHashMap

class JavaHashtableDictTest(NullRejectingDictTest):
    type2test = Hashtable

class JavaConcurrentHashMapDictTest(NullRejectingDictTest):
    type2test = ConcurrentHashMap


def test_main():
    test_support.run_unittest(
        DictInitTest,
        DictCmpTest,
        DictMiscTest,
        DerivedDictTest,
        JavaHashMapTest,
        JavaLinkedHashMapTest,
        JavaConcurrentHashMapTest,
        JavaHashtableTest,
        JavaDictTest,
        PyStringMapDictTest,
        JavaHashMapDictTest,
        JavaLinkedHashMapDictTest,
        JavaHashtableDictTest,
        JavaConcurrentHashMapDictTest,
    )


if __name__ == '__main__':
    test_main()
