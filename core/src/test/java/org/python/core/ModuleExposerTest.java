// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.core.Exposed.PositionalOnly;
import org.python.core.Exposed.PythonMethod;
import org.python.core.Exposed.PythonStaticMethod;
import org.python.core.ModuleDef.MethodDef;

/**
 * Test that a Python <b>module</b> defined in Java, using the scheme of
 * annotations defined in {@link Exposed}, can be processed correctly by
 * a {@link Exposer} to a {@link ModuleDef}. This tests a large part of
 * the exposure mechanism.
 * <p>
 * The class used in the test {@link FakeModule} is not actually a
 * {@link PyModule}, but we go through the actions of the
 * {@link ModuleExposer} so we can examine the intermediate results.
 */
@DisplayName("For a module exposed from a Java definition")
class ModuleExposerTest extends UnitTestSupport {

    /**
     * This class is not actually a Python module definition, but is
     * annotated as if it were. We will test whether the
     * {@link MethodDef}s are created as expected. We'll also act on it
     * to produce a dictionary as if it were a real module.
     */
    static class FakeModule {

        static final Lookup LOOKUP = MethodHandles.lookup();

        // Signature: ()
        @PythonStaticMethod
        static void f0() {}

        // Signature: ($module, /)
        @PythonMethod
        void m0() {}

        // Signature: (a)
        @PythonStaticMethod
        static PyTuple f1(double a) {return Py.tuple(a);}

        // Signature: ($module, a, /)
        @PythonMethod
        @SuppressWarnings("static-method")
        PyTuple m1(double a) {return Py.tuple(a);}

        // Signature: (a, b, c, /)
        @PythonStaticMethod
        static PyTuple f3(int a, String b, Object c) {
            return Py.tuple(a, b, c);
        }

        // Signature: ($module, a, b, c, /)
        @PythonMethod
        @SuppressWarnings("static-method")
        PyTuple m3(int a, String b, Object c) {
            return Py.tuple(a, b, c);
        }

        // Signature: (/, a, b, c)
        @PythonStaticMethod(positionalOnly = false)
        static PyTuple f3pk(int a, String b, Object c) {
            return Py.tuple(a, b, c);
        }

        // Signature: ($module, /, a, b, c)
        @PythonMethod(positionalOnly = false)
        @SuppressWarnings("static-method")
        PyTuple m3pk(int a, String b, Object c) {
            return Py.tuple(a, b, c);
        }

        // Signature: (a, b, /, c)
        @PythonStaticMethod
        static PyTuple f3p2(int a, @PositionalOnly String b, Object c) {
            return Py.tuple(a, b, c);
        }

        // Signature: ($module, a, b, /, c)
        @PythonMethod
        @SuppressWarnings("static-method")
        PyTuple m3p2(int a, @PositionalOnly String b, Object c) {
            return Py.tuple(a, b, c);
        }
    }

    @Nested
    @DisplayName("calling the Exposer")
    class TestExposer {

        @Test
        @DisplayName("produces a ModuleExposer")
        void getExposer() {
            ModuleExposer exposer =
                    Exposer.exposeModule(FakeModule.class);
            assertNotNull(exposer);
        }

        @Test
        @DisplayName("finds the expected methods")
        void getMethodDefs() {
            ModuleExposer exposer =
                    Exposer.exposeModule(FakeModule.class);
            MethodDef[] mdArray =
                    exposer.getMethodDefs(FakeModule.LOOKUP);
            checkMethodDefArray(mdArray);
        }
    }

    @Nested
    @DisplayName("constructing a ModuleDef")
    class TestDefinition {

        @Test
        @DisplayName("produces a MethodDef array")
        void createMethodDef() {
            ModuleDef def = new ModuleDef("example", FakeModule.LOOKUP);
            checkMethodDefArray(def.getMethods());
        }
    }

    @Nested
    @DisplayName("a module instance")
    class TestInstance {

        @Test
        @DisplayName("has expected method signatures")
        void hasMethods() {
            /*
             * As FakeModule is not a PyModule, we must work a bit
             * harder to take care of things normally automatic. Make a
             * ModuleDef to hold the MethodDefs from the Exposer.
             */
            ModuleDef def = new ModuleDef("example", FakeModule.LOOKUP);
            // An instance of the "module" to bind in PyJavaMethods
            FakeModule fake = new FakeModule();
            // A map to stand in for the module dictionary to hold them
            Map<Object, Object> dict = new HashMap<>();
            // Which we now fill ...
            for (MethodDef md : def.getMethods()) {
                ArgParser ap = md.argParser;
                MethodHandle mh = md.handle;
                PyJavaFunction m =
                        PyJavaFunction.fromParser(ap, mh, fake, def.name);
                dict.put(md.argParser.name, m);
            }
            // And here we check what's in it
            checkMethodSignatures(dict);
        }
    }

    private static void checkMethodDefArray(MethodDef[] defs) {
        assertNotNull(defs);

        Map<String, MethodDef> mds = new TreeMap<>();
        for (MethodDef def : defs) { mds.put(def.argParser.name, def); }

        Set<String> expected = new TreeSet<>();
        expected.addAll(List.of( //
                "f0", "f1", "f3", "f3pk", "f3p2", //
                "m0", "m1", "m3", "m3pk", "m3p2"));

        assertEquals(expected, mds.keySet(), "contains expected names");
    }

    private static void
            checkMethodSignatures(Map<Object, Object> dict) {
        assertNotNull(dict);

        checkSignature(dict, "f0()");
        checkSignature(dict, "m0($module, /)");
        checkSignature(dict, "f1(a, /)");
        checkSignature(dict, "m1($module, a, /)");
        checkSignature(dict, "f3(a, b, c, /)");
        checkSignature(dict, "m3($module, a, b, c, /)");
        checkSignature(dict, "f3pk(a, b, c)");
        checkSignature(dict, "m3pk($module, /, a, b, c)");
        checkSignature(dict, "f3p2(a, b, /, c)");
        checkSignature(dict, "m3p2($module, a, b, /, c)");
    }

    /**
     * Check that a method with the expected signature is in the
     * dictionary.
     *
     * @param dict dictionary
     * @param spec signature
     */
    private static void checkSignature(Map<Object, Object> dict,
            String spec) {
        int k = spec.indexOf('(');
        assertTrue(k > 0);
        String name = spec.substring(0, k);
        String expect = spec.substring(k);
        PyJavaFunction pjf = (PyJavaFunction)dict.get(name);
        assertEquals(expect, pjf.argParser.textSignature());
    }

}
