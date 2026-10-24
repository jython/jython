// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import org.python.kernel.SpecialMethod;
import org.python.types.FastCall;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test that methods exposed by a Python <b>type</b> defined in Java,
 * identified implicitly by name and signature, result in method
 * descriptors with characteristics expected. We create an adoptive type
 * in order to test the more complex case.
 * <p>
 * The first test in each case is to examine fields in the
 * {@link PyWrapperDescr} to ensure the expected slot has been
 * identified. Then we call the function using the {@code __call__}
 * special method, and using our "Java call" signatures.
 */
@DisplayName("When exposed as a special method")
class TypeExposerSlotWrapperTest {

    /**
     * Certain nested test classes implement these as standard. A base class
     * here is just a way to describe the tests once that reappear in each
     * nested case.
     */
    abstract static class Standard {

        // Working variables for the tests
        /** Unbound descriptor by type access to examine or call. */
        PyWrapperDescr descr;
        /** The object on which to invoke the method. */
        Object obj;
        /**
         * The bound method to examine or call (bound to {@code obj}).
         */
        PyMethodWrapper func;

        /**
         * The special method descriptor should have field values that correctly
         * reflect the signature and annotations in the defining class.
         */
        abstract void has_expected_fields();

        /**
         * Call the descriptor using the {@code __call__} special method
         * directly with arguments correct for the slot's specification. The
         * method should obtain the correct result (and not throw). We do not
         * test exposure of the descriptor's {@code __call__}.
         *
         * @throws Throwable unexpectedly
         */
        abstract void descriptor_supports__call__() throws Throwable;

        /**
         * Call the descriptor using the {@code __call__} special method
         * directly with various incorrect arguments. The method should raise
         * {@code TypeError} each time.
         *
         * @throws Throwable unexpectedly
         */
        abstract void descriptor_checks__call__() throws Throwable;

        /**
         * Call the bound method using the {@code __call__} special method
         * directly with arguments correct for the slot's specification. The
         * method should obtain the correct result (and not throw). We do not
         * test exposure of the bound method's {@code __call__}.
         *
         * @throws Throwable unexpectedly
         */
        abstract void method_supports__call__() throws Throwable;

        /**
         * Call the bound method using the {@code __call__} special method
         * directly with various incorrect arguments. The method should raise
         * {@code TypeError} each time.
         *
         * @throws Throwable unexpectedly
         */
        abstract void method_checks__call__() throws Throwable;

        /**
         * Call the descriptor using {@link FastCall} API with arguments correct
         * for the slot's specification. The method should obtain the correct
         * result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void descriptor_supports_java_call() throws Throwable;

        /**
         * Call the descriptor using {@link FastCall} API with various incorrect
         * arguments. The method should throw each time.
         *
         * @throws Throwable unexpectedly
         */
        abstract void descriptor_checks_java_call() throws Throwable;

        /**
         * Call the bound method using {@link FastCall} API with arguments
         * correct for the slot's specification. The method should obtain the
         * correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void method_supports_java_call() throws Throwable;

        /**
         * Call the bound method using {@link FastCall} API with various
         * incorrect arguments. The method should throw each time.
         */
        abstract void method_checks_java_call();

        /**
         * Check that the fields of the descriptor match expectations.
         *
         * @param slot to be implemented
         */
        void expect(SpecialMethod slot) {
            assertEquals(slot, descr.sm);
            assertEquals(slot.methodName, descr.__name__());
            assertEquals(PyWrapperDescr.TYPE(), descr.getType());
            assertEquals(ExampleObject.TYPE, descr.__objclass__());
        }

        /**
         * Helper to set up each test.
         *
         * @param name of the method
         * @param o to use as the self argument
         * @throws PyAttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(String name, Object o) throws PyAttributeError, Throwable {
            descr = (PyWrapperDescr)ExampleObject.TYPE.lookup(name);
            obj = o;
            func = new PyMethodWrapper(descr, o);
        }

        /**
         * Check the result of a call against that expected. The reference
         * result is the same throughout a given sub-class test.
         *
         * @param result of call
         */
        abstract void check(Object result);

    }

    /**
     * A Python type definition that defines some special methods to be
     * found by the exposer.
     */
    static class ExampleObject implements WithClass {

        static PyType TYPE = PyType.fromSpec( //
                new TypeSpec("Example", MethodHandles.lookup()) //
                        .add(Feature.IMMUTABLE) //
                        .adopt(ExampleObject2.class));

        private int value;

        ExampleObject(int value) { this.value = value; }

        @Override
        public PyType getType() { return TYPE; }

        /**
         * See {@link Test__str__}: a unary operation.
         *
         * @return the value in angle-brackets.
         */
        String __str__() { return "<" + value + ">"; }

