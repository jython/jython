package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.python.core.UnitTestSupport.assertPythonType;

import java.lang.invoke.MethodHandle;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.python.core.Slot.Signature;

/**
 *
 * Support for invoking slot wrapper descriptors in several forms,
 * and for checking the results, as used in the nested tests of
 * {@link UnarySlotWrapperTest}, {@link BinarySlotWrapperTest}, and
 * others. As a base class for a test it creates and holds the
 * descriptor object under test certain related information, and
 * defines test methods that we repeat in each nested case.
 */
class SlotWrapperTestBase {

    /** Empty array of {@code Object}. */
    static final Object[] NOARGS = new Object[0];
    /** Empty array of {@code String}. */
    static final String[] NOKEYWORDS = new String[0];

    // Working variables for the tests
    /** Name of the special method under test. */
    String name;
    /** Unbound slot wrapper to examine or call. */
    PyWrapperDescr descr;
    /** The slot corresp9onding to the descriptor name. */
    Slot slot;
    /** The type on which to invoke the special method. */
    PyType type;

    /**
     * The slot wrapper should have field values that correctly reflect
     * the signature and defining class.
     */
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
     * @param signature required of the slot method
     * @throws IllegalArgumentException if not a slot method name of the
     *     required signature
     * @throws AttributeError if method not found
     * @throws Throwable other errors
     */
    void setup(PyType type, String name, Signature signature) throws AttributeError, Throwable {
        this.name = name;
        this.type = type;

        this.slot = Slot.forMethodName(name);
        if (slot == null)
            throw new IllegalArgumentException(String.format("'%s' does not name a slot", name));
        else if (slot.signature != signature)
            throw new IllegalArgumentException(String.format("'%s' is not %s", name, signature));

        descr = (PyWrapperDescr)type.lookup(name);
        if (descr == null)
            throw Abstract.noAttributeOnType(type, name);
    }

    /**
     * Check a return value that is expected to be a Python {@code int}.
     *
     * @param exp value expected
     * @param r return value to test
     * @throws Throwable unexpectedly
     */
    static void checkInt(Object exp, Object r) throws Throwable {
        assertPythonType(PyLong.TYPE, r);
        BigInteger e = PyLong.asBigInteger(exp);
        BigInteger res = PyLong.asBigInteger(r);
        assertEquals(e, res);
    }

