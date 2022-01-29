package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the {@link PySequence} API class on a variety of types. We
 * are looking for correct behaviour in the cases attempted but
 * mostly testing the invocation of special methods through the
 * operations objects of the particular implementation classes.
 * <p>
 * To reach our main goal, we need only try enough types to exercise
 * every abstract method once in some type.
 */
@DisplayName("In the Abstract API for sequences")
class AbstractSequenceAPITest extends UnitTestSupport {

    /**
     * Provide a stream of examples as parameter sets to the tests of
     * methods that search or concatenate their arguments. Each argument
     * object provides a reference value and a test object compatible
     * with the parameterised test methods.
     *
     * @return the examples for non-mutating tests.
     */
    static Stream<Arguments> readableProvider() {
        return Stream.of(//
                tupleExample(Collections.emptyList(), List.of(42)), //
                tupleExample(List.of(42), Collections.emptyList()), //
                tupleExample( //
                        List.of(-1, 0, 1, 42 * 42, "y", -1e42, 42 * 42), //
                        List.of("y", -1, 42 * 42)),
                tupleExample(List.of(Py.None, 1, PyLong.TYPE), List.of("other", List.of(1, 2, 3))),
                stringExample("a", "bc"), //
                stringExample("", "abc"), //
                stringExample("Σωκρατικὸς", " λόγος"), //
                unicodeExample("a", "bc"), //
                unicodeExample("", "abc"), //
                unicodeExample("Σωκρατικὸς", " λόγος"), //
                unicodeExample("画蛇", "添足"), //
                /*
                 * The following contain non-BMP characters 🐍=U+1F40D and
                 * 🦓=U+1F993, each of which Python must consider to be a single
                 * character.
                 */
                // In the Java String realisation each is two chars
                stringExample("one 🐍", "🦓 two"),  // 🐍=\ud83d\udc0d
                stringExample("🐍🦓", ""), // 🐍=\ud83d\udc0d
                // In the PyUnicode realisation each is one int
                unicodeExample("one 🐍", "🦓 two"), // 🐍=U+1F40D
                unicodeExample("🐍🦓", ""),  // 🐍=U+1F40D
                // Surrogate concatenation should not create U+1F40D
                stringExample("\udc0d A \ud83d", "\udc0d B"),
                unicodeExample("\udc0d A \ud83d", "\udc0d B"));
    }

    /**
     * Construct an example with two Python {@code tuple}, from
     * arbitrary objects. One is {@code self} in the test, and the other
     * is to be a second argument when needed (for testing
     * {@code concatenation}, say).
     *
     * @param a the objects for {@code self}
     * @param b the objects for the other
     * @return the example (a reference value, test object, and other)
     */
    static Arguments tupleExample(List<?> a, List<?> b) {
        Object v = new PyTuple(a), w = new PyTuple(b);
        return arguments(PyType.of(v).name, a, v, b, w);
    }

    /**
     * Construct an example with two Python {@code str}, each
     * implemented by a Java {@code String}. One is {@code self} in the
     * test, and the other is to be a second argument when needed (for
     * testing {@code concatenation}, say).
     *
     * @param a the String to treat as a Python sequence
     * @param b a second Python sequence as the other argument
     * @return the example (a reference value, test object, and other)
     */
    static Arguments stringExample(String a, String b) {
        // The sequence element of a str is a str of one char.
        List<Object> aa = listCodePoints(a);
        List<Object> bb = listCodePoints(b);
        return arguments("str(String)", aa, a, bb, b);
    }

