// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the {@link PySequence} API class on a variety of types. We are
 * looking for correct behaviour in the cases attempted but mostly
 * testing the invocation of special methods through the operations
 * objects of the particular implementation classes.
 * <p>
 * To reach our main goal, we need only try enough types to exercise
 * every abstract method once in some type.
 */
@DisplayName("In the Abstract API for sequences")
class AbstractSequenceAPITest extends UnitTestSupport {

    /**
     * Provide a stream of examples as parameter sets to the tests of
     * methods that concatenate their arguments (or slice one). Each
     * argument object provides a reference value and a test object
     * compatible with the parameterised test methods.
     *
     * @return the examples for concatenate and slice tests.
     */
    static Stream<Arguments> concatProvider() {
        return Stream.of(//
                bytesConcat("", "abc"), //
                bytesConcat("a", "bc"), //
                bytesConcat("café", " crème"), // bytes > 127
                tupleConcat(Collections.emptyList(), List.of(42)), //
                tupleConcat(List.of(42), Collections.emptyList()), //
                tupleConcat(List.of(-1, 0, 1, 42 * 42, "y", -1e42, 42 * 42),
                        List.of("y", -1, 42 * 42)), //
                tupleConcat(List.of(Py.None, 1, PyLong.TYPE), List.of("other", List.of(1, 2, 3))),
                stringConcat("a", "bc"), //
                stringConcat("", "abc"), //
                stringConcat("Σωκρατικὸς", " λόγος"), //
                unicodeConcat("a", "bc"), //
                unicodeConcat("", "abc"), //
                unicodeConcat("Σωκρατικὸς", " λόγος"), //
                unicodeConcat("画蛇", "添足"), //
                /*
                 * The following contain non-BMP characters 🐍=U+1F40D and 🦓=U+1F993,
                 * each of which Python must consider to be a single character.
                 */
                // In the Java String realisation each is two chars
                stringConcat("one 🐍", "🦓 two"),  // 🐍=\ud83d\udc0d
                stringConcat("🐍🦓", ""), // 🐍=\ud83d\udc0d
                // In the PyUnicode realisation each is one int
                unicodeConcat("one 🐍", "🦓 two"), // 🐍=U+1F40D
                unicodeConcat("🐍🦓", ""),  // 🐍=U+1F40D
                // Surrogate concatenation should not create U+1F40D
                stringConcat("\udc0d A \ud83d", "\udc0d B"),
                unicodeConcat("\udc0d A \ud83d", "\udc0d B") //
        );
    }

    /**
     * Provide a stream of examples as parameter sets to the tests of
     * methods that count or find their arguments. Each argument object
     * provides test objects compatible with the parameterised test methods.
     *
     * @return the examples for concatenate and slice tests.
     */
    static Stream<Arguments> findProvider() {

        return Stream.of(//
                // TODO: test sequence count/find operations
                unicodeFind("abc", "b", 1, 1), unicodeFind("abracadabra", "a", 5, 0), //
                unicodeFind("abracadabra", "r", 2, 2), //
                unicodeFind("abracadabra", "x", 0, 0), //
                unicodeFind("abracadabra", "bra", 0, 0),//
                unicodeFind("123", 3, 0, -1),//
                unicodeFind("画蛇添足", "蛇", 1, 1), //
                tupleFind(List.of(1, 2, 3), 2, 1, 1), //
                tupleFind(List.of(), 42, 0, 0), //
                tupleFind(List.of("a", "b", "c", "b"), "b", 2, 1), //
                tupleFind(List.of(42.0, 42, "42"), 42, 2, 0), //
                listFind(List.of(1, 2, 3), 2, 1, 1), //
                listFind(List.of(), 42, 0, 0), //
                listFind(List.of("a", "b", "c", "b"), "b", 2, 1), //
                listFind(List.of(42.0, 42, "42"), 42, 2, 0) //
        );
    }

    /**
     * Provide a stream of examples as parameter sets to the tests of
     * methods that mutate their arguments (set or delete an item or slice).
     * Each argument object provides test objects compatible with the
     * parameterised test methods.
     *
     * @return the examples for mutation tests.
     */
    static Stream<Arguments> mutationProvider() {
        return Stream.of(//
                listMutate(List.of(1, 2, 3)), //
                listMutate(List.of()), //
                listMutate(List.of("a", "b", "c", "d")), //
                listMutate(Stream.iterate(0, i -> i < 10, i -> i + 1).toList()), //
                listMutate(Stream.iterate(0, i -> i <= 10, i -> i + 1).toList()) //
        );
    }

    // concat-family tests -------------------------------------------

