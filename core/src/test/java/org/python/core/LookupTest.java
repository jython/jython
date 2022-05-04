package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test actions that depend on looking up keys in dictionaries,
 * including those embedded in {@code type} objects (which have
 * special characteristics). A particular concern is to verify the
 * interchangeability of acceptable implementations of Python types
 * when used as key. For example, data entered with a key that is an
 * INteger, should be retrievable by one that is a BigInteger with
 * the same value.
 */
class LookupTest extends UnitTestSupport {

    /**
     * A {@code KeyTuple} holds a single Python value, realised in each
     * of the accepted implementations of some Python type.
     */
    private abstract static class KeyTuple {

        final Object py;

        KeyTuple(Object py) { this.py = py; }
    }

    private static final String A = "a";
    private static final String B = "b123";
    private static final String C = "Python";
    private static final String D = "QZthon"; // Same hash as C

    /**
     * A {@code StrKeyTuple} holds a single Python {@code str} value,
     * realised in each of the accepted implementations {@code String}
     * and {@link PyUnicode}.
     */
    private static class StrKeyTuple extends KeyTuple {

        final String s;

        StrKeyTuple(String s) {
            super(newPyUnicode(s));
            this.s = s;
        }
    }

    private static final List<StrKeyTuple> strKeyTuples = new LinkedList<StrKeyTuple>();

    @BeforeAll
    static void fillStrKeyTuples() {
        strKeyTuples.add(new StrKeyTuple(A));
        strKeyTuples.add(new StrKeyTuple(B));
        strKeyTuples.add(new StrKeyTuple(C));
        strKeyTuples.add(new StrKeyTuple(D));
    }

    /**
     * An {@code IntKeyTuple} holds a single Python {@code int} value,
     * realised in each of the accepted implementations {@code Integer},
     * {@code BigInteger}, and {@link PyLong}.
     *
     */
    private static class IntKeyTuple extends KeyTuple {

        final Integer i;
        final BigInteger b;

        IntKeyTuple(int i) {
            super(newPyLong(i));
            this.i = i;
            this.b = BigInteger.valueOf(i);
        }
    }

    private static final List<IntKeyTuple> intKeyTuples = new LinkedList<IntKeyTuple>();

    @BeforeAll
    static void fillIntKeyTuples() {
        intKeyTuples.add(new IntKeyTuple(4));
        intKeyTuples.add(new IntKeyTuple(-5));
        intKeyTuples.add(new IntKeyTuple(Integer.MAX_VALUE));
        intKeyTuples.add(new IntKeyTuple(Integer.MIN_VALUE));
        intKeyTuples.add(new IntKeyTuple(Integer.MIN_VALUE + 1));
    }

    @Nested
    @DisplayName("Object hashes equal each other and Java hashCode()")
    class HashesEqual {

        /**
         * Verify that {@code str.__hash__} produces the same value as Java
         * {@code hashCode()} in each accepted implementation.
         *
         * @throws TypeError if {@code v} is an unhashable type
         * @throws Throwable on errors within {@code __hash__}
         */
        @Test
        void where_str() throws TypeError, Throwable {
            assertEqualHashes(strKeyTuples, k -> k.s, k -> k.py);
        }

        /**
         * Verify that {@code int.__hash__} produces the same value as Java
         * {@code hashCode()} in each accepted implementation.
         *
         * @throws TypeError if {@code v} is an unhashable type
         * @throws Throwable on errors within {@code __hash__}
         */
        @Test
        void where_int() throws TypeError, Throwable {
            assertEqualHashes(intKeyTuples, k -> k.i, k -> k.b, k -> k.py);
        }
    }

    /**
     * Iterate a list of key tuples, which are tuples of Python values,
     * and assert that each member of the tuple has a hash that is equal
     * to the Java {@code hashCode} of the first. {@code keyImpl}
     * specifies which implementations by a sequence of functions that
     * may be given as an argument like {@code k -> k.py}
     *
     * @param <KT> one of the {@code *KeyTuple} classes containing
     *     different realisations of the same value
     * @param keyTuples keys to validate
     * @param keyImpl specifies which implementation to test
     * @throws TypeError if we find an unhashable type
     * @throws Throwable on errors within {@code __hash__}
     */
    @SafeVarargs
    private static <KT> void assertEqualHashes(List<KT> keyTuples, Function<KT, Object>... keyImpl)
            throws TypeError, Throwable {

        for (KT keyTuple : keyTuples) {
            // Get the Java hash of the first key type
            Object value = keyImpl[0].apply(keyTuple);
            int expected = value.hashCode();

            // Each Python hash should be equal to it
            for (Function<KT, Object> ki : keyImpl) {
                // Compare Python hash obtained via abstract object API
                value = ki.apply(keyTuple);
                Object hash = Abstract.hash(value);
                assertEquals(expected, hash);
            }
        }
    }