    /**
     * Construct an example with two Python {@code str}, each
     * implemented by a {@code PyUnicode}. One is {@code self} in the
     * test, and the other is to be a second argument when needed (for
     * testing {@code concatenation}, say).
     *
     * @param a the String to treat as a Python sequence
     * @param b a second Python sequence as the other argument
     * @return the example (a reference value, test object, and other)
     */
    static Arguments unicodeExample(String a, String b) {
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
     * {@code size()} and {@code getItem()} are in a sense fundamental
     * since we shall use them to access members when testing the result
     * of other operations.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.size")
    @ParameterizedTest(name = "{0}: size({2})")
    @MethodSource("readableProvider")
    @SuppressWarnings("static-method")
    void supports_size(String type, List<Object> ref, Object obj) throws Throwable {
        Object r = PySequence.size(obj);
        assertEquals(ref.size(), r);
    }

    /**
     * Test {@link PySequence#getItem(Object, Object)
     * PySequence.getItem} for integer index. The methods {@code size()}
     * and {@code getItem()} are in a sense fundamental since we shall
     * use them to access members when testing the result of other
     * operations.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getItem(int)")
    @ParameterizedTest(name = "{0}: getItem({2}, i)")
    @MethodSource("readableProvider")
    @SuppressWarnings("static-method")
    void supports_getItem(String type, List<Object> ref, Object obj) throws Throwable {
        final int N = ref.size();
        for (int i = 0; i < N; i++) {
            Object r = PySequence.getItem(obj, i);
            assertEquals(ref.get(i), r);
        }
        // And again relative to the end -1...-N
        for (int i = 1; i <= N; i++) {
            Object r = PySequence.getItem(obj, -i);
            assertEquals(ref.get(N - i), r);
        }
        Class<IndexError> ie = IndexError.class;
        assertThrows(ie, () -> PySequence.getItem(obj, -(N + 1)));
        assertThrows(ie, () -> PySequence.getItem(obj, N));
    }

    /**
     * Test {@link PySequence#getItem(Object, Object)
     * PySequence.getItem} for slice index.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getItem(slice)")
    @ParameterizedTest(name = "{0}: getItem({2}, slice(p,q,s))")
    @MethodSource("readableProvider")
    @SuppressWarnings("static-method")
    void supports_getItemSlice(String type, List<Object> ref, Object obj) throws Throwable {

        // Get size and locate middle
        final int N = ref.size(), M = (N + 1) / 2;
        getItemTest(ref, obj, new PySlice(0, N));
        getItemTest(ref, obj, new PySlice(0, M));
        getItemTest(ref, obj, new PySlice(0, M, 2));
        getItemTest(ref, obj, new PySlice(M, N));
        // End-relative
        getItemTest(ref, obj, new PySlice(0, -1));
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
        sliceCheck(result, ref, obj, i.start, i.stop, i.step);
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
    @MethodSource("readableProvider")
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
    @MethodSource("readableProvider")
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

    // /**
    // * Test {@link PySequence#setItem(Object, int, Object)
    // * PySequence.setItem}
    // */
    // void supports_setItem(String type, List<Object> ref, Object obj)
    // throws Throwable{fail("not implemented");}
    //
    // /**
    // * Test {@link PySequence#delItem(Object, int) PySequence.delItem}
    // */
    // void supports_delItem(String type, List<Object> ref, Object obj)
    // throws Throwable{fail("not implemented");}

    /**
     * Test {@link PySequence#getSlice(Object, int, int)
     * PySequence.getSlice}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getSlice")
    @ParameterizedTest(name = "{0}: getSlice({2}, p, q)")
    @MethodSource("readableProvider")
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
     * {@link #supports_getSlice(String, List, Object)} with given
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
        sliceCheck(result, ref, obj, p, q, 1);
    }

    /**
     * Check a slice result against items obtained by indexing a
     * reference list.
     *
     * @param result of invocation
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @param p start index
     * @param q stop index (exclusive)
     * @param s index step
     * @throws Throwable from the implementation
     */
    private static void sliceCheck(Object result, List<Object> ref, Object obj, int p, int q, int s)
            throws Throwable {

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

    // /**
    // * Test {@link PySequence#setSlice(Object, int, int, Object)
    // * PySequence.setSlice}
    // */
    // void supports_setSlice(String type, List<Object> ref, Object obj)
    // throws Throwable {fail("not implemented");}
    //
    // /**
    // * Test {@link PySequence#delSlice(Object, int, int)
    // * PySequence.delSlice}
    // */
    // void supports_delSlice(String type, List<Object> ref, Object obj)
    // throws Throwable {fail("not implemented");}

    // XXX AWaits implementation of PySequence.tuple
    // /**
    // * Test {@link PySequence#tuple(Object) PySequence.tuple}
    // *
    // * @param type unused (for parameterised name only)
    // * @param ref a list having elements equal to those of {@code obj}
    // * @param obj Python object under test
    // * @throws Throwable from the implementation
    // */
    // @DisplayName("PySequence.tuple")
    // @ParameterizedTest(name = "{0}: tuple({2})")
    // @MethodSource("readableProvider")
    // @SuppressWarnings("static-method")
    // void supports_tuple(String type, List<Object> ref, Object obj)
    // throws Throwable {
    // PyTuple result = PySequence.tuple(obj);
    // checkItems(ref, result);
    // }

    // XXX AWaits implementation of PySequence.tuple
    // /**
    // * Test {@link PySequence#list(Object) PySequence.list}
    // *
    // * @param type unused (for parameterised name only)
    // * @param ref a list having elements equal to those of {@code obj}
    // * @param obj Python object under test
    // * @throws Throwable from the implementation
    // */
    // @DisplayName("PySequence.list")
    // @ParameterizedTest(name = "{0}: list({2})")
    // @MethodSource("readableProvider")
    // @SuppressWarnings("static-method")
    // void supports_list(String type, List<Object> ref, Object obj)
    // throws Throwable {
    // PyList result = PySequence.list(obj);
    // checkItems(ref, result);
    // }

    /**
     * Check a test result for size and content. The result must allow
     * indexing with {@link PySequence#getItem(Object, Object)}.
     *
     * @param ref a list having elements expected of {@code result}
     * @param result Python object under test
     * @throws Throwable from the implementation
     */
    private static void checkItems(List<Object> ref, Object result) throws Throwable {
        int L = ref.size();
        assertEquals(L, PySequence.size(result));
        for (int i = 0; i < L; i++) { assertEquals(ref.get(i), PySequence.getItem(result, i)); }
    }

    /**
     * Test {@link PySequence#list(Object) PySequence.list}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @Disabled("Missing feature: fastList() from  iterable or sequence (str/bytes)")
    @DisplayName("PySequence.fastList (Java API)")
    @ParameterizedTest(name = "{0}: fastList({2})")
    @MethodSource("readableProvider")
    @SuppressWarnings("static-method")
    void supports_fastList(String type, List<Object> ref, Object obj) throws Throwable {
        List<Object> result = PySequence.fastList(obj, () -> new ValueError(""));
        checkItems(ref, result);
    }

    /**
     * Check a test result for size and content. The result must be a
     * Java List<Object>.
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

    // /**
    // * Test {@link PySequence#count(Object, Object) PySequence.count}
    // */
    // void supports_count(String type, List<Object> ref, Object obj)
    // throws Throwable {fail("not implemented");}
    //
    // /**
    // * Test {@link PySequence#contains(Object, Object)
    // * PySequence.contains}
    // */
    // void supports_contains(String type, List<Object> ref, Object obj)
    // throws Throwable {fail("not implemented");}
    //
    // // Not to be confused with PyNumber.index
    // /**
    // * Test {@link PySequence#index(Object, Object) PySequence.index}
    // */
    // void supports_index(String type, List<Object> ref, Object obj)
    // throws Throwable {fail("not implemented");}

}
