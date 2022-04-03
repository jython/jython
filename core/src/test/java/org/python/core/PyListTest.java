package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.python.base.MissingFeature;
import org.python.core.PyType.Spec;

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
         * methods that have "search" character, that is {@code find},
         * {@code index}, {@code partition}, {@code count}, etc..
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

    /** Tests of {@code str.find} operating on the whole string. */
    @Nested
    // @DisplayName("insertion")
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

}