        static String __str__(ExampleObject2 self) { return "<" + self.value + ">"; }

        /**
         * See {@link Test__add__}: a binary operation.
         *
         * @param other to pretend "add"
         * @return the arguments.
         */
        PyTuple __add__(Object other) { return Py.tuple(this, other); }

        static PyTuple __add__(ExampleObject2 self, Object other) { return Py.tuple(self, other); }
    }

    /**
     * Class cited as an "adopted implementation" of {@link ExampleObject}
     */
    static class ExampleObject2 {

        private BigInteger value;

        ExampleObject2(long value) { this.value = BigInteger.valueOf(value); }
    }

    /** {@link ExampleObject#__str__()} accepts no arguments. */
    @Nested
    @DisplayName("__str__")
    class Test__str__ extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("__str__", new ExampleObject(42)); }

        @Override
        void check(Object result) { assertEquals("<42>", result); }

        @Override
        @Test
        void has_expected_fields() { expect(SpecialMethod.op_str); }

        @Override
        @Test
        void descriptor_supports__call__() throws Throwable {
            // We call type(obj).__str__(obj)
            Object[] args = {obj};
            String[] kwnames = {};
            Object r = descr.__call__(args, kwnames);
            check(r);
        }

        @Override
        @Test
        void descriptor_checks__call__() throws Throwable {

            Object[] args = {obj};
            String[] kwnames = {};

            // We call type(obj).__str__(obj, 111)
            // Spurious positional argument.
            Object[] args2 = {obj, 111};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args2, kwnames));

            // We call type(obj).__str__(obj, other=111)
            // Unexpected keyword.
            String[] kwnames2 = {"other"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args2, kwnames2));

            // We call type(obj).__str__("oops")
            // Wrong self type.
            args[0] = "oops";
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, kwnames));
        }

        @Override
        @Test
        void method_supports__call__() throws Throwable {
            // We call obj.__str__()
            Object[] args = {};
            String[] kwnames = {};
            Object r = func.__call__(args, kwnames);
            check(r);
        }

        @Override
        @Test
        void method_checks__call__() throws Throwable {

            String[] kwnames = {};

            // We call obj.__str__(111)
            // Spurious positional argument.
            Object[] args2 = {111};
            assertRaises(PyExc.TypeError, () -> func.__call__(args2, kwnames));

            // We call obj.__str__(other=111)
            // Unexpected keyword.
            String[] kwnames2 = {"other"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args2, kwnames2));
        }

        @Override
        @Test
        void descriptor_supports_java_call() throws Throwable {
            // We call type(obj).__str__(obj)
            Object r = descr.call(obj);
            check(r);
        }

        @Override
        @Test
        void descriptor_checks_java_call() throws Throwable {
            // We call type(obj).__str__(obj, 111)
            // Spurious positional argument.
            ArgumentError ae = assertThrows(ArgumentError.class, () -> descr.call(obj, 111));
            assertEquals(ArgumentError.Mode.NOARGS, ae.mode);

            // We call type(obj).__str__(obj, other=111)
            // Unexpected keyword.
            Object[] args2 = {obj, 111};
            String[] kwnames2 = {"other"};
            ae = assertThrows(ArgumentError.class, () -> descr.call(args2, kwnames2));
            assertEquals(ArgumentError.Mode.NOARGS, ae.mode);

            // We call type(obj).__str__("oops")
            // Wrong self type.
            assertRaises(PyExc.TypeError, () -> descr.call("oops"));
        }

        @Override
        @Test
        void method_supports_java_call() throws Throwable {
            // We call obj.__str__()
            Object r = func.call();
            check(r);
        }

        @Override
        @Test
        void method_checks_java_call() {
            // We call obj.__str__(111)
            // Spurious positional argument.
            ArgumentError ae = assertThrows(ArgumentError.class, () -> func.call(111));
            assertEquals(ArgumentError.Mode.NOARGS, ae.mode);

            // We call obj.__str__(other=111)
            // Unexpected keyword.
            String[] kwnames2 = {"other"};
            Object[] args2 = {obj, 111};
            ae = assertThrows(ArgumentError.class, () -> func.call(args2, kwnames2));
            assertEquals(ArgumentError.Mode.NOARGS, ae.mode);
        }
    }

    /**
     * {@link Test__str__} with {@link ExampleObject2} as the
     * implementation.
     */
    @Nested
    @DisplayName("__str__" + " (type 2 impl)")
    class Test__str__2 extends Test__str__ {

        @Override
        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Example.__str__
            setup("__str__", new ExampleObject2(42));
        }
    }

    /**
     * {@link ExampleObject#__add__(Object)} accepts 1 argument that
     * <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("__add__")
    class Test__add__ extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Example.__add__
            setup("__add__", new ExampleObject(555));
        }

        @Override
        void check(Object result) {
            Object[] r = ((PyTuple)result).toArray();
            assertArrayEquals(new Object[] {obj, 111}, r);
        }

        @Override
        @Test
        void has_expected_fields() { expect(SpecialMethod.op_add); }

        @Override
        @Test
        void descriptor_supports__call__() throws Throwable {
            // We call type(obj).__add__(obj, 111)
            Object[] args = {obj, 111};
            String[] kwnames = {};
            Object r = descr.__call__(args, kwnames);
            check(r);
        }

        @Override
        @Test
        void descriptor_checks__call__() throws Throwable {

            Object[] args = {obj, 111};
            String[] kwnames = {};

            // We call type(obj).__add__(obj)
            // Missing argument.
            Object[] args2 = {obj};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args2, kwnames));

            // We call type(obj).__add__(obj, other=111)
            // Unexpected keyword.
            String[] kwnames2 = {"other"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, kwnames2));

            // We call type(obj).__add__("oops", 111)
            // Wrong self type.
            args[0] = "oops";
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, kwnames));
        }

        @Override
        @Test
        void method_supports__call__() throws Throwable {
            // We call obj.__add__(111)
            Object[] args = {111};
            String[] kwnames = {};
            Object r = func.__call__(args, kwnames);
            check(r);
        }

        @Override
        @Test
        void method_checks__call__() throws Throwable {

            Object[] args = {111};
            String[] kwnames = {};

            // We call obj.__add__()
            // Missing argument.
            Object[] args2 = {};
            assertRaises(PyExc.TypeError, () -> func.__call__(args2, kwnames));

            // We call obj.__add__(other=111)
            // Unexpected keyword.
            String[] kwnames2 = {"other"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, kwnames2));
        }

        @Override
        @Test
        void descriptor_supports_java_call() throws Throwable {
            // We call type(obj).__add__(obj, 111)
            Object r = descr.call(obj, 111);
            check(r);
        }

        @Override
        @Test
        void descriptor_checks_java_call() throws Throwable {
            // We call type(obj).__add__(obj)
            // Missing argument.
            ArgumentError ae = assertThrows(ArgumentError.class, () -> descr.call(obj));
            assertEquals(ArgumentError.Mode.NUMARGS, ae.mode);

            // We call type(obj).__add__(obj, other=111)
            // Unexpected keyword.
            Object[] args2 = {obj, 111};
            String[] kwnames2 = {"other"};
            ae = assertThrows(ArgumentError.class, () -> descr.call(args2, kwnames2));
            assertEquals(ArgumentError.Mode.NOKWARGS, ae.mode);

            // We call type(obj).__add__("oops", 111)
            // Wrong self type.
            assertRaises(PyExc.TypeError, () -> descr.call("oops", 111));
        }

        @Override
        @Test
        void method_supports_java_call() throws Throwable {
            // We call obj.__add__(111)
            Object r = func.call(111);
            check(r);
        }

        @Override
        @Test
        void method_checks_java_call() {
            // We call obj.__add__()
            // Missing argument.
            ArgumentError ae = assertThrows(ArgumentError.class, () -> func.call());
            assertEquals(ArgumentError.Mode.NUMARGS, ae.mode);

            // We call obj.__add__(other=111)
            // Unexpected keyword.
            Object[] args2 = {111};
            String[] kwnames2 = {"other"};
            ae = assertThrows(ArgumentError.class, () -> func.call(args2, kwnames2));
            assertEquals(ArgumentError.Mode.NOKWARGS, ae.mode);
        }
    }

    /**
     * {@link Test__add__} with {@link ExampleObject2} as the
     * implementation.
     */
    @Nested
    @DisplayName("__add__" + " (type 2 impl)")
    class Test__add__2 extends Test__add__ {

        @Override
        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Example.__add__
            setup("__add__", new ExampleObject2(555));
        }
    }

    /**
     * Assert that calling the function raises a Python exception of
     * (exactly) the expected type. {@code Throwable}s that are not Python
     * exceptions propagate to the caller.
     *
     * @param exc expected type (one of the {@link PyExc}.* constants)
     * @param action to invoke
     * @return the exception thrown
     * @throws Throwable propagating on non-Python errors
     */
    static PyBaseException assertRaises(PyType exc, Executable action) throws Throwable {
        try {
            action.execute();
        } catch (PyBaseException pye) {
            PyType type = pye.getType();
            assertSame(exc, type);
            return pye;
        }
        fail("No exeption raised");
        return null;  // Not reached in practice
    }
}
