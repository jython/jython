package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link PyWrapperDescr}s for unary special functions on a
 * variety of types. The particular operations are not the focus: we
 * are testing the mechanisms for creating and calling slot
 * wrappers.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnarySlotWrapperTest extends UnitTestSupport {

    /**
     * Nested test classes, that test a particular slot for a particular
     * type, implement these as standard. A base class here is just a
     * way to describe the tests once that we repeat in each nested
     * case.
     */
    abstract static class SlotDetails {

        // Working variables for the tests
        /** Name of the special method under test. */
        String name;
        /** Unbound slot wrapper to examine or call. */
        PyWrapperDescr descr;
        /** The type on which to invoke the special method. */
        PyType type;

        /**
         * The slot wrapper should have field values that correctly reflect
         * the signature and defining class.
         */
        @Test
        void has_expected_fields() {
            assertEquals(name, descr.name);
            assertTrue(type.isSubTypeOf(descr.objclass), "target is sub-type of defining class");
            // more ...
        }

        /**
         * Helper to set up each test.
         *
         * @param type under test
         * @param name of the special method
         * @throws AttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(PyType type, String name) throws AttributeError, Throwable {
            this.name = name;
            this.type = type;
            descr = (PyWrapperDescr)type.lookup(name);
            if (descr == null)
                throw Abstract.noAttributeOnType(type, name);
        }

        /**
         * Call the slot wrapper using the {@code __call__} special method,
         * unbound, with arguments correct for the slot's specification. The
         * called method should obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_call() throws Throwable;

        /**
         * Make a single invocation of {@link #descr} with {@code null}
         * keywords argument.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeCall(Object x) throws Throwable {
            return descr.__call__(new Object[] {x}, null);
        }

        /**
         * Make a single invocation of {@link #descr} directly.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeCallKW(Object x) throws Throwable {
            return descr.__call__(new Object[] {x}, NOKEYWORDS);
        }

        /**
         * Call the slot wrapper using the {@code __call__} special method,
         * bound, with arguments correct for the slot's specification. The
         * called method should obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_bound_call() throws Throwable;

        /**
         * Make a single invocation of {@link #descr} having bound it to the
         * argument.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeBoundCall(Object x) throws Throwable {
            PyMethodWrapper meth = (PyMethodWrapper)descr.__get__(x, null);
            return meth.__call__(NOARGS, null);
        }

        /**
         * Make a single invocation of {@link #descr} having bound it to the
         * argument.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeBoundCallKW(Object x) throws Throwable {
            PyMethodWrapper meth = (PyMethodWrapper)descr.__get__(x, null);
            return meth.__call__(NOARGS, NOKEYWORDS);
        }

        /**
         * Call the lot wrapper using the Java call interface with arguments
         * correct for the slot's specification. The function should obtain
         * the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_java_call() throws Throwable;

        /**
         * Make a single invocation of {@link #descr} as a Java call .
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeJavaCall(Object x) throws Throwable { return descr.call(x); }

        /**
         * Check a return value that is expected to be a Python {@code int}.
         *
         * @param exp value expected
         * @param r return value to test
         * @throws Throwable unexpectedly
         */
        void checkInt(Object exp, Object r) throws Throwable {
            assertPythonType(PyLong.TYPE, r);
            BigInteger e = PyLong.asBigInteger(exp);
            BigInteger res = PyLong.asBigInteger(r);
            assertEquals(e, res);
        }

        /**
         * Check a return value that is expected to be a Python {@code str}.
         *
         * @param exp value expected
         * @param r return value to test
         * @throws Throwable unexpectedly
         */
        void checkStr(Object exp, Object r) throws Throwable {
            assertPythonType(PyUnicode.TYPE, r);
            assertEquals(exp.toString(), r.toString());
        }
    }

    /**
     * A class that implements the tests for one combination of slot
     * wrapper and type. The class specialises its type to the Java
     * return type {@code R} of the special method under test, and a
     * Java super-type {@code S} of the {@code self} argument. For a
     * Python type with just one implementation, {@code S} may be that
     * implementation type. For a Python type with multiple
     * implementations, {@code S} must be the common super-type, which
     * is usually {@code Object}.
     *
     * @param <R> the return type of the special method under test
     * @param <S> the common Java super-type of implementations
     */
    abstract class SlotDetailTest<R, S> extends SlotDetails {

        /**
         * A list of arguments to which the special method under test will
         * be applied.
         */
        private List<S> cases;

        /**
         * Compute the expected result of a call
         *
         * @param x argument to the call under test
         * @return expected return from call under test
         */
        abstract R expected(S x);

        /**
         * Check the result of a call, potentially failing the test. Quite
         * often this simply calls one of the base tests
         * {@link #checkInt(Object, Object)}, etc..
         *
         * @param exp value expected
         * @param r return value to test
         * @throws Throwable unexpectedly
         */
        abstract void check(R exp, Object r) throws Throwable;

        /**
         * Helper to set up each test.
         *
         * @param type under test
         * @param name of the special method
         * @throws AttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(PyType type, String name, List<S> cases) throws AttributeError, Throwable {
            super.setup(type, name);
            this.cases = cases;
        }

        @Override
        @Test
        void supports_call() throws Throwable {
            for (S x : cases) {
                R exp = expected(x);
                check(exp, makeCall(x));
            }
        }

        @Override
        @Test
        void supports_bound_call() throws Throwable {
            for (S x : cases) {
                R exp = expected(x);
                check(exp, makeBoundCall(x));
            }
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            for (S x : cases) {
                R exp = expected(x);
                check(exp, makeJavaCall(x));
            }
        }

        List<S> getCases() { return Collections.unmodifiableList(cases); }
    }

    static final Object[] NOARGS = new Object[0];
    static final String[] NOKEYWORDS = new String[0];

    @Nested
    @DisplayName("The slot wrapper '__neg__'")
    class Slot__neg__ {

        final String NAME = "__neg__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends SlotDetailTest<Object, Object> {

            @Override
            Object expected(Object x) {
                // Test material is 32 bit. Maybe BigInteger instead?
                return Integer.valueOf(-toInt(x));
            }

            @Override
            void check(Object exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                // x is Integer, BigInteger, PyLong, Boolean
                Integer ix = 42;
                super.setup(PyLong.TYPE, NAME,
                        List.of(ix, BigInteger.valueOf(ix), newPyLong(ix), false, true));
            }

            /**
             * As {@link #supports_call()} but with empty keyword array.
             */
            @Test
            void supports_call_with_keywords() throws Throwable {
                for (Object x : getCases()) {
                    Object exp = expected(x);
                    checkInt(exp, makeBoundCallKW(x));
                }
            }

            /**
             * As {@link #supports_bound_call()} but with empty keyword array.
             */
            @Test
            void supports_bound_call_with_keywords() throws Throwable {
                for (Object x : getCases()) {
                    Object exp = expected(x);
                    checkInt(exp, makeBoundCallKW(x));
                }
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends SlotDetailTest<Object, Boolean> {

            @Override
            Object expected(Boolean x) { return x ? -1 : 0; }

            @Override
            void check(Object exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                super.setup(PyBool.TYPE, NAME, List.of(false, true));
            }
        }
    }

    @Nested
    @DisplayName("The slot wrapper '__repr__'")
    class Slot__repr__ {

        final String NAME = "__repr__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends SlotDetailTest<String, Object> {

            @Override
            String expected(Object x) { return Integer.toString(toInt(x)); }

            @Override
            void check(String exp, Object r) throws Throwable { checkStr(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                // x is Integer, BigInteger, PyLong, Boolean
                Integer ix = 42;
                super.setup(PyLong.TYPE, NAME,
                        List.of(ix, BigInteger.valueOf(ix), newPyLong(ix), false, true));
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends SlotDetailTest<String, Boolean> {

            @Override
            String expected(Boolean x) { return x ? "True" : "False"; }

            @Override
            void check(String exp, Object r) throws Throwable { checkStr(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                super.setup(PyBool.TYPE, NAME, List.of(false, true));
            }
        }
    }

    @Nested
    @DisplayName("The slot wrapper '__hash__'")
    class Slot__hash__ {

        final String NAME = "__hash__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends SlotDetailTest<Integer, Object> {

            @Override
            Integer expected(Object x) { return toInt(x); }

            @Override
            void check(Integer exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                // x is Integer, BigInteger, PyLong, Boolean
                Integer ix = 42;
                super.setup(PyLong.TYPE, NAME,
                        List.of(ix, BigInteger.valueOf(ix), newPyLong(ix), false, true));
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends SlotDetailTest<Integer, Boolean> {

            @Override
            Integer expected(Boolean x) { return x ? 1 : 0; }

            @Override
            void check(Integer exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                super.setup(PyBool.TYPE, NAME, List.of(false, true));
            }
        }
    }
}
