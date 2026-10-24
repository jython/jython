// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.internal.Util;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test some convenience methods offered from the {@link PyUtil} class.
 * Tests of methods returning built-in objects do not replace those on
 * the implementation the types themselves.
 */
public class PyUtilTest {

    /**
     * Test some classes with {@link PyUtil#defaultToString(Object)} as
     * their {@code toString()}, which we define to call {@code __str__}, so
     * that the output looks familiar. However, that requires the type to be
     * well-behaved. We test that we get an output even when types
     * misbehave.
     */
    @Nested
    @DisplayName("Test use of PyUtil.defaultToString")
    class TestDefaultToString {
        /** Test PyTuple.toString is its repr. */
        @Test
        void testTuple_toString() {
            assertEquals("()", Py.tuple().toString());
            Object[] x = {1, 2};
            assertEquals("(1, 2)", Py.tuple(x).toString());
            assertEquals("(1,)", Py.tuple(x[0]).toString());
        }

        /** Test PyDict.toString is its repr. */
        @Test
        void testDict_toString() {
            PyDict d = Py.dict();
            assertEquals("{}", d.toString());
            d.put("b", 1);
            assertEquals("{'b': 1}", d.toString());
            d.put("a", Py.dict());
            assertEquals("{'b': 1, 'a': {}}", d.toString());
        }

        /** Test PyException to String for a simple case. */
        @Test
        void testException_toString() {
            PyBaseException e = PyErr.format(PyExc.TypeError, "test");
            assertEquals("TypeError: test", e.toString());
        }

        /**
         * Test PyException repr for a simple case.
         *
         * @throws Throwable
         */
        @Test
        void testException_repr() throws Throwable {
            PyBaseException e = PyErr.format(PyExc.TypeError, "test");
            assertEquals("TypeError('test')", e.__repr__());
        }

        /**
         * Test {@code repr()} falls back to "type at id", when {@code __repr__}
         * and {@code __str__} are undefined.
         *
         * @throws Throwable
         */
        @Test
        void testHasToString_repr() throws Throwable {
            String s = Abstract.repr(new HasToString()).toString();
            assertObjectAt("HasToString", s);
        }

        /**
         * Test {@code str()} falls back to "type at id", when {@code __repr__}
         * and {@code __str__} are undefined.
         *
         * @throws Throwable
         */
        @Test
        void testHasToString_str() throws Throwable {
            String s = Abstract.str(new HasToString()).toString();
            assertObjectAt("HasToString", s);
        }

        /**
         * Test default {@code toString()} falls back to "type at id", when
         * {@code __repr__} and {@code __str__} are undefined.
         */
        @Test
        void testHasToString_toString() {
            String s = (new HasToString()).toString();
            assertObjectAt("HasToString", s);
        }

        /**
         * Test {@code repr()} throws an error, when {@code __repr__} and
         * {@code __str__} throw errors.
         *
         * @throws Throwable
         */
        @Test
        void testBadBad_repr() throws Throwable {
            assertThrows(RuntimeException.class, () -> Abstract.repr(new BadBad()).toString());
        }

        /**
         * Test {@code str()} throws an error, when {@code __repr__} and
         * {@code __str__} throw errors.
         *
         * @throws Throwable
         */
        @Test
        void testBadBad_str() throws Throwable {
            assertThrows(RuntimeException.class, () -> Abstract.str(new BadBad()).toString());
        }

        /**
         * Test default {@code toString()} falls back to "type at id", when
         * {@code __repr__} and {@code __str__} throw errors.
         */
        @Test
        void testBadBad_toString() {
            String s = (new BadBad()).toString();
            assertObjectAt("BadBad", s);
        }

        /**
         * Test {@code repr()} returns what we defined in {@code __repr__}, when
         * {@code __repr__} is defined and {@code __str__} is not.
         *
         * @throws Throwable
         */
        @Test
        void testHasRepr_repr() throws Throwable {
            String s = Abstract.repr(new HasRepr()).toString();
            assertEquals("grim!", s, "repr() is __repr__");
        }

