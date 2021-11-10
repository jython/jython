package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collections;
import java.util.List;
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
     * methods that do not mutate their arguments. Each argument object
     * provides a reference value and a test object compatible with the
     * parameterised test methods.
     *
     * @return the examples for non-mutating tests.
     */
    static Stream<Arguments> readableProvider() {
        return Stream.of(//
                tupleExample(Collections.emptyList(), List.of(42)), //
                tupleExample(List.of(42), Collections.emptyList()), //
                tupleExample(List.of(Py.None, 1, PyLong.TYPE),
                        List.of("other", List.of(1,2,3))));
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
    void supports_size(String type, List<Object> ref, Object obj)
            throws Throwable {
        Object r = PySequence.size(obj);
        assertEquals(ref.size(), r);
    }

    /**
     * Test {@link PySequence#getItem(Object, int) PySequence.getItem}.
     * The methods {@code size()} and {@code getItem()} are in a sense
     * fundamental since we shall use them to access members when
     * testing the result of other operations.
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.getItem")
    @ParameterizedTest(name = "{0}: getItem({2}, i)")
    @MethodSource("readableProvider")
    @SuppressWarnings("static-method")
    void supports_getItem(String type, List<Object> ref, Object obj)
            throws Throwable {
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
     * Test {@link PySequence#concat(Object, Object) PySequence.concat}
     *
     * @param type unused (for parameterised name only)
     * @param ref a list having elements equal to those of {@code obj}
     * @param obj Python object under test
     * @throws Throwable from the implementation
     */
    @DisplayName("PySequence.concat")
    @ParameterizedTest(name = "{0}: concat({2}, {4})")
    @MethodSource("readableProvider")
    @SuppressWarnings("static-method")
    void supports_concat(String type, List<Object> ref, Object obj,
            List<Object> ref2, Object obj2) throws Throwable {
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
    void supports_repeat(String type, List<Object> ref, Object obj)
            throws Throwable {
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

    /// **
    // * Test {@link PySequence#setItem(Object, int, Object)
    // * PySequence.setItem}
    // */
    // void supports_setItem(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// **
    // * Test {@link PySequence#delItem(Object, int) PySequence.delItem}
    // */
    // void supports_delItem(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// **
    // * Test {@link PySequence#getSlice(Object, int, int)
    // * PySequence.getSlice}
    // */
    // void supports_getSlice(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// **
    // * Test {@link PySequence#setSlice(Object, int, int, Object)
    // * PySequence.setSlice}
    // */
    // void supports_setSlice(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// **
    // * Test {@link PySequence#delSlice(Object, int, int)
    // * PySequence.delSlice}
    // */
    // void supports_delSlice(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// ** Test {@link PySequence#tuple(Object) PySequence.tuple} */
    // void supports_tuple(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// ** Test {@link PySequence#list(Object) PySequence.list} */
    // void supports_list(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// **
    // * Test {@link PySequence#count(Object, Object) PySequence.count}
    // */
    // void supports_count(List<Object> ref, Object obj) throws
    // Throwable;
    //
    /// **
    // * Test {@link PySequence#contains(Object, Object)
    // * PySequence.contains}
    // */
    // void supports_contains(List<Object> ref, Object obj) throws
    // Throwable;
    //
    //// Not to be confused with PyNumber.index
    /// **
    // * Test {@link PySequence#index(Object, Object) PySequence.index}
    // */
    // void supports_index(List<Object> ref, Object obj) throws
    // Throwable;
}
