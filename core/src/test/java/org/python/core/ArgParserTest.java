// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * This is a test of {@link ArgParser}. Since it is quite a
 * complicated beast, and that might make it fragile, we try to be
 * thorough here, rather than wait for it to let us down inside some
 * complicated Python built-in called in an unforeseen way.
 * <p>
 * Each nested test class provides one parser specification to all
 * its test methods, which then exercise that parser in a range of
 * circumstances. As far as possible, we use the same test names
 * when testing the same kind of behaviour.
 */
class ArgParserTest {

    abstract static class Standard {

        /**
         * A parser should have field values that correctly reflect the
         * arguments used in its construction.
         */
        abstract void has_expected_fields();

        /**
         * A parser should obtain the correct result (and not throw) when
         * applied to classic arguments matching its specification.
         */
        abstract void parses_classic_args();

        /**
         * {@link ArgParser#toString()} matches its specification.
         */
        @Test
        abstract void has_expected_toString();
    }

    @Nested
    @DisplayName("A parser for no arguments")
    class NoArgs extends Standard {

        ArgParser ap = ArgParser.fromSignature("func");

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(0, ap.argnames.length);
            assertEquals(0, ap.argcount);
            assertEquals(0, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(0, ap.regargcount);
            assertEquals(-1, ap.varArgsIndex);
            assertEquals(-1, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = PyTuple.EMPTY;
            PyDict kwargs = Py.dict();

            // It's enough that this not throw
            ap.parse(args, kwargs);
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals("func()", ap.toString()); }
    }

    @Nested
    @DisplayName("A parser for positional arguments")
    class PositionalArgs extends Standard {

        ArgParser ap = ArgParser.fromSignature("func", "a", "b", "c");

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(3, ap.argnames.length);
            assertEquals(3, ap.argcount);
            assertEquals(0, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(3, ap.regargcount);
            assertEquals(-1, ap.varArgsIndex);
            assertEquals(-1, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = Py.tuple(1, 2, 3);
            PyDict kwargs = Py.dict();

            Object[] frame = ap.parse(args, kwargs);
            assertArrayEquals(new Object[] {1, 2, 3}, frame);
        }

        @Test
        void parses_classic_kwargs() {
            PyTuple args = Py.tuple(1);
            PyDict kwargs = Py.dict();
            kwargs.put("c", 3);
            kwargs.put("b", 2);

            Object[] frame = ap.parse(args, kwargs);
            assertArrayEquals(new Object[] {1, 2, 3}, frame);
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals("func(a, b, c)", ap.toString()); }
    }

    @Nested
    @DisplayName("A parser for positional-only arguments")
    class PositionalOnlyArgs extends Standard {

        ArgParser ap = ArgParser.fromSignature("func", "a", "b", "c", "/");

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(3, ap.argnames.length);
            assertEquals(3, ap.argcount);
            assertEquals(3, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(3, ap.regargcount);
            assertEquals(-1, ap.varArgsIndex);
            assertEquals(-1, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = Py.tuple(1, 2, 3);
            PyDict kwargs = Py.dict();

            Object[] frame = ap.parse(args, kwargs);
            assertArrayEquals(new Object[] {1, 2, 3}, frame);
        }

        @Test
        void raises_TypeError_on_kwargs() {
            PyTuple args = Py.tuple(1);
            PyDict kwargs = Py.dict();
            kwargs.put("c", 3);
            kwargs.put("b", 2);

            assertThrows(TypeError.class, () -> ap.parse(args, kwargs));
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals("func(a, b, c, /)", ap.toString()); }
    }

    @Nested
    @DisplayName("A parser for some positional-only arguments")
    class SomePositionalOnlyArgs extends Standard {

        ArgParser ap = ArgParser.fromSignature("func", "a", "b", "/", "c");

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(3, ap.argnames.length);
            assertEquals(3, ap.argcount);
            assertEquals(2, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(3, ap.regargcount);
            assertEquals(-1, ap.varArgsIndex);
            assertEquals(-1, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = Py.tuple(1, 2, 3);
            PyDict kwargs = Py.dict();

            Object[] frame = ap.parse(args, kwargs);
            assertArrayEquals(new Object[] {1, 2, 3}, frame);
        }

        @Test
        void throws_when_arg_missing() {
            PyTuple args = Py.tuple(1);
            PyDict kwargs = Py.dict();
            kwargs.put("c", 3);
            assertThrows(TypeError.class, () -> ap.parse(args, kwargs));
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals("func(a, b, /, c)", ap.toString()); }
    }

