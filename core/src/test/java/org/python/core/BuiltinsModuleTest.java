package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * This is a test of instantiating and using the {@code builtins}
 * module, which has a special place in the Python interpreter as the
 * name space. Many built-in types and functions are named there for use
 * by the Python interpreter and it is effectively implicitly imported.
 */
@DisplayName("The builtins module")
class BuiltinsModuleTest extends UnitTestSupport {

    static final String FILE = "BuiltinsModuleTest.java";

    @Test
    @DisplayName("exists on an interepreter")
    @SuppressWarnings("static-method")
    void existsOnInterpreter() {
        Interpreter interp = new Interpreter();
        PyModule builtins = interp.builtinsModule;
        assertNotNull(builtins);
    }

    @Test
    @DisplayName("has independent instances")
    @SuppressWarnings("static-method")
    void canBeInstantiated() {
        Interpreter interp1 = new Interpreter();
        Interpreter interp2 = new Interpreter();
        // Look up an arbitrary function in each interpreter
        PyJavaFunction abs1 = (PyJavaFunction)interp1.getBuiltin("abs");
        assertSame(abs1.self, interp1.builtinsModule);
        PyJavaFunction abs2 = (PyJavaFunction)interp2.getBuiltin("abs");
        assertSame(abs2.self, interp2.builtinsModule);
        // Each module provides distinct function objects
        assertNotSame(abs1, abs2);
        // builtins module instances are distinct
        assertNotSame(interp1.builtinsModule, interp2.builtinsModule);
    }

    @Nested
    @DisplayName("provides expected function ...")
    class TestFunctions {
        Interpreter interp;
        PyDict globals;
        /* BuiltinsModule? */ PyModule builtins;

        @BeforeEach
        void setup() {
            interp = new Interpreter();
            globals = Py.dict();
            builtins = interp.builtinsModule;
        }


        @Test
        @DisplayName("abs")
        void testAbs() throws Throwable {
            Object f = Abstract.getAttr(builtins, "abs");
            Object r = Callables.callFunction(f, -5.0);
            assertEquals(5.0, r);
        }

 
        @Test
        @DisplayName("len")
        void testLen() throws Throwable {
            Object f = Abstract.getAttr(builtins, "len");
            Object r = Callables.callFunction(f, "hello");
            assertEquals(5, r);
        }

        @Test
        @DisplayName("max")
        void testMax() throws Throwable {
            Object f = Abstract.getAttr(builtins, "max");
            Object r = Callables.callFunction(f, 4, 4.2, 5.0, 6);
            assertEquals(6, r);
            r = Callables.callFunction(f, Py.tuple(4, 4.2, 5.0, 6));
            assertEquals(6, r);
        }

        @Test
        @DisplayName("min")
        void testMin() throws Throwable {
            Object f = Abstract.getAttr(builtins, "min");
            Object r = Callables.callFunction(f, 4, 5.0, 6, 4.2);
            assertEquals(4, r);
            r = Callables.callFunction(f, Py.tuple(4, 5.0, 6, 4.2));
            assertEquals(4, r);
        }

        @Test
        @DisplayName("repr")
        void testRepr() throws Throwable {
            Object f = Abstract.getAttr(builtins, "repr");
            assertEquals("123", Callables.callFunction(f, 123));
            assertEquals("'spam'", Callables.callFunction(f, "spam"));
            // XXX implement None.__repr__
            // assertEquals("None", Callables.callFunction(f, Py.None));
        }
    }
}
