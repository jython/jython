package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.python.core.Exposed.KeywordCollector;
import org.python.core.Exposed.PositionalCollector;
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
@DisplayName("The Fake built-in type ...")
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

        // Signature: (a, b, /, *c)
        @PythonStaticMethod
        static PyTuple f2v(int a, String b, @PositionalCollector PyTuple c) {
            return Py.tuple(a, b, c);
        }

        // Signature: ($self, a, b, /, *c)
        @PythonMethod
        PyTuple m2v(int a, String b, @PositionalCollector PyTuple c) { return Py.tuple(a, b, c); }

        // Signature: (a, b, /, *c)
        @PythonStaticMethod
        static PyTuple f2pvk(int a, String b, @PositionalCollector PyTuple c,
                @KeywordCollector PyDict d) {
            return Py.tuple(a, b, c, d);
        }

        // Signature: ($self, a, b, /, *c)
        @PythonMethod
        PyTuple m2pvk(int a, String b, @PositionalCollector PyTuple c, @KeywordCollector PyDict d) {
            return Py.tuple(a, b, c, d);
        }

    }

    /**
     * We collect the method specifications here during set-up for
     * examination in tests.
     */
    static Map<String, CallableSpec> methods = new TreeMap<>();

    /**
     * Set-up method filling {@link #methods}.
     */
    @BeforeAll
    static void createExposer() {
        // type=null in order not to wake the type system
        TypeExposer exposer = Exposer.exposeType(null, Fake.class, null);

        // Populate the dictionaries used in the tests.
        for (Exposer.Spec s : exposer.specs.values()) {
            if (s instanceof CallableSpec) {
                CallableSpec ms = (CallableSpec)s;
                methods.put(ms.name, ms);
            }
        }
    }

    /**
     * Check that a method, member or get-set for a given name.
     *
     * @param dict of members
     * @param name of member
     * @return the spec (for further checks)
     */
    private static <S extends Exposer.Spec> S find(Map<String, S> dict, String name) {
        S spec = dict.get(name);
        assertNotNull(spec, () -> name + " not found");
        return spec;
    }

    // ----------------------------------------------------------------
    @Test
    @DisplayName("has the expected number of methods.")
    void numberOfMethods() { assertEquals(12, methods.size(), "number of methods"); }

    /**
     * Check that a method with the expected signature is in the method
     * table.
     *
     * @param sig signature
     */
    @DisplayName("has a method with signature ...")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { //
            "f0()", //
            "m0($self, /)", //
            "f3(a, b, c, /)", //
            "m3($self, a, b, c, /)", //
            "f3pk(a, b, c)", //
            "m3pk($self, /, a, b, c)", //
            "f3p2(a, b, /, c)", //
            "m3p2($self, a, b, /, c)", //
            "f2v(a, b, /, *c)", //
            "m2v($self, a, b, /, *c)", //
            "f2pvk(a, b, /, *c, **d)", //
            "m2pvk($self, a, b, /, *c, **d)", //
    })
    void checkSignature(String sig) {
        int k = sig.indexOf('(');
        assert k > 0;
        String name = sig.substring(0, k);
        String expect = sig.substring(k);
        CallableSpec ms = find(methods, name);
        ArgParser ap = ms.getParser();
        assertEquals(expect, ap.textSignature());
    }
}
