// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link PyMethodDescr}s on a variety of types. The particular
 * operations are not the focus: we are testing the mechanisms for
 * creating and calling method descriptors from methods defined in Java.
 * <p>
 * An example the type of object under test is:<pre>
 * &gt;&gt;&gt; str.replace
 * &lt;method 'replace' of 'str' objects&gt;
 * </pre> and its bound counterpart <pre>
 * &gt;&gt;&gt; "hello".replace
 * &lt;built-in method replace of str object at 0x000001EC5935A3B0&gt;
 * </pre>
 */
class MethodDescriptorTest extends UnitTestSupport {

    @Nested
    @DisplayName("Check some basic Exposer properties")
    class ExposerBasic {

        /**
         * An instance method with no arguments.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_isascii() throws Throwable {
            PyMethodDescr isascii = (PyMethodDescr)PyUnicode.TYPE.lookup("isascii");

            assertEquals("isascii", isascii.name);
            assertEquals(PyUnicode.TYPE, isascii.objclass);

            // Check the parser
            ArgParser ap = isascii.argParser;
            assertEquals("isascii", ap.name);
            assertEquals(0, ap.regargcount);
        }

        /**
         * An instance method with no arguments and an object return.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_lower() throws Throwable {
            PyMethodDescr lower = (PyMethodDescr)PyUnicode.TYPE.lookup("lower");

            assertEquals("lower", lower.name);
            assertEquals(PyUnicode.TYPE, lower.objclass);

            // Check the parser
            ArgParser ap = lower.argParser;
            assertEquals("lower", ap.name);
            assertEquals(0, ap.regargcount);
        }

        /**
         * An instance method with one argument and object return.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_zfill() throws Throwable {
            PyMethodDescr zfill = (PyMethodDescr)PyUnicode.TYPE.lookup("zfill");

            assertEquals("zfill", zfill.name);
            assertEquals(PyUnicode.TYPE, zfill.objclass);

            // Check the parser
            ArgParser ap = zfill.argParser;
            assertEquals("zfill", ap.name);
            assertEquals(1, ap.regargcount);
            assertEquals("width", ap.argnames[0]);
        }

        /**
         * An instance method with two object arguments. (There should be an
         * optional {@code int} but it isn't implemented.)
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_replace() throws Throwable {
            PyMethodDescr replace = (PyMethodDescr)PyUnicode.TYPE.lookup("replace");

            assertEquals("replace", replace.name);
            assertEquals(PyUnicode.TYPE, replace.objclass);

            // Check the parser
            ArgParser ap = replace.argParser;
            assertEquals("replace", ap.name);
            assertEquals(3, ap.regargcount);
            assertEquals("old", ap.argnames[0]);
            assertEquals("new", ap.argnames[1]);
            assertEquals("count", ap.argnames[2]);
        }

        /**
         * An instance method with an int and an optional object argument.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_ljust() throws Throwable {
            PyMethodDescr ljust = (PyMethodDescr)PyUnicode.TYPE.lookup("ljust");

            assertEquals("ljust", ljust.name);
            assertEquals(PyUnicode.TYPE, ljust.objclass);

            // Check the parser
            ArgParser ap = ljust.argParser;
            String sig = "ljust($self, width, fillchar=' ', /)";
            assertEquals(sig, ap.toString());

            assertEquals("ljust", ap.name);
            assertEquals(2, ap.regargcount);
            assertEquals("width", ap.argnames[0]);
            assertEquals("fillchar", ap.argnames[1]);
            assertEquals(2, ap.posonlyargcount);
        }
    }

    @Nested
    @DisplayName("Call built-in method via descriptor")
    class CallBuiltin {

        /**
         * An instance method with no arguments and a primitive return.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_isascii() throws Throwable {
            PyMethodDescr isascii = (PyMethodDescr)PyUnicode.TYPE.lookup("isascii");

            boolean even = true;
            for (String s : List.of("Hej!", "¡Hola!")) {
                PyUnicode u = newPyUnicode(s);
                for (Object o : List.of(s, u)) {
                    Object r = isascii.__call__(new Object[] {o}, null);
                    assertEquals(even, r);
                }
                even = !even;
            }
        }

        /**
         * An instance method with no arguments and an object return.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_lower() throws Throwable {
            PyMethodDescr lower = (PyMethodDescr)PyUnicode.TYPE.lookup("lower");

            for (String s : List.of("1. MiXeD", "2. cAsE")) {
                PyUnicode u = newPyUnicode(s);
                PyUnicode e = newPyUnicode(s.toLowerCase());
                for (Object o : List.of(s, u)) {
                    Object r = lower.__call__(new Object[] {o}, null);
                    assertEquals(e, r);
                }
            }
        }

        /**
         * An instance method with one argument.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_zfill() throws Throwable {
            PyMethodDescr zfill = (PyMethodDescr)PyUnicode.TYPE.lookup("zfill");

            String s = "-123";
            PyUnicode u = newPyUnicode(s);

            for (Object o : List.of(s, u)) {
                Object r = zfill.__call__(new Object[] {o, 6}, null);
                assertEquals("-00123", r.toString());
            }
        }

        /**
         * An instance method with two object arguments and an optional
         * {@code int}.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_replace() throws Throwable {
            PyMethodDescr replace = (PyMethodDescr)PyUnicode.TYPE.lookup("replace");

            String s = "hello";
            PyUnicode u = newPyUnicode(s);

            // Test with count taking default value
            for (Object o : List.of(s, u)) {
                Object r = replace.__call__(new Object[] {o, "ell", "ipp"}, null);
                assertEquals("hippo", r.toString());
            }

            // Test with count explicit
            for (Object o : List.of(s, u)) {
                Object r = replace.__call__(new Object[] {o, "l", "", 1}, null);
                assertEquals("helo", r.toString());
            }
        }

        /**
         * An instance method with an int and an optional object argument.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void str_ljust() throws Throwable {
            PyMethodDescr ljust = (PyMethodDescr)PyUnicode.TYPE.lookup("ljust");

            String s = "hello";
            PyUnicode u = newPyUnicode(s);

            // Test with fill character explicit
            for (Object o : List.of(s, u)) {
                Object r = ljust.__call__(new Object[] {o, 8, "*"}, null);
                assertEquals("hello***", r.toString());
            }

            // Test with fill character taking default value
            for (Object o : List.of(s, u)) {
                Object r = ljust.__call__(new Object[] {o, 7}, null);
                assertEquals("hello  ", r.toString());
            }
        }
    }
}