    @Nested
    @DisplayName("Keys in a dict match every accepted implementation")
    class DictKeysMatch {

        /**
         * Test that for a {@code dict} (a {@link PyDict}) {@code str} keys
         * that are equal act as equal keys. Here we put data in with a
         * {@code String} key.
         */
        @Test
        void when_str_key_is_String() {

            PyDict dict = Py.dict();

            // Insert counter value by the String key
            insertSequentialInts(dict, strKeyTuples, k -> k.s);

            // Retrieve the same value by the various keys
            assertSequentialInts(dict, strKeyTuples, k -> k.s);
            assertSequentialInts(dict, strKeyTuples, k -> k.py);
        }

        /**
         * Test that for a {@code dict} (a {@link PyDict}) {@code str} keys
         * that are equal act as equal keys. Here we put data in with a
         * {@code PyUnicode} key.
         */
        @Test
        void when_str_key_is_PyUnicode() {

            PyDict dict = Py.dict();

            // Insert counter value by the String key
            insertSequentialInts(dict, strKeyTuples, k -> k.py);

            // Retrieve the same value by the various keys
            assertSequentialInts(dict, strKeyTuples, k -> k.s);
            assertSequentialInts(dict, strKeyTuples, k -> k.py);
        }

        /**
         * Test that for a {@code dict} (a {@link PyDict}) {@code int} keys
         * that are equal act as equal keys. Here we put data in with an
         * {@code Integer} key.
         */
        @Test
        void when_int_key_is_Integer() {

            PyDict dict = Py.dict();

            // Insert counter value by the PyLong key
            insertSequentialInts(dict, intKeyTuples, k -> k.py);

            // Retrieve the same value by the various keys
            assertSequentialInts(dict, intKeyTuples, k -> k.py);
            assertSequentialInts(dict, intKeyTuples, k -> k.i);
            assertSequentialInts(dict, intKeyTuples, k -> k.b);
        }

        /**
         * Test that for a {@code dict} (a {@link PyDict}) {@code int} keys
         * that are equal act as equal keys. Here we put data in with a
         * {@code BigInteger} key.
         */
        @Test
        void when_int_key_is_BigInteger() {

            PyDict dict = Py.dict();

            // Insert counter value by the PyLong key
            insertSequentialInts(dict, intKeyTuples, k -> k.b);

            // Retrieve the same value by the various keys
            assertSequentialInts(dict, intKeyTuples, k -> k.b);
            assertSequentialInts(dict, intKeyTuples, k -> k.i);
            assertSequentialInts(dict, intKeyTuples, k -> k.py);
        }

        /**
         * Test that for a {@code dict} (a {@link PyDict}) {@code int} keys
         * that are equal act as equal keys. Here we put data in with a
         * {@code PyLong} key.
         */
        @Test
        void when_int_key_is_PyLong() {

            PyDict dict = Py.dict();

            // Insert counter value by the PyLong key
            insertSequentialInts(dict, intKeyTuples, k -> k.py);

            // Retrieve the same value by the various keys
            assertSequentialInts(dict, intKeyTuples, k -> k.i);
            assertSequentialInts(dict, intKeyTuples, k -> k.b);
            assertSequentialInts(dict, intKeyTuples, k -> k.py);
        }

        /**
         * Test that for a {@code dict} (a {@link PyDict}) {@code int} keys
         * that are equal to {@code bool} values act as equal keys. Here we
         * put data in with a {@code Boolean} key.
         */
        @Test
        void when_int_key_is_Boolean() {

            PyDict dict = Py.dict();

            // Insert counter value by the Boolean key
            int counter = 100;
            dict.put(Boolean.FALSE, counter++);
            dict.put(Boolean.TRUE, counter++);

            // Now try to retrieve as if the values were int 0 and 1
            List<IntKeyTuple> boolKeyTuples = List.of(new IntKeyTuple(0), new IntKeyTuple(1));

            // Retrieve the same value by the various keys
            assertSequentialInts(dict, boolKeyTuples, k -> k.i);
            assertSequentialInts(dict, boolKeyTuples, k -> k.b);
            assertSequentialInts(dict, boolKeyTuples, k -> k.py);
        }

        /**
         * Insert data into a {@link PyDict} in the Python {@code int}
         * sequence {@code 100, 101, 102, ...}. We use a specified
         * acceptable implementation of a the key type under test.
         * {@code keyImpl} specifies which implementation by a function that
         * may be given as an argument like {@code k -> k.py}
         *
         * @param <KT> one of the {@code *KeyTuple} classes containing
         *     different realisations of the same value
         * @param dict to insert to
         * @param keyTuples keys to insert with
         * @param keyImpl specifies which implementation to use
         */
        private <KT extends KeyTuple> void insertSequentialInts(PyDict dict, List<KT> keyTuples,
                Function<KT, Object> keyImpl) {

            int counter = 100;

            for (KT keyTuple : keyTuples) {
                // Insert the value by the specified key type
                Object key = keyImpl.apply(keyTuple);
                dict.put(key, counter++);
            }
        }