    @Nested
    @DisplayName("A parser for a positional collector")
    class PositionalCollector extends Standard {

        ArgParser ap = ArgParser.fromSignature("func", "*aa");

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(1, ap.argnames.length);
            assertEquals(0, ap.argcount);
            assertEquals(0, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(0, ap.regargcount);
            assertEquals(0, ap.varArgsIndex);
            assertEquals(-1, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = Py.tuple(1, 2, 3);
            PyDict kwargs = Py.dict();

            Object[] frame = ap.parse(args, kwargs);
            assertEquals(1, frame.length);
            assertEquals(List.of(1, 2, 3), frame[0]);
        }

        @Test
        void throws_on_keyword() {
            PyTuple args = Py.tuple(1);
            PyDict kwargs = Py.dict();
            kwargs.put("c", 3);
            assertThrows(TypeError.class, () -> ap.parse(args, kwargs));
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals("func(*aa)", ap.toString()); }
    }

    @Nested
    @DisplayName("A parser for a keyword collector")
    class KeywordCollector extends Standard {

        ArgParser ap = ArgParser.fromSignature("func", "**kk");

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(1, ap.argnames.length);
            assertEquals(0, ap.argcount);
            assertEquals(0, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(0, ap.regargcount);
            assertEquals(-1, ap.varArgsIndex);
            assertEquals(0, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = Py.tuple();
            PyDict kwargs = Py.dict();
            kwargs.put("b", 2);
            kwargs.put("c", 3);
            kwargs.put("a", 1);

            Object[] frame = ap.parse(args, kwargs);
            assertEquals(1, frame.length);
            PyDict kk = (PyDict)frame[0];
            assertEquals(1, kk.get("a"));
            assertEquals(2, kk.get("b"));
            assertEquals(3, kk.get("c"));
        }

        @Test
        void throws_on_positional() {
            PyTuple args = Py.tuple(1);
            PyDict kwargs = Py.dict();
            kwargs.put("b", 2);
            kwargs.put("c", 3);
            kwargs.put("a", 1);
            assertThrows(TypeError.class, () -> ap.parse(args, kwargs));
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals("func(**kk)", ap.toString()); }
    }

    @Nested
    @DisplayName("Example from the Javadoc")
    class FromJavadoc extends Standard {

        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "aa", "kk"};
        ArgParser ap = new ArgParser("func", names, names.length - 2, 4, 3, true, true) //
                .defaults(3, 4, 5, 6) //
                .kwdefaults(77, null, 99);
        private String SIG = "func(a, b, c=3, d=4, /, e=5, f=6, *aa, g=77, h, i=99, **kk)";

        @Override
        @Test
        void has_expected_fields() {
            assertEquals("func", ap.name);
            assertEquals(11, ap.argnames.length);
            assertEquals(6, ap.argcount);
            assertEquals(4, ap.posonlyargcount);
            assertEquals(3, ap.kwonlyargcount);
            assertEquals(9, ap.regargcount);
            assertEquals(9, ap.varArgsIndex);
            assertEquals(10, ap.varKeywordsIndex);
        }

        @Override
        @Test
        void parses_classic_args() {
            PyTuple args = Py.tuple(10, 20, 30);
            PyDict kwargs = Py.dict();
            kwargs.put("g", 70);
            kwargs.put("h", 80);

            PyTuple expectedTuple = PyTuple.EMPTY;
            PyDict expectedDict = Py.dict();
            Object[] expected =
                    new Object[] {10, 20, 30, 4, 5, 6, 70, 80, 99, expectedTuple, expectedDict};

            Object[] frame = ap.parse(args, kwargs);
            assertArrayEquals(expected, frame);
        }

        /**
         * When the keyword defaults are replaced with a client-supplied
         * {@code dict}, the new values take effect.
         */
        @Test
        void parses_classic_args_kwmap() {
            PyTuple args = Py.tuple(10, 20, 30);
            PyDict kwargs = Py.dict();
            kwargs.put("g", 70);

            PyDict kwd = Py.dict();
            kwd.put("h", 28);
            kwd.put("i", 29);
            ap.kwdefaults(kwd);

            PyTuple expectedTuple = PyTuple.EMPTY;
            PyDict expectedDict = Py.dict();
            Object[] expected =
                    new Object[] {10, 20, 30, 4, 5, 6, 70, 28, 29, expectedTuple, expectedDict};

            Object[] frame = ap.parse(args, kwargs);
            assertArrayEquals(expected, frame);
        }

        @Override
        @Test
        void has_expected_toString() { assertEquals(SIG, ap.toString()); }
    }
}