    /**
     * Construct an example with two Python {@code bytes} objects, from
     * text. One is {@code self} in the test, and the other is to be a
     * second argument when needed (for testing {@code concatenation}, say).
     *
     * @param s to encode to bytes ({@code self})
     * @param t to encode to bytes ({@code other})
     * @return the example (a reference value, test object, and other)
     */
    static Arguments bytesConcat(String s, String t) {
        try {
            return bytesConcat(s.getBytes("UTF-8"), t.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            fail("failed to encode bytes");
            return arguments();
        }
    }

    /**
     * Construct an example with two Python {@code bytes} objects, from
     * bytes. One is {@code self} in the test, and the other is to be a
     * second argument when needed (for testing {@code concatenation}, say).
     *
     * @param a the "self" bytes
     * @param b the other bytes
     * @return the example (a reference value, test object, and other)
     */
    static Arguments bytesConcat(byte[] a, byte[] b) {
        ArrayList<Object> vv = new ArrayList<>(a.length);
        for (byte x : a) { vv.add(x & 0xff); }
        ArrayList<Object> ww = new ArrayList<>(b.length);
        for (byte x : b) { ww.add(x & 0xff); }
        Object v = new PyBytes(a), w = new PyBytes(b);
        return arguments(PyType.of(v).getName(), vv, v, ww, w);
    }

    /**
     * Construct an example with two Python {@code tuple}, from arbitrary
     * objects. One is {@code self} in the test, and the other is to be a
     * second argument when needed (for testing {@code concatenation}, say).
     *
     * @param a the objects for {@code self}
     * @param b the objects for the other
     * @return the example (a reference value, test object, and other)
     */
    static Arguments tupleConcat(List<?> a, List<?> b) {
        Object v = new PyTuple(a), w = new PyTuple(b);
        return arguments(PyType.of(v).getName(), a, v, b, w);
    }

    /**
     * Construct an example with two Python {@code str}, each implemented by
     * a Java {@code String}. One is {@code self} in the test, and the other
     * is to be a second argument when needed (for testing
     * {@code concatenation}, say).
     *
     * @param a the String to treat as a Python sequence
     * @param b a second Python sequence as the other argument
     * @return the example (a reference value, test object, and other)
     */
    static Arguments stringConcat(String a, String b) {
        // The sequence element of a str is a str of one char.
        List<Object> aa = listCodePoints(a);
        List<Object> bb = listCodePoints(b);
        return arguments("str(String)", aa, a, bb, b);
    }

    /**
     * Construct an example with two Python {@code str}, each implemented by
     * a {@code PyUnicode}. One is {@code self} in the test, and the other
     * is to be a second argument when needed (for testing
     * {@code concatenation}, say).
     *
     * @param a the String to treat as a Python sequence
     * @param b a second Python sequence as the other argument
     * @return the example (a reference value, test object, and other)
     */
    static Arguments unicodeConcat(String a, String b) {
        // The sequence element of a str is a str of one code point.
        List<Object> vv = listCodePoints(a);
        List<Object> ww = listCodePoints(b);
        Object v = newPyUnicode(a), w = newPyUnicode(b);
        return arguments("str(PyUnicode)", vv, v, ww, w);
    }

    /** Break the String into Python {@code str} code points */
    private static List<Object> listCodePoints(String a) {
        return a.codePoints().mapToObj(PyUnicode::fromCodePoint).collect(Collectors.toList());
    }

    /**
     * Test {@link PySequence#size(Object) PySequence.size}. The methods
     * {@code size()} and {@code getItem()} are in a sense fundamental since
     * we shall use them to access members when testing the result of other
     * operations.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.size")
    @ParameterizedTest(name = "{0}: size({2})")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_size(String type, List<Object> ref, Object obj) throws Throwable {
        Object r = PySequence.size(obj);
        assertEquals(ref.size(), r);
    }

    /**
     * Test {@link PySequence#concat(Object, Object) PySequence.concat}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @param ref2 a list having elements equal to those of {@code obj2}
     * @param obj2 argument to method
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.concat")
    @ParameterizedTest(name = "{0}: concat({2}, {4})")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_concat(String type, List<Object> ref, Object obj, List<Object> ref2, Object obj2)
            throws Throwable {
        Object r = PySequence.concat(obj, obj2);
        final int N = ref.size(), T = ref2.size();
        assertEquals(PyType.of(obj), PyType.of(r)); // Same type
        assertEquals(N + T, PySequence.size(r));    // Right length
        // Now check all the elements (if N+T != 0).
        for (int i = 0; i < N + T; i++) {
            Object e = PySequence.getItem(r, i);
            if (i < N)
                assertEquals(ref.get(i), e);
            else
                assertEquals(ref2.get(i - N), e);
        }
    }

    /**
     * Test {@link PySequence#repeat(Object, int) PySequence.repeat}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.repeat")
    @ParameterizedTest(name = "{0}: repeat({2}, n)")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_repeat(String type, List<Object> ref, Object obj) throws Throwable {
        final int N = ref.size();
        // Try this for a few repeat sizes.
        for (int n = 0; n <= 3; n++) {
            Object r = PySequence.repeat(obj, n);
            assertEquals(PyType.of(obj), PyType.of(r)); // Same type
            assertEquals(N * n, PySequence.size(r));    // Right length
            // Now check all the elements (if n*N != 0).
            for (int i = 0; i < N * n; i++) {
                Object e = PySequence.getItem(r, i);
                assertEquals(ref.get(i % N), e);
            }
        }
    }

    /**
     * Test {@link PySequence#getItem(Object, Object) PySequence.getItem(s,
     * i)} for integer index. The methods {@code size()} and
     * {@code getItem()} are in a sense fundamental since we shall use them
     * to access members when testing the result of other operations.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getItem(int)")
    @ParameterizedTest(name = "{0}: getItem({2}, i)")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_getItem(String type, List<Object> ref, Object obj) throws Throwable {
        final int N = ref.size();
        // Test getting item at each index
        for (int i = 0; i < N; i++) {
            Object r = PySequence.getItem(obj, i);
            assertEquals(ref.get(i), r);
        }
        // And again relative to the end -1...-N
        for (int i = 1; i <= N; i++) {
            Object r = PySequence.getItem(obj, -i);
            assertEquals(ref.get(N - i), r);
        }
        PyType ie = PyExc.IndexError;
        assertRaises(ie, () -> PySequence.getItem(obj, -(N + 1)));
        assertRaises(ie, () -> PySequence.getItem(obj, N));
    }

    /**
     * Test {@link PySequence#getItem(Object, Object) PySequence.getItem(s,
     * [i:j:k])} for slice index.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getItem(slice)")
    @ParameterizedTest(name = "{0}: getItem({2}, slice(p,q,s))")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_getItemSlice(String type, List<Object> ref, Object obj) throws Throwable {
        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        getItemTest(ref, obj, new PySlice(N));
        getItemTest(ref, obj, new PySlice(M));
        getItemTest(ref, obj, new PySlice(0, M, 2));
        getItemTest(ref, obj, new PySlice(M, N));
        // End-relative
        getItemTest(ref, obj, new PySlice(-1));
        getItemTest(ref, obj, new PySlice(M, -1, 2));
        getItemTest(ref, obj, new PySlice(N, -1));
        getItemTest(ref, obj, new PySlice(-1, 0, -2));
        getItemTest(ref, obj, new PySlice(-1, M, -2));
        getItemTest(ref, obj, new PySlice(-1, N));
        // Out of bounds
        getItemTest(ref, obj, new PySlice(-1000, 1000));
        getItemTest(ref, obj, new PySlice(-1000, 1000, 3));
        getItemTest(ref, obj, new PySlice(-1000, M));
        getItemTest(ref, obj, new PySlice(M, 1000));
    }

    /**
     * Test {@link PySequence#getSlice(Object, int, int)}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getSlice")
    @ParameterizedTest(name = "{0}: getSlice({2}, p, q)")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_getSlice(String type, List<Object> ref, Object obj) throws Throwable {
        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        getSliceTest(ref, obj, 0, N);
        getSliceTest(ref, obj, 0, M);
        getSliceTest(ref, obj, M, N);
        // End-relative
        getSliceTest(ref, obj, 0, -1);
        getSliceTest(ref, obj, M, -1);
        getSliceTest(ref, obj, N, -1);
        getSliceTest(ref, obj, -1, 0);
        getSliceTest(ref, obj, -1, M);
        getSliceTest(ref, obj, -1, N);
        // Out of bounds
        getSliceTest(ref, obj, -1000, 1000);
        getSliceTest(ref, obj, -1000, M);
        getSliceTest(ref, obj, M, 1000);
    }

    /**
     * Perform one test of
     * {@link #supports_getItemSlice(String, List, Object)} with given
     * slice.
     *
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @param s index slice
     * @throws Throwable from the implementation
     */
    private static void getItemTest(List<Object> ref, Object obj, PySlice s) throws Throwable {
        // Use library to decode s, but check constraints
        PySlice.Indices i = s.new Indices(ref.size());
        if (i.slicelength == 0) {
            // CPython does not guarantee, but our logic does
            assertEquals(i.start, i.stop, "start==stop");
        } else if (i.step > 0) {
            // stop index consistent with addressing equation
            assertTrue(i.stop > i.start);
            assertTrue(i.stop <= i.start + i.slicelength * i.step);
        } else if (i.step < 0) {
            // stop index consistent with addressing equation
            assertTrue(i.stop < i.start);
            assertTrue(i.stop >= i.start + i.slicelength * i.step);
        }
        // Now check the actual method we're testing
        Object result = PySequence.getItem(obj, s);
        getSliceCheck(result, ref, obj, i.start, i.stop, i.step);
    }

    /**
     * A test of {@link PySequence#getSlice(Object, int, int)} with given
     * indices.
     *
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @param p start index
     * @param q stop index (exclusive)
     * @throws Throwable from the implementation
     */
    private static void getSliceTest(List<Object> ref, Object obj, int p, int q) throws Throwable {
        Object result = PySequence.getSlice(obj, p, q);
        getSliceCheck(result, ref, obj, p, q, 1);
    }

    /**
     * Check the result of a slice result against items obtained by indexing
     * a reference list according to the indices.
     *
     * @param result of invocation
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @param p start index
     * @param q stop index (exclusive)
     * @param s index step
     * @throws Throwable from the implementation
     */
    private static void getSliceCheck(Object result, List<Object> ref, Object obj, int p, int q,
            int s) throws Throwable {

        final int N = ref.size();

        // Deal with end-relative addressing of the source sequence
        if (p < 0) { p = p + N; }
        if (q < 0) { q = q + N; }

        // Effective indices are the bounded version of each
        p = Math.max(Math.min(p, N), 0);
        q = Math.max(Math.min(q, N), 0);

        // Form expected result by stepping naïvely through ref
        List<Object> expected = new ArrayList<>();
        for (int i = p; i >= 0 && i < N; i += s) {
            // Check we have not passed q in the direction of travel
            if (s > 0 && i >= q || s < 0 && i <= q) { break; }
            expected.add(ref.get(i));
        }

        // Check the result slice against the reference
        assertEquals(PyType.of(obj), PyType.of(result)); // Same type
        final int M = expected.size();
        assertEquals(M, PySequence.size(result));    // Right length
        for (int i = 0; i < M; i++) {
            Object e = PySequence.getItem(result, i);
            assertEquals(expected.get(i), e);
        }
    }

    /**
     * Test {@link PySequence#tuple(Object) PySequence.tuple}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.tuple")
    @ParameterizedTest(name = "{0}: tuple({2})")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_tuple(String type, List<Object> ref, Object obj) throws Throwable {
        PyTuple result = PySequence.tuple(obj);
        checkItems(ref, result);
    }

    /**
     * Test {@link PySequence#list(Object) PySequence.list}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.list")
    @ParameterizedTest(name = "{0}: list({2})")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_list(String type, List<Object> ref, Object obj) throws Throwable {
        PyList result = PySequence.list(obj);
        checkItems(ref, result);
    }

    /**
     * Test {@link PySequence#list(Object) PySequence.list}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.fastList (Java API)")
    @ParameterizedTest(name = "{0}: fastList({2})")
    @MethodSource("concatProvider")
    @SuppressWarnings("static-method")
    void supports_fastList(String type, List<Object> ref, Object obj) throws Throwable {
        List<Object> result = PySequence.fastList(obj, () -> PyErr.format(PyExc.ValueError, ""));
        checkItems(ref, result);
    }

    /**
     * Check a test result for size and content. The result must be a Java
     * List<Object>.
     *
     * @param ref a list having elements expected of {@code result}
     * @param result Python object under test
     * @throws Throwable from the implementation
     */
    private static void checkItems(List<Object> ref, List<Object> result) throws Throwable {
        int L = ref.size();
        assertEquals(L, result.size());
        for (int i = 0; i < L; i++) { assertEquals(ref.get(i), result.get(i)); }
    }

    // find-family tests -------------------------------------------

    /**
     * Construct an example with a Python {@code str} and an {@code object}
     * for testing {@code count}, {@code contains} and {@code index}, and
     * the reference answers for {@code count} and {@code index}.
     * {@code str} is implemented by a {@code PyUnicode}.
     *
     * @param o the String to treat as a Python sequence
     * @param value to find
     * @param count number of occurrences of {@code value}
     * @param index of first occurrence (ignored if {@code count==0})
     * @return the example (a reference value, test object, and others)
     */
    static Arguments unicodeFind(String o, Object value, int count, int index) {
        // The sequence element of a str is a str of one code point.
        List<Object> oo = listCodePoints(o);
        if (count == 0) { index = -1; }
        return arguments("str(PyUnicode)", o, oo, value, count, index);
    }

    /**
     * Construct an example with a Python {@code tuple} and an
     * {@code object} for testing {@code count}, {@code contains} and
     * {@code index}, and the reference answers for {@code count} and
     * {@code index}.
     *
     * @param oo the values for the {@code tuple} Python sequence
     * @param value to find
     * @param count number of occurrences of {@code value}
     * @param index of first occurrence (ignored if {@code count==0})
     * @return the example (a reference value, test object, and others)
     */
    static Arguments tupleFind(List<Object> oo, Object value, int count, int index) {
        // Supply a tuple made from the reference List.
        PyTuple o = PyTuple.from(oo);
        if (count == 0) { index = -1; }
        return arguments("tuple", o, oo, value, count, index);
    }

    /**
     * Construct an example with a Python {@code list} and an {@code object}
     * for testing {@code count}, {@code contains} and {@code index}, and
     * the reference answers for {@code count} and {@code index}.
     *
     * @param oo the values for the {@code list} Python sequence
     * @param value to find
     * @param count number of occurrences of {@code value}
     * @param index of first occurrence (ignored if {@code count==0})
     * @return the example (a reference value, test object, and others)
     */
    static Arguments listFind(List<Object> oo, Object value, int count, int index) {
        // Supply a Python list made from the reference List.
        PyList o = new PyList(oo);
        if (count == 0) { index = -1; }
        return arguments("list", o, oo, value, count, index);
    }

    /**
     * Test {@link PySequence#count(Object, Object) PySequence.count}
     *
     * @param type unused (for parameterised name only)
     * @param o object under test
     * @param oo the values for the {@code list} Python sequence
     * @param value to find
     * @param count number of occurrences of {@code value}
     * @param index of first occurrence (ignored if {@code count==0})
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.count")
    @ParameterizedTest(name = "{0}: count({1}, {3})")
    @MethodSource("findProvider")
    @SuppressWarnings("static-method")
    void supports_count(String type, Object o, List<Object> oo, Object value, int count, int index)
            throws Throwable {
        int r = PySequence.count(o, value);
        assertEquals(count, r);
    }

    /**
     * Test {@link PySequence#contains(Object, Object) PySequence.contains}
     *
     * @param type unused (for parameterised name only)
     * @param o object under test
     * @param oo the values for the {@code list} Python sequence
     * @param value to find
     * @param count number of occurrences of {@code value}
     * @param index of first occurrence (ignored if {@code count==0})
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.contains")
    @ParameterizedTest(name = "{0}: contains({1}, {3})")
    @MethodSource("findProvider")
    @SuppressWarnings("static-method")
    void supports_contains(String type, Object o, List<Object> oo, Object value, int count,
            int index) throws Throwable {
        boolean r = PySequence.contains(o, value);
        assertEquals(count > 0, r);
    }

    // Not to be confused with PyNumber.index
    /**
     * Test {@link PySequence#index(Object, Object) PySequence.index}
     *
     * @param type unused (for parameterised name only)
     * @param o object under test
     * @param oo the values for the {@code list} Python sequence
     * @param value to find
     * @param count number of occurrences of {@code value}
     * @param index of first occurrence (ignored if {@code count==0})
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.index")
    @ParameterizedTest(name = "{0}: index({1}, {3})")
    @MethodSource("findProvider")
    @SuppressWarnings("static-method")
    void supports_index(String type, Object o, List<Object> oo, Object value, int count, int index)
            throws Throwable {
        try {
            int r = PySequence.index(o, value);
            assertEquals(index, r);
        } catch (PyBaseException e) {
            assertSame(PyExc.ValueError, e.getType());
        }
    }

    /**
     * Construct an example with a Python {@code list} for tests involving
     * slice assignment and deletion.
     *
     * @param <T> Type of element in {@code ref} (ignored)
     * @param ref the values for the {@code list} Python sequence
     * @return the example (a test object and a reference value)
     */
    static <T> Arguments listMutate(List<T> ref) {
        // Supply a factory the test can use to make a Python list
        Function<List<Object>, Object> factory = new Function<List<Object>, Object>() {
            @Override
            public PyList apply(List<Object> a) { return new PyList(a); }
        };
        return arguments("list", ref, factory);
    }

    /**
     * Test {@link PySequence#delItem(Object, Object) PySequence.delItem(s,
     * i)} for integer index. This test depends on {@code getItem()}.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.delItem(int)")
    @ParameterizedTest(name = "{0}: delItem({1}, i)")
    @MethodSource("mutationProvider")
    @SuppressWarnings("static-method")
    void supports_delItem(String type, List<Object> ref, Function<List<Object>, Object> f)
            throws Throwable {
        final int N = ref.size();
        // Delete at indices 0...N-1
        for (int i = 0; i < N; i++) {
            Object obj = f.apply(ref);
            PySequence.delItem(obj, i);
            // Look at obj before an after the deletion point
            Object obj1 = PySequence.getSlice(obj, 0, i);
            Object obj2 = PySequence.getSlice(obj, i, N - 1);
            // Expect to match the corresponding parts of ref
            assertEquals(ref.subList(0, i), obj1);
            assertEquals(ref.subList(i + 1, N), obj2);
        }
        // And again relative to the end -1...-N
        for (int i = 1; i <= N; i++) {
            Object obj = f.apply(ref);
            PySequence.delItem(obj, -i);
            // Look at obj before an after the deletion point
            Object obj1 = PySequence.getSlice(obj, 0, N - i);
            Object obj2 = PySequence.getSlice(obj, N - i, N - 1);
            // Expect to match the corresponding parts of ref
            assertEquals(ref.subList(0, N - i), obj1);
            assertEquals(ref.subList(N - i + 1, N), obj2);
        }
        PyType ie = PyExc.IndexError;
        Object obj = f.apply(ref);
        assertRaises(ie, () -> PySequence.delItem(obj, -(N + 1)));
        assertRaises(ie, () -> PySequence.delItem(obj, N));
    }

    /**
     * Test {@link PySequence#delSlice(Object, int, int)}. This test depends
     * on {@code getItem()}.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.delItem(slice)")
    @ParameterizedTest(name = "{0}: delItem({1}, [i:j:k])")
    @MethodSource("mutationProvider")
    @SuppressWarnings("static-method")
    void supports_delItemSlice(String type, List<Object> ref, Function<List<Object>, Object> f)
            throws Throwable {
        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        delItemTest(ref, f, new PySlice(N));
        delItemTest(ref, f, new PySlice(M));
        delItemTest(ref, f, new PySlice(0, M, 2));
        delItemTest(ref, f, new PySlice(M, N));
        // End-relative
        delItemTest(ref, f, new PySlice(-1));
        delItemTest(ref, f, new PySlice(M, -1, 2));
        delItemTest(ref, f, new PySlice(N, -1));
        delItemTest(ref, f, new PySlice(-1, 0, -2));
        delItemTest(ref, f, new PySlice(-1, M, -2));
        delItemTest(ref, f, new PySlice(-1, N));
        // Out of bounds
        delItemTest(ref, f, new PySlice(-1000, 1000));
        delItemTest(ref, f, new PySlice(-1000, 1000, 3));
        delItemTest(ref, f, new PySlice(-1000, M));
        delItemTest(ref, f, new PySlice(M, 1000));
    }

    /**
     * Test {@link PySequence#delSlice(Object, int, int)
     * PySequence.delSlice}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.delSlice")
    @ParameterizedTest(name = "{0}: delSlice({1}, p, q)")
    @MethodSource("mutationProvider")
    @SuppressWarnings("static-method")
    void supports_delSlice(String type, List<Object> ref, Function<List<Object>, Object> f)
            throws Throwable {
        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        delSliceTest(ref, f, 0, N);
        delSliceTest(ref, f, 0, M);
        delSliceTest(ref, f, M, N);
        // End-relative
        delSliceTest(ref, f, 0, -1);
        delSliceTest(ref, f, M, -1);
        delSliceTest(ref, f, N, -1);
        delSliceTest(ref, f, -1, 0);
        delSliceTest(ref, f, -1, M);
        delSliceTest(ref, f, -1, N);
        // Out of bounds
        delSliceTest(ref, f, -1000, 1000);
        delSliceTest(ref, f, -1000, M);
        delSliceTest(ref, f, M, 1000);
    }

    /**
     * Perform one test of {@link #supports_delSlice(String, List, Object)}
     * with given slice.
     *
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @param s index slice
     * @throws Throwable from the implementation
     */
    private static void delItemTest(List<Object> ref, Function<List<Object>, Object> f, PySlice s)
            throws Throwable {
        // Use library to decode s, but check constraints
        PySlice.Indices i = s.new Indices(ref.size());
        if (i.slicelength == 0) {
            // CPython does not guarantee, but our logic does
            assertEquals(i.start, i.stop, "start==stop");
        } else if (i.step > 0) {
            // stop index consistent with addressing equation
            assertTrue(i.stop > i.start);
            assertTrue(i.stop <= i.start + i.slicelength * i.step);
        } else if (i.step < 0) {
            // stop index consistent with addressing equation
            assertTrue(i.stop < i.start);
            assertTrue(i.stop >= i.start + i.slicelength * i.step);
        }
        // Now check the actual method we're testing
        Object obj = f.apply(ref);
        PySequence.delItem(obj, s);
        delSliceCheck(ref, obj, i.start, i.stop, i.step);
    }

    /**
     * A test of {@link PySequence#delSlice(Object, int, int)} with given
     * indices.
     *
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @param p start index
     * @param q stop index (exclusive)
     * @throws Throwable from the implementation
     */
    private static void delSliceTest(List<Object> ref, Function<List<Object>, Object> f, int p,
            int q) throws Throwable {
        Object obj = f.apply(ref);
        PySequence.delSlice(obj, p, q);
        delSliceCheck(ref, obj, p, q, 1);
    }

    /**
     * Check a slice deletion result against a reference list deleted at the
     * same places.
     *
     * @param ref a list having elements equal original {@code obj}
     * @param obj Python object under test (mutated)
     * @param p start index
     * @param q stop index (exclusive)
     * @param s index step
     * @throws Throwable from the implementation
     */
    private static void delSliceCheck(List<Object> ref, Object obj, int p, int q, int s)
            throws Throwable {

        final int N = ref.size();

        // Deal with end-relative addressing of the source sequence
        if (p < 0) { p = p + N; }
        if (q < 0) { q = q + N; }

        // Effective indices are the bounded version of each
        p = Math.max(Math.min(p, N), 0);
        q = Math.max(Math.min(q, N), 0);

        // Form list of deletions by stepping through a copy
        List<Integer> deletions = new ArrayList<>();
        for (int i = p;; i += s) {
            // Check we have not passed q in the direction of travel
            if (s > 0 && i >= q || s < 0 && i <= q) { break; }
            deletions.add(i);
        }

        // Now delete (in a copy) working from high to low.
        List<Object> expected = new ArrayList<>(ref);
        deletions.sort(Comparator.reverseOrder());
        for (int i : deletions) { expected.remove(i); }

        // Check the result slice against the reference
        final int M = expected.size();
        assertEquals(M, PySequence.size(obj));    // Right length
        for (int i = 0; i < M; i++) {
            Object e = PySequence.getItem(obj, i);
            assertEquals(expected.get(i), e);
        }
    }

    /**
     * Test {@link PySequence#setItem(Object, Object, Object)
     * PySequence.setItem(s, i, v)} for integer index. This test depends on
     * {@code getItem()}.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.setItem(int)")
    @ParameterizedTest(name = "{0}: setItem({1}, i, v)")
    @MethodSource("mutationProvider")
    @SuppressWarnings("static-method")
    void supports_setItem(String type, List<Object> ref, Function<List<Object>, Object> f)
            throws Throwable {
        final int N = ref.size();
        // Assign at indices 0...N-1
        for (int i = 0; i < N; i++) {
            Object obj = f.apply(ref);
            Object v = i * 100;
            PySequence.setItem(obj, i, v);
            // Look at obj before an after the assigned element
            Object obj1 = PySequence.getSlice(obj, 0, i);
            Object obj2 = PySequence.getSlice(obj, i + 1, N);
            // Expect to match the corresponding parts of ref
            assertEquals(ref.subList(0, i), obj1);
            assertEquals(v, PySequence.getItem(obj, i));
            assertEquals(ref.subList(i + 1, N), obj2);
        }
        // And again relative to the end -1...-N
        for (int i = 1; i <= N; i++) {
            Object obj = f.apply(ref);
            Object v = i * 100;
            PySequence.setItem(obj, -i, v);
            // Look at obj before an after the assigned element
            Object obj1 = PySequence.getSlice(obj, 0, N - i);
            Object obj2 = PySequence.getSlice(obj, N - i + 1, N);
            // Expect to match the corresponding parts of ref
            assertEquals(ref.subList(0, N - i), obj1);
            assertEquals(v, PySequence.getItem(obj, N - i));
            assertEquals(ref.subList(N - i + 1, N), obj2);
        }
        PyType ie = PyExc.IndexError;
        Object obj = f.apply(ref);
        assertRaises(ie, () -> PySequence.setItem(obj, -(N + 1), 42));
        assertRaises(ie, () -> PySequence.setItem(obj, N, 42));
    }

    /**
     * Test {@link PySequence#setItem(Object, Object, Object)}. This test
     * depends on {@code getItem()}.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.setItem(slice)")
    @ParameterizedTest(name = "{0}: setItem({1}, [i:j:k])")
    @MethodSource("mutationProvider")
    @SuppressWarnings("static-method")
    void supports_setItemSlice(String type, List<Object> ref, Function<List<Object>, Object> f)
            throws Throwable {
        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        setItemTest(ref, f, new PySlice(N), pattern(N - 2));
        setItemTest(ref, f, new PySlice(M), pattern(M + 2));
        setItemTest(ref, f, new PySlice(0, M, 2), pattern((M + 1) / 2));
        setItemTest(ref, f, new PySlice(M, N), pattern(N - M - 2));
        // End-relative
        setItemTest(ref, f, new PySlice(-1), pattern(N + 2));
        setItemTest(ref, f, new PySlice(M, -1, 2), pattern((N - M) / 2));
        setItemTest(ref, f, new PySlice(N, -1), pattern(0));
        setItemTest(ref, f, new PySlice(-1, 0, -2), pattern(N / 2));
        setItemTest(ref, f, new PySlice(-1, M, -2), pattern((N - M) / 2));
        setItemTest(ref, f, new PySlice(-1, N), pattern(3));
        // Out of bounds
        setItemTest(ref, f, new PySlice(-1000, 1000), pattern(3));
        setItemTest(ref, f, new PySlice(-1000, 1000, 3), pattern((N + 2) / 3));
        setItemTest(ref, f, new PySlice(-1000, M), pattern(3));
        setItemTest(ref, f, new PySlice(M, 1000), pattern(3));
    }

    /**
     * Test {@link PySequence#setSlice(Object, int, int, Object)
     * PySequence.setSlice}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.setSlice")
    @ParameterizedTest(name = "{0}: setSlice({1}, p, q)")
    @MethodSource("mutationProvider")
    @SuppressWarnings("static-method")
    void supports_setSlice(String type, List<Object> ref, Function<List<Object>, Object> f)
            throws Throwable {
        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        setSliceTest(ref, f, 0, N, pattern(N + 2));
        setSliceTest(ref, f, 0, M, pattern(M - 2));
        setSliceTest(ref, f, M, N, pattern(M + 2));
        // End-relative
        setSliceTest(ref, f, 0, -1, pattern(N - 1));
        setSliceTest(ref, f, M, -1, pattern(N - M - 1));
        setSliceTest(ref, f, N, -1, pattern(0));
        setSliceTest(ref, f, -1, 0, pattern(0));
        setSliceTest(ref, f, -1, M, pattern(N - M - 1));
        setSliceTest(ref, f, -1, N, pattern(1));
        // Out of bounds
        setSliceTest(ref, f, -1000, 1000, pattern(N));
        setSliceTest(ref, f, -1000, M, pattern(M));
        setSliceTest(ref, f, M, 1000, pattern(N - M));
    }

    /**
     * Make a Python list to assign to a slice, with a distinctive pattern
     * to aid debugging.
     *
     * @param L length of pattern (effectively zero if negative)
     * @return the pattern as a list
     */
    private static List<Object> pattern(int L) {
        List<Object> pattern = new LinkedList<>();
        for (int i = 0; i < L; i++) { pattern.add(i + 100); }
        return new PyList(pattern);
    }

    /**
     * Perform one test of {@link #supports_setSlice(String, List, Object)}
     * with given slice.
     *
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @param s index slice
     * @throws Throwable from the implementation
     */
    private static void setItemTest(List<Object> ref, Function<List<Object>, Object> f, PySlice s,
            List<Object> v) throws Throwable {
        // Use library to decode s, but check constraints
        PySlice.Indices i = s.new Indices(ref.size());
        if (i.slicelength == 0) {
            // CPython does not guarantee, but our logic does
            assertEquals(i.start, i.stop, "start==stop");
        } else if (i.step > 0) {
            // stop index consistent with addressing equation
            assertTrue(i.stop > i.start);
            assertTrue(i.stop <= i.start + i.slicelength * i.step);
        } else if (i.step < 0) {
            // stop index consistent with addressing equation
            assertTrue(i.stop < i.start);
            assertTrue(i.stop >= i.start + i.slicelength * i.step);
        }
        // In an extended slice, must match (or test is bad)
        assert i.step == 1 || i.slicelength == v.size();
        // Now check the actual method we're testing
        Object obj = f.apply(ref);
        PySequence.setItem(obj, s, v);
        setSliceCheck(ref, obj, i.start, i.stop, i.step, v);
    }

    /**
     * A test of {@link PySequence#setSlice(Object, int, int)} with given
     * indices.
     *
     * @param ref a list having elements specifying the test object
     * @param f factory for Python object under test
     * @param p start index
     * @param q stop index (exclusive)
     * @throws Throwable from the implementation
     */
    private static void setSliceTest(List<Object> ref, Function<List<Object>, Object> f, int p,
            int q, List<Object> v) throws Throwable {
        Object obj = f.apply(ref);
        PySequence.setSlice(obj, p, q, v);
        setSliceCheck(ref, obj, p, q, 1, v);
    }

    /**
     * Check a slice assignment result against a reference list mutated at
     * the same places.
     *
     * @param ref a list having elements equal original {@code obj}
     * @param obj Python object under test (mutated)
     * @param p start index
     * @param q stop index (exclusive)
     * @param s index step
     * @param v values to assign to slice
     * @throws Throwable from the implementation
     */
    private static void setSliceCheck(List<Object> ref, Object obj, int p, int q, int s,
            List<Object> v) throws Throwable {

        final int N = ref.size();

        // Deal with end-relative addressing of the source sequence
        if (p < 0) { p = p + N; }
        if (q < 0) { q = q + N; }

        // Effective indices are the bounded version of each
        p = Math.max(Math.min(p, N), 0);
        q = Math.max(Math.min(q, N), 0);

        ArrayList<Object> expected = new ArrayList<>();
        if (s == 1) {
            // A contiguous slice: replace slice with v
            for (int i = 0; i < p; i++) { expected.add(ref.get(i)); }
            expected.addAll(v);
            for (int i = Math.max(q, p); i < N; i++) { expected.add(ref.get(i)); }
        } else {
            // An extended slice: replace at indices generated
            expected.addAll(ref);
            int count = 0;
            for (int i = p;; i += s) {
                // Check we have not passed q in the direction of travel
                if (s > 0 && i >= q || s < 0 && i <= q) { break; }
                expected.set(i, v.get(count++));
            }
            // Test is incorrect if we did not use all of v
            assert count == v.size();
        }

        // Check the result slice against the reference
        final int M = expected.size();
        assertEquals(M, PySequence.size(obj));    // Right length
        for (int i = 0; i < M; i++) {
            Object e = PySequence.getItem(obj, i);
            assertEquals(expected.get(i), e);
        }
    }
}