        /**
         * Retrieve data from a {@link PyDict}, asserting that it consists
         * of a Python {@code int} sequence {@code 100, 101, 102, ...} . We
         * use keys of the same value (from the same list of key tuples) as
         * was used to make each entry, but in a specified acceptable
         * implementation of a the key type under test. {@code keyImpl}
         * specifies which implementation by a function that may be given as
         * an argument like {@code k -> k.py}
         *
         * @param <KT> one of the {@code *KeyTuple} classes containing
         *     different realisations of the same value
         * @param dict to retrieve from
         * @param keyTuples keys to retrieve with
         * @param keyImpl specifies which implementation to test
         */
        private <KT extends KeyTuple> void assertSequentialInts(PyDict dict, List<KT> keyTuples,
                Function<KT, Object> keyImpl) {

            int counter = 100;

            for (KT keyTuple : keyTuples) {
                // Retrieve the value by the specified key type
                Object key = keyImpl.apply(keyTuple);
                Object value = dict.get(key);

                // The result should be a Python int
                assertNotNull(value, () -> String.format("key %s %s not matched in dict",
                        key.getClass().getSimpleName(), key));
                assertPythonType(PyLong.TYPE, value);

                // And the value should equal the counter
                assertEquals(counter++, value);
            }
        }
    }

    @Nested
    @DisplayName("Expected attributes found")
    class AttrLookupString {

        /**
         * Test that {@link PyType#lookup(String)} and
         * {@link PyType#lookup(String)} retrieve some well-known
         * attributes.
         */
        @Test
        void type_lookup() {
            checkTypeLookup(PyBaseObject.TYPE, "__getattribute__");
            checkTypeLookup(PyLong.TYPE, "__neg__");
            checkTypeLookup(PyUnicode.TYPE, "__add__");
            checkTypeLookup(PyTuple.TYPE, "__repr__");
        }

        /**
         * Check {@code PyType.lookup} succeeds for {@code String} and
         * {@link PyUnicode}.
         *
         * @param type to address
         * @param name to look up
         */
        private void checkTypeLookup(PyType type, String name) {
            Object u, s;
            // Lookup signals no match with null
            assertNotNull(s = type.lookup(name));
            assertNotNull(u = type.lookup(newPyUnicode(name)));
            assertEquals(s, u);
        }

        /**
         * Test that {@link PyType#lookup(String)} and
         * {@link PyType#lookup(String)} retrieve some well-known
         * attributes.
         *
         * @throws Throwable on failure
         */
        @Test
        void abstract_lookupAttr() throws Throwable {
            checkLookupAttr(PyBaseObject.TYPE, "__getattribute__");
            checkLookupAttr(PyLong.TYPE, "__neg__");
            checkLookupAttr(PyUnicode.TYPE, "__add__");
            checkLookupAttr(PyTuple.TYPE, "__repr__");
        }

        /**
         * Check {@code Abstract.lookupAttr} succeeds for {@code String} and
         * {@link PyUnicode}.
         *
         * @param obj to address
         * @param name to look up
         * @throws Throwable on errors
         */
        private void checkLookupAttr(Object obj, String name) throws Throwable {
            Object u, s;
            // lookupAttr signals no match with null
            assertNotNull(s = Abstract.lookupAttr(obj, name));
            assertNotNull(u = Abstract.lookupAttr(obj, newPyUnicode(name)));
            assertEquals(s, u);
        }

        /**
         * Test that {@link PyType#lookup(String)} and
         * {@link PyType#lookup(String)} retrieve some well-known
         * attributes.
         *
         * @throws Throwable on failure
         */
        @Test
        void abstract_getAttr() throws Throwable {
            checkGetAttr(PyBaseObject.TYPE, "__getattribute__");
            checkGetAttr(PyLong.TYPE, "__neg__");
            checkGetAttr(PyUnicode.TYPE, "__add__");
            checkGetAttr(PyTuple.TYPE, "__repr__");
        }

        /**
         * Check {@code Abstract.getAttr} succeeds for {@code String} and
         * {@link PyUnicode}.
         *
         * @param obj to address
         * @param name to look up
         * @throws Throwable on failure
         */
        private void checkGetAttr(Object obj, String name) throws Throwable {
            // GetAttr signals no match with and exception
            Object s = Abstract.getAttr(obj, name);
            Object u = Abstract.getAttr(obj, newPyUnicode(name));
            assertEquals(s, u);
        }
    }
}
