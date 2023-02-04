// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test selected methods of {@link PyList} on a variety of argument
 * types. We can mostly use Java List as a reference, except that,
 * where we need it, the definition of equality must be Python's.
 */
@DisplayName("In PyList")
class PyListTest extends UnitTestSupport {

    /** Base of tests that add, insert and remove elements. */
    abstract static class AbstractInsertionTest {
        /**
         * Provide a stream of examples as parameter sets to the tests of
         * methods that have "insertion" character, that is {@code insert},
         * {@code remove}, assignment to a zero-length slice, etc..
         *
         * @return the examples for search tests.
         */
        static Stream<Arguments> insertionExamples() {
            return Stream.of(//
                    insertionExample(List.of(1, 2, 3, 4, 5), 9, 1), //
                    insertionExample(Collections.emptyList(), 9, 0) //
            );
        }

        /**
         * Construct an insertion (etc.) problem and reference result. This
         * uses Java {@code ArrayList} for the reference answer. The result
         * is an
         * {@code arguments(without, with, front, back, needle, index)}
         * where {@code without} is (a mutable copy of) the corresponding
         * list argument, {@code with} is the same list with {@code needle}
         * inserted at {@code index}, and front and back have the if added
         * first and last respectively. The arguments can be used for
         * testing several types of insert and remove.
         *
         * @param without list without the needle
         * @param needle to insert
         * @param index location to insert
         * @return example data for a test
         */
        private static Arguments insertionExample(List<Object> without, Object needle, int index) {
            ArrayList<Object> with = new ArrayList<>(without);
            with.add(index, needle);
            ArrayList<Object> front = new ArrayList<>(without);
            front.add(0, needle);
            ArrayList<Object> back = new ArrayList<>(without);
            back.add(needle);
            without = new ArrayList<>(without);
            return arguments(without, with, front, back, needle, index);
        }
    }

    /**
     * Tests of several things that amount to insertion of one element.
     */
    @Nested
    @SuppressWarnings("unused")
    class InsertionTest extends AbstractInsertionTest {

        @DisplayName("Java add(i, v)")
        @ParameterizedTest(name = "{0}.add({5}, {4})")
        @MethodSource("insertionExamples")
        void java_add(List<Object> without, List<Object> with, List<Object> front,
                List<Object> back, Object needle, int index) {
            PyList list = new PyList(without);
            list.add(index, needle);
            assertEquals(with, list);
        }

        @DisplayName("Python insert")
        @ParameterizedTest(name = "{0}.insert({5}, {4})")
        @MethodSource("insertionExamples")
        void insert(List<Object> without, List<Object> with, List<Object> front, List<Object> back,
                Object needle, int index) throws Throwable {
            PyList list = new PyList(without);
            list.list_insert(index, needle);
            assertEquals(with, list);
        }

        @DisplayName("Python slice-insert")
        @ParameterizedTest(name = "list[{5}:{5}] = [{4}])")
        @MethodSource("insertionExamples")
        void setitem(List<Object> without, List<Object> with, List<Object> front, List<Object> back,
                Object needle, int index) throws Throwable {
            PyList list = new PyList(without);
            PyList rhs = new PyList(List.of(needle));
            PySlice slice = new PySlice(index, index);
            list.__setitem__(slice, rhs);
            assertEquals(with, list);
        }
    }

    /** Base of tests that sort lists. */
    abstract static class AbstractSortTest {
        /**
         * Provide a stream of examples as parameter sets to the tests of
         * methods that sort the list in various ways.
         *
         * @return the examples for search tests.
         */
        static Stream<Arguments> sortExamples() {
            return Stream.of(//
                    sortExample(List.of(1, 2, 3, 4, 5, 6, 7, 8), 100023), //
                    sortExample(List.of("a", "b", "c", "d", "e"), 420042), //
                    sortExample(List.of(1, 2, 3, 4, 5, 6, 7, 8), 100023), //
                    sortExample(Stream.iterate(0, i -> i < 1000, i -> i + 1)
                            .collect(Collectors.toList()), 555555), //
                    sortExample(List.of(1, 2, 3, 4, 5, 6, 7, 8), 100024, Function.identity()), //
                    sortExample(List.of("python", "anaconda", "boa", "coral snake", "bushmaster"),
                            420042, s -> ((String)s).indexOf('a')), //
                    sortExample(Collections.emptyList(), 1) //
            );
        }

        /**
         * Construct a sort problem and reference result.
         *
         * @param sorted list before randomisation
         * @param seed for randomisation
         * @return example data for a test
         */
        private static Arguments sortExample(List<Object> sorted, long seed) {
            return sortExample(sorted, seed, null);
        }

        /**
         * Construct a sort problem and reference result.
         *
         * @param sorted list before randomisation
         * @param seed for randomisation
         * @return example data for a test
         */
        private static Arguments sortExample(List<Object> sorted, long seed,
                Function<Object, Object> cmp) {
            ArrayList<Object> muddled = new ArrayList<>(sorted);
            randomise(muddled, new Random(seed));
            String mudString = shortString(muddled, 5);
            return arguments(mudString, sorted, muddled, cmp == null ? "null" : "key=f", cmp);
        }

        /**
         * A toString that limits the array size
         *
         * @param a the array to return as a string
         * @param n maximum number of array elements to show
         * @return string representation of {@code a}
         */
        private static String shortString(ArrayList<Object> a, int n) {
            if (a.size() <= n) {
                return a.toString();
            } else {
                String mudString = a.subList(0, n).toString();
                return mudString.substring(0, mudString.length() - 1) + ", ... ]";
            }
        }
    }

    /**
     * Randomise the order of elements in a list.
     *
     * @param m to randomise
     * @param r random generator
     */
    private static void randomise(List<Object> m, Random r) {
        for (int i = m.size() - 1; i > 0; --i) {
            int j = r.nextInt(i + 1);
            if (j != i) {
                // Swap [i] and [j]
                Object temp = m.get(j);
                m.set(j, m.get(i));
                m.set(i, temp);
            }
        }
    }

    /**
     * Tests of {@code list.sort} with key functions and reverse
     * comparison. These are simplistic: tests of change detection and
     * concurrency are needed, and could be added here the CPython
     * regression tests don't do so, or extra fidelity is required,
     * which is often easier under a Java debugger.
     */
    @Nested
    @SuppressWarnings("unused")
    class SortTest extends AbstractSortTest {

        @DisplayName("Normal sort")
        @ParameterizedTest(name = "{0}.sort({3})")
        @MethodSource("sortExamples")
        void normalSort(String mudString, List<Object> sorted, List<Object> muddled,
                String keyString, Function<Object, Object> key) throws Throwable {
            PyList list = new PyList(muddled);
            list.sort(key, false);
            assertEquals(sorted, list);
        }

        @DisplayName("Reverse sort")
        @ParameterizedTest(name = "{0}.sort({3}, reverse=true)")
        @MethodSource("sortExamples")
        void reverseSort(String mudString, List<Object> sorted, List<Object> muddled,
                String keyString, Function<Object, Object> key) throws Throwable {
            PyList list = new PyList(muddled);
            sorted = new ArrayList<>(sorted);
            Collections.reverse(sorted);
            list.sort(key, true);
            assertEquals(sorted, list);
        }
    }
}