        /**
         * Test {@code str()} returns what we defined in {@code __repr__}, when
         * {@code __repr__} is defined and {@code __str__} is not.
         *
         * @throws Throwable
         */
        @Test
        void testHasRepr_str() throws Throwable {
            String s = Abstract.str(new HasRepr()).toString();
            assertEquals("grim!", s, "str() is __repr__");
        }

        /**
         * Test default {@code toString()} falls back to "type at id", when
         * {@code __repr__} is defined and {@code __str__} is not.
         */
        @Test
        void testHasRepr_toString() {
            String s = (new HasRepr()).toString();
            assertObjectAt("HasRepr", s);
        }

        /**
         * Test {@code repr()} falls back to "type at id", when {@code __str__}
         * is defined and {@code __repr__} is not.
         *
         * @throws Throwable
         */
        @Test
        void testHasStr_repr() throws Throwable {
            String s = Abstract.repr(new HasStr()).toString();
            assertObjectAt("HasStr", s);
        }

        /**
         * Test {@code str()} returns what we defined in {@code __str__}, when
         * {@code __str__} is defined and {@code __repr__} is not.
         *
         * @throws Throwable
         */
        @Test
        void testHasStr_str() throws Throwable {
            String s = Abstract.str(new HasStr()).toString();
            assertEquals("w00t!", s, "str() is __str__");
        }

        /**
         * Test default {@code toString()} returns what we defined in
         * {@code __str__}, when {@code __str__} is defined and {@code __repr__}
         * is not.
         */
        @Test
        void testHasStr_toString() {
            String s = (new HasStr()).toString();
            assertEquals("w00t!", s, "toString() is __str__");
        }

        /**
         * Check the string is "<N object at 0xA>" for a given name N and A a
         * hexadecimal number.
         */
        private static void assertObjectAt(String expectedName, String s) {
            int n = s.length();
            assertEquals('<', s.charAt(0));
            assertEquals('>', s.charAt(n - 1));
            String[] parts = s.substring(1, n - 1).split("\\s+");
            assertEquals(4, parts.length);
            assertEquals(expectedName, parts[0]);
            assertEquals("object", parts[1]);
            assertEquals("at", parts[2]);
            String h = parts[3];
            assertTrue(h.startsWith("0x"));
            Integer.parseInt(h.substring(2), 16);
        }

    }

    /**
     * A Python type that implements {@code toString()} using
     * {@link PyUtil#defaultToString(Object)}, but {@code __str__} and
     * {@code __repr__} are both empty. Every other special method will
     * inherit from {@link PyObject}. The default toString should find
     */
    @SuppressWarnings({"static-method", "unused"})
    private static class HasToString implements WithClass {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("HasToString", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }

        // Undefine __str__ to fail the primary path in defaultToString
        Object __str__() throws Throwable { throw Util.EMPTY_EXCEPTION; }

        // Undefine __repr__ forcing use of object.__repr__
        Object __repr__() throws Throwable { throw Util.EMPTY_EXCEPTION; }

        @Override
        public String toString() { return PyUtil.defaultToString(this); }
    }

    /**
     * Python object that defines {@code __repr__} but {@code __str__} is
     * empty.
     */
    @SuppressWarnings("unused")
    private static class HasRepr extends HasToString {

        static PyType TYPE = PyType.fromSpec(new TypeSpec("HasRepr", MethodHandles.lookup()));

        // Should make no difference to defaultToString
        @Override
        Object __repr__() { return "grim!"; }
    }

    /** Python object that defines __str__. */
    private static class HasStr extends HasToString {

        static PyType TYPE = PyType.fromSpec(new TypeSpec("HasStr", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }

        // Succeeds in primary path in defaultToString
        @Override
        Object __str__() { return "w00t!"; }
    }

    /**
     * A Python type that implements {@code toString()} using
     * {@link PyUtil#defaultToString(Object)}, but {@code __str__} and
     * {@code __repr__} are both faulty. Every other special method will
     * inherit from to {@link PyObject}.
     */
    private static class BadBad extends HasToString {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("BadBad", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }

        // Fails the primary path in defaultToString
        @Override
        Object __str__() { throw new RuntimeException(); }

        // Fails the primary path in defaultToString
        @Override
        Object __repr__() { throw new RuntimeException(); }
    }

}
