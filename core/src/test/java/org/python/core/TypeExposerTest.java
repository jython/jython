package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.python.core.Exposed.PositionalOnly;
import org.python.core.Exposed.PythonMethod;
import org.python.core.Exposed.PythonStaticMethod;
import org.python.core.Exposer.CallableSpec;

/**
 * Test that the annotations defined in {@link Exposed}, and
 * intended for exposing attributes of a type defined in Java, are
 * processed correctly by a {@link Exposer} to a {@link TypeExposer}
 * containing appropriate attribute specifications. This tests a
 * large part of the exposure mechanism, without activating the
 * wider Python type system.
 */
@DisplayName("For a type exposed from a Java definition")
class TypeExposerTest {

    /**
     * This class is not actually a Python type definition, but is
     * annotated as if it were. We will test whether the type dictionary
     * is filled as expected.
     *
     * Methods named {@code m*()} are instance methods to Python,
     * declared to Java as instance methods ({@code this} is
     * {@code self}).
     *
     * Methods named {@code f*()} are static methods to Python (no
     * {@code self}), declared to Java as static methods.
     */
    static class Fake {

        static final Lookup LOOKUP = MethodHandles.lookup();

        // Instance methods -------------------------------------------

        // Signature: (/)
        @PythonStaticMethod
        static void f0() {}

        // Signature: ($self, /)
        @PythonMethod
        void m0() {}

        // Signature: (a, b, c /)
        @PythonStaticMethod
        static PyTuple f3(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: ($self, a, b, c /)
        @PythonMethod
        PyTuple m3(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: (/, a, b, c)
        @PythonStaticMethod(positionalOnly = false)
        static PyTuple f3pk(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: ($self, /, a, b, c)
        @PythonMethod(positionalOnly = false)
        PyTuple m3pk(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: (a, b, /, c)
        @PythonStaticMethod
        static PyTuple f3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: ($self, a, b, /, c)
        @PythonMethod
        PyTuple m3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(a, b, c); }
    }

    @Nested
    @DisplayName("calling the Exposer")
    class TestExposer {

        @Test
        @DisplayName("produces a TypeExposer")
        void getExposer() {
            TypeExposer exposer = Exposer.exposeType(null, Fake.class, null);
            assertNotNull(exposer);
        }

        @Test
        @DisplayName("finds the expected methods")
        void getMethodSignatures() {
            // type=null in order not to wake the type system
            TypeExposer exposer = Exposer.exposeType(null, Fake.class, null);
            // Fish out those things that are methods
            Map<String, ArgParser> dict = new TreeMap<>();
            for (Exposer.Spec s : exposer.specs.values()) {
                if (s instanceof CallableSpec) {
                    CallableSpec ms = (CallableSpec)s;
                    dict.put(ms.name, ms.getParser());
                }
            }
            checkMethodSignatures(dict);
        }

        private void checkMethodSignatures(Map<String, ArgParser> dict) {
            assertEquals(8, dict.size());
            /*
             * If the names of the arguments have a synthetic look (arg0, arg1,
             * ...), it is because the compiler is not preserving the names of
             * method parameters for use by reflection. The build script
             * specifies "options.compilerArgs.add('-parameters')", but you may
             * have to tick the corresponding box in any IDE under which you try
             * to debug this test.
             */
            checkSignature(dict, "f0()");
            checkSignature(dict, "m0($self, /)");
            checkSignature(dict, "f3(a, b, c, /)");
            checkSignature(dict, "m3($self, a, b, c, /)");
            checkSignature(dict, "f3pk(a, b, c)");
            checkSignature(dict, "m3pk($self, /, a, b, c)");
            checkSignature(dict, "f3p2(a, b, /, c)");
            checkSignature(dict, "m3p2($self, a, b, /, c)");
        }

        /**
         * Check that a method with the expected signature is in the
         * dictionary.
         *
         * @param dict dictionary
         * @param spec signature
         */
        private void checkSignature(Map<String, ArgParser> dict, String spec) {
            int k = spec.indexOf('(');
            assertTrue(k > 0);
            String name = spec.substring(0, k);
            String expect = spec.substring(k);
            ArgParser ap = dict.get(name);
            assertNotNull(ap, () -> name + " not found");
            assertEquals(expect, ap.textSignature());
        }
    }
}