    /**
     * Check a return value that is expected to be a Python
     * {@code bool}.
     *
     * @param exp value expected
     * @param r return value to test
     * @throws Throwable unexpectedly
     */
    static void checkBool(Object exp, Object r) throws Throwable {
        assertPythonType(PyBool.TYPE, r);
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
    static void checkStr(Object exp, Object r) throws Throwable {
        assertPythonType(PyUnicode.TYPE, r);
        assertEquals(exp.toString(), r.toString());
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
    abstract class BaseTest<R, S> {
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
         * The slot wrapper should have field values that correctly reflect
         * the signature and defining class.
         */
        @Test
        void has_expected_fields() {
            // Implement using the enclosing instance
            SlotWrapperTestBase.this.has_expected_fields();
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
         * Call the slot wrapper using the {@code __call__} special method,
         * bound, with arguments correct for the slot's specification. The
         * called method should obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_bound_call() throws Throwable;

        /**
         * Call the slot wrapper using the Java call interface with
         * arguments correct for the slot's specification. The function
         * should obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_java_call() throws Throwable;

        /**
         * Call the wrapped operation through the {@link Operations} object
         * for the implementation type, using invokeExact and arguments
         * correct for the slot's specification. The function should obtain
         * the correct result (and not throw). (Unlike CPython, the
         * "wrapper" is not a wrapper on the slot, on the implementing
         * method for which op_NAME becomes the cache.)
         *
         * @throws Throwable unexpectedly
         */
        abstract void fills_op_slot() throws Throwable;
    }

    /**
     * A class that implements the tests for one combination of a
     * {@link Signature#UNARY} slot wrapper and type, extending
     * {@link BaseTest}.
     */
    abstract class UnaryTest<R, S> extends BaseTest<R, S> {

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
        @Override
        abstract void check(R exp, Object r) throws Throwable;

        /**
         * Helper to set up each test specifying the slot signature.
         *
         * @param type under test
         * @param name of the special method
         * @param signature required of the slot method
         * @param cases list of values to use as self
         * @throws IllegalArgumentException if not a slot method name of the
         *     required signature
         * @throws AttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(PyType type, String name, Signature signature, List<S> cases)
                throws IllegalArgumentException, AttributeError, Throwable {
            SlotWrapperTestBase.this.setup(type, name, signature);
            this.cases = cases;
        }

        /**
         * Helper to set up each test.
         *
         * @param type under test
         * @param name of the special method
         * @param signature required of the slot method
         * @param cases list of values to use as self
         * @throws IllegalArgumentException if not a unary slot method name
         * @throws AttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(PyType type, String name, List<S> cases)
                throws IllegalArgumentException, AttributeError, Throwable {
            setup(type, name, Signature.UNARY, cases);
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

        @Override
        @Test
        void fills_op_slot() throws Throwable {
            for (S x : cases) {
                R exp = expected(x);
                check(exp, makeHandleCall(x));
            }
        }

        List<S> getCases() { return Collections.unmodifiableList(cases); }

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
         * Make a single invocation of {@link #descr} with empty keywords
         * argument.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeCallKW(Object x) throws Throwable {
            return descr.__call__(new Object[] {x}, NOKEYWORDS);
        }

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
         * Make a single invocation of {@link #descr} as a Java call.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeJavaCall(Object x) throws Throwable { return descr.call(x); }

        /**
         * Make a single invocation of the slot as a method handle.
         *
         * @param x argument on which to invoke (it's unary)
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeHandleCall(Object x) throws Throwable {
            Operations ops = Operations.of(x);
            MethodHandle mh = slot.getSlot(ops);
            return mh.invokeExact(x);
        }
    }

    /**
     * A class that implements the tests for one combination of a
     * {@link Signature#LEN} slot wrapper and type, extending
     * {@link BaseTest}.
     */
    abstract class LenTest<R, S> extends UnaryTest<R, S> {
        @Override
        void setup(PyType type, String name, List<S> cases)
                throws IllegalArgumentException, AttributeError, Throwable {
            setup(type, name, Signature.LEN, cases);
        }

        @Override
        Object makeHandleCall(Object x) throws Throwable {
            Operations ops = Operations.of(x);
            MethodHandle mh = slot.getSlot(ops);
            return (int)mh.invokeExact(x);
        }
    }

    /**
     * A class that implements the tests for one combination of a
     * {@link Signature#BINARY} slot wrapper and type, extending
     * {@link BaseTest}.
     */
    abstract class BinaryTest<R, S> extends BaseTest<R, S> {

        /** Holds a pair of arguments for a binary call. */
        class Args {
            S s;
            Object o;

            Args(S s, Object o) {
                this.s = s;
                this.o = o;
            }

        }

        /**
         * A list of arguments to which the special method under test will
         * be applied.
         */
        private List<Args> cases;

        /**
         * Compute the expected result of a call
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @return expected return from call under test
         */
        abstract R expected(S s, Object o);

        /**
         * Check the result of a call, potentially failing the test. Quite
         * often this simply calls one of the base tests
         * {@link #checkInt(Object, Object)}, etc..
         *
         * @param exp value expected
         * @param r return value to test
         * @throws Throwable unexpectedly
         */
        @Override
        abstract void check(R exp, Object r) throws Throwable;

        /**
         * Helper to set up each test. The schedule of tests will be all
         * pairs of values that may be formed from the two lists.
         *
         * @param type under test
         * @param name of the special method
         * @param sList list of values to use as self
         * @param oList list of values to use as other argument
         * @throws AttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(PyType type, String name, List<S> sList, List<Object> oList)
                throws AttributeError, Throwable {
            SlotWrapperTestBase.this.setup(type, name, Signature.BINARY);
            cases = new ArrayList<>();
            for (S s : sList) {
                for (Object o : oList) {
                    addCase(s, o);
                }
            }
        }

        @Override
        @Test
        void supports_call() throws Throwable {
            for (Args args : cases) {
                R exp = expected(args.s, args.o);
                check(exp, makeCall(args.s, args.o));
            }
        }

        @Override
        @Test
        void supports_bound_call() throws Throwable {
            for (Args args : cases) {
                R exp = expected(args.s, args.o);
                check(exp, makeBoundCall(args.s, args.o));
            }
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            for (Args args : cases) {
                R exp = expected(args.s, args.o);
                check(exp, makeJavaCall(args.s, args.o));
            }
        }

        @Override
        @Test
        void fills_op_slot() throws Throwable {
            for (Args args : cases) {
                R exp = expected(args.s, args.o);
                check(exp, makeHandleCall(args.s, args.o));
            }
        }

        List<Args> getCases() { return Collections.unmodifiableList(cases); }

        void addCase(S s, Object o) { cases.add(new Args(s, o)); }

        /**
         * Make a single invocation of {@link #descr} with {@code null}
         * keywords argument.
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @throws Throwable unexpectedly
         */
        Object makeCall(Object s, Object o) throws Throwable {
            return descr.__call__(new Object[] {s, o}, null);
        }

        /**
         * Make a single invocation of {@link #descr} with empty keywords
         * argument.
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @throws Throwable unexpectedly
         */
        Object makeCallKW(Object s, Object o) throws Throwable {
            return descr.__call__(new Object[] {s, o}, NOKEYWORDS);
        }

        /**
         * Make a single invocation of {@link #descr} having bound it to the
         * argument.
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeBoundCall(Object s, Object o) throws Throwable {
            PyMethodWrapper meth = (PyMethodWrapper)descr.__get__(s, null);
            return meth.__call__(new Object[] {o}, null);
        }

        /**
         * Make a single invocation of {@link #descr} having bound it to the
         * argument.
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeBoundCallKW(Object s, Object o) throws Throwable {
            PyMethodWrapper meth = (PyMethodWrapper)descr.__get__(s, null);
            return meth.__call__(new Object[] {o}, NOKEYWORDS);
        }

        /**
         * Make a single invocation of {@link #descr} as a Java call.
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeJavaCall(Object s, Object o) throws Throwable { return descr.call(s, o); }

        /**
         * Make a single invocation of the slot as a method handle.
         *
         * @param s self argument on which to invoke
         * @param o other argument on which to invoke
         * @return result of call
         * @throws Throwable unexpectedly
         */
        Object makeHandleCall(Object s, Object o) throws Throwable {
            Operations ops = Operations.of(s);
            MethodHandle mh = slot.getSlot(ops);
            return mh.invokeExact(s, o);
        }
    }
}
