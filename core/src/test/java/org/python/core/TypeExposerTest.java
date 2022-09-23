package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.python.core.Exposed.Deleter;
import org.python.core.Exposed.DocString;
import org.python.core.Exposed.Getter;
import org.python.core.Exposed.KeywordCollector;
import org.python.core.Exposed.Member;
import org.python.core.Exposed.PositionalCollector;
import org.python.core.Exposed.PositionalOnly;
import org.python.core.Exposed.PythonMethod;
import org.python.core.Exposed.PythonStaticMethod;
import org.python.core.Exposed.Setter;
import org.python.core.Exposer.CallableSpec;
import org.python.core.TypeExposer.GetSetSpec;
import org.python.core.TypeExposer.MemberSpec;

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
        @SuppressWarnings("static-method")
        @PythonMethod
        PyTuple m3(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: (/, a, b, c)
        @PythonStaticMethod(positionalOnly = false)
        static PyTuple f3pk(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: ($self, /, a, b, c)
        @SuppressWarnings("static-method")
        @PythonMethod(positionalOnly = false)
        PyTuple m3pk(int a, String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: (a, b, /, c)
        @PythonStaticMethod
        static PyTuple f3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: ($self, a, b, /, c)
        @SuppressWarnings("static-method")
        @PythonMethod
        PyTuple m3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(a, b, c); }

        // Signature: (a, b, /, *c)
        @PythonStaticMethod
        static PyTuple f2v(int a, String b, @PositionalCollector PyTuple c) {
            return Py.tuple(a, b, c);
        }

        // Signature: ($self, a, b, /, *c)
        @SuppressWarnings("static-method")
        @PythonMethod
        PyTuple m2v(int a, String b, @PositionalCollector PyTuple c) { return Py.tuple(a, b, c); }

        // Signature: (a, b, /, *c)
        @PythonStaticMethod
        static PyTuple f2pvk(int a, String b, @PositionalCollector PyTuple c,
                @KeywordCollector PyDict d) {
            return Py.tuple(a, b, c, d);
        }

        // Signature: ($self, a, b, /, *c)
        @SuppressWarnings("static-method")
        @PythonMethod
        PyTuple m2pvk(int a, String b, @PositionalCollector PyTuple c, @KeywordCollector PyDict d) {
            return Py.tuple(a, b, c, d);
        }

        // Instance members -------------------------------------------

        // Plain int
        @Member
        int i;

        // Plain float (with doc string)
        @Member
        @DocString("Doc string for x")
        double x;

        // String with change of name.
        @Member("text")
        String t;

        // String can be properly deleted without popping up as None
        @Member(optional = true)
        String s;

        // Arbitrary object
        @Member
        Object obj;

        // Read-only by annotation
        @Member(readonly = true)
        int i2;

        // Read-only by final.
        @Member
        final double x2 = 1.0;

        // Read-only by annotation given before name change
        @Member(readonly = true, value = "text2")
        String t2;

        // String again (?)
        @Member(readonly = true)
        PyUnicode strhex2;

        // Instance attributes ----------------------------------------

        // Read-only (but changes to count updates to foo
        int count = 0;
        // Writable, but cannot delete
        String foo;
        // Writable, and has delete operation
        double thingValue;

        @Getter
        Object count() { return count; }

        @Getter
        Object foo() { return thingValue; }

        @Setter
        void foo(Object v) throws TypeError, Throwable {
            try {
                foo = (String)v;
            } catch (ClassCastException cce) {
                foo = "<invalid>";
            }
        }

        @Getter
        Object thing() { return thingValue; }

        @Setter("thing")
        void thing(Object v) throws TypeError, Throwable {
            try {
                thingValue = (Double)v;
            } catch (ClassCastException cce) {
                thingValue = Double.NaN;
            }
            count += 1;
        }

        @Deleter("thing")
        void deleteThing() throws TypeError, Throwable {
            thingValue = Double.NaN;
            count = 0;
        }
    }

    /**
     * We collect the method specifications here during set-up for
     * examination in tests.
     */
    static Map<String, CallableSpec> methods = new TreeMap<>();
    /**
     * We collect the member specifications here during set-up for
     * examination in tests.
     */
    static Map<String, MemberSpec> members = new TreeMap<>();
    /**
     * We collect the get-set attribute specifications here during
     * set-up for examination in tests.
     */
    static Map<String, GetSetSpec> getsets = new TreeMap<>();

    /**
     * Set-up method filling {@link #methods}, {@link #members} and
     * {@link #getsets}.
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
            } else if (s instanceof MemberSpec) {
                MemberSpec ms = (MemberSpec)s;
                members.put(ms.name, ms);
            } else if (s instanceof GetSetSpec) {
                GetSetSpec gs = (GetSetSpec)s;
                getsets.put(gs.name, gs);
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
    @SuppressWarnings("static-method")
    void numberOfMethods() { assertEquals(12, methods.size(), "number of methods"); }

    /**
     * Check that a method with the expected signature is in the method
     * table.
     *
     * @param sig signature
     */
    @ParameterizedTest(name = "{0}")
    @DisplayName("has a method with signature ...")
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
    @SuppressWarnings("static-method")
    void checkSignature(String sig) {
        int k = sig.indexOf('(');
        assert k > 0;
        String name = sig.substring(0, k);
        String expect = sig.substring(k);
        CallableSpec ms = find(methods, name);
        ArgParser ap = ms.getParser();
        assertEquals(expect, ap.textSignature());
    }

    // ----------------------------------------------------------------
    @Test
    @DisplayName("has the expected number of members.")
    @SuppressWarnings("static-method")
    void numberOfMembers() { assertEquals(9, members.size(), "number of members"); }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a writable member ...")
    @ValueSource(strings = { //
            "i", //
            "x", //
            "text", // name for t
            "s", //
            "obj", //
    })
    @SuppressWarnings("static-method")
    void checkWritableMember(String name) {
        MemberSpec ms = find(members, name);
        assertFalse(ms.readonly, () -> name + " readonly");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a readonly member ...")
    @ValueSource(strings = { //
            "i2", //
            "x2", //
            "text2", //
            "strhex2", //
    })
    @SuppressWarnings("static-method")
    void checkReadonlyMember(String name) {
        MemberSpec ms = find(members, name);
        assertTrue(ms.readonly, () -> name + " readonly");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has an optional member ...")
    @ValueSource(strings = { //
            "s", //
    })
    @SuppressWarnings("static-method")
    void checkOptionalMember(String name) {
        MemberSpec ms = find(members, name);
        assertTrue(ms.optional, () -> name + " optional");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a non-optional member ...")
    @ValueSource(strings = { //
            "i", //
            "x", //
            "text", // name for t
            "obj", //
            "i2", //
            "x2", //
            "text2", //
            "strhex2", //
    })
    @SuppressWarnings("static-method")
    void checkMandatoryMember(String name) {
        MemberSpec ms = find(members, name);
        assertFalse(ms.optional, () -> name + " optional");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a documented member ...")
    @ValueSource(strings = { //
            "x", //
    })
    @SuppressWarnings("static-method")
    void checkDocMember(String name) {
        MemberSpec ms = find(members, name);
        assertEquals(ms.doc, "Doc string for " + name);
    }

    // ----------------------------------------------------------------
    @Test
    @DisplayName("has the expected number of get-set attributes.")
    @SuppressWarnings("static-method")
    void numberOfGetSets() { assertEquals(3, getsets.size(), "number of get-set attributes"); }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a readonly get-set ...")
    @ValueSource(strings = { //
            "count", //
    })
    @SuppressWarnings("static-method")
    void checkReadonlyGetSet(String name) {
        GetSetSpec gs = find(getsets, name);
        assertTrue(gs.readonly(), () -> name + " readonly");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a writable get-set ...")
    @ValueSource(strings = { //
            "foo", //
            "thing", //
    })
    @SuppressWarnings("static-method")
    void checkWritableGetSet(String name) {
        GetSetSpec gs = find(getsets, name);
        assertFalse(gs.readonly(), () -> name + " readonly");
        // There must be a setter for each implementation
        assertEquals(gs.getters.size(), gs.setters.size(), () -> name + " setter size mismatch");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has a non-optional get-set ...")
    @ValueSource(strings = { //
            "thing", //
    })
    @SuppressWarnings("static-method")
    void checkMandatoryGetSet(String name) {
        GetSetSpec gs = find(getsets, name);
        assertTrue(gs.optional(), () -> name + " optional");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("has an optional get-set ...")
    @ValueSource(strings = { //
            "thing", //
    })
    @SuppressWarnings("static-method")
    void checkOptionalGetSet(String name) {
        GetSetSpec gs = find(getsets, name);
        assertTrue(gs.optional(), () -> name + " optional");
        // There must be a deleter for each implementation
        assertEquals(gs.getters.size(), gs.deleters.size(), () -> name + " deleter size mismatch");
    }
}
