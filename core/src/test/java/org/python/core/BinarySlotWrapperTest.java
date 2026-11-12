// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link PyWrapperDescr}s for binary special functions on a
 * variety of types. Unlike the companion call-site tests, a descriptor
 * is <b>the descriptor in a particular type</b>. The particular
 * operations are not the focus: we are testing the mechanisms for
 * creating and calling slot wrappers.
 */
class BinarySlotWrapperTest extends UnitTestSupport {

    @Nested
    @DisplayName("The slot wrapper '__sub__'")
    class Slot__sub__ extends SlotWrapperTestBase {

        final String NAME = "__sub__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends BinaryTest<Object, Object> {

            @Override
            Object expected(Object s, Object o) {
                return PyLong.asBigInteger(s).subtract(PyLong.asBigInteger(o));
            }

            @Override
            void check(Object exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                Integer iv = 50, iw = 8;
                // int and bool are both served by int.__sub__
                List<Object> vList =
                        List.of(iv, BigInteger.valueOf(iv), newPyLong(iv), true, false);
                // other argument accepts same types
                List<Object> wList =
                        List.of(iw, BigInteger.valueOf(iw), newPyLong(iw), true, false);
                super.setup(PyLong.TYPE, NAME, vList, wList);
            }

            /**
             * As {@link #supports_call()} but with empty keyword array.
             *
             * @throws Throwable unexpectedly
             */
            @Test
            void supports_call_with_keywords() throws Throwable {
                for (Args args : getCases()) {
                    Object exp = expected(args.s, args.o);
                    checkInt(exp, makeBoundCallKW(args.s, args.o));
                }
            }

            /**
             * As {@link #supports_bound_call()} but with empty keyword array.
             *
             * @throws Throwable unexpectedly
             */
            @Test
            void supports_bound_call_with_keywords() throws Throwable {
                for (Args args : getCases()) {
                    Object exp = expected(args.s, args.o);
                    checkInt(exp, makeBoundCallKW(args.s, args.o));
                }
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends BinaryTest<Object, Boolean> {

            @Override
            Object expected(Boolean s, Object o) {
                return (s ? BigInteger.ONE : BigInteger.ZERO).subtract(PyLong.asBigInteger(o));
            }

            @Override
            void check(Object exp, Object r) throws Throwable {
                checkInt(exp, r);  // even bool-bool is int
            }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                List<Boolean> vList = List.of(true, false);
                // other argument accepts int and bool types
                Integer iw = 42;
                List<Object> wList =
                        List.of(true, false, iw, BigInteger.valueOf(iw), newPyLong(iw));
                super.setup(PyBool.TYPE, NAME, vList, wList);
            }

            @Test
            @Override
            void has_expected_fields() {
                super.has_expected_fields();
                // The descriptor should be *exactly* that from int
                assertSame(PyLong.TYPE.lookup(NAME), descr);
            }
        }

        @Nested
        @DisplayName("of 'float' objects")
        class OfFloat extends BinaryTest<Object, Object> {

            @Override
            Object expected(Object s, Object o) {
                try {
                    return PyFloat.asDouble(s) - PyFloat.asDouble(o);
                } catch (Throwable e) {
                    return fail("unconvertible");
                }
            }

            @Override
            void check(Object exp, Object r) throws Throwable { checkFloat(exp, r); }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                Integer iw = 8;
                Double dv = 50.0, dw = iw.doubleValue();

                // self argument must be a float
                List<Object> vList = List.of(dv, newPyFloat(dv));
                // other argument accepts float, int, bool
                List<Object> wList = List.of(dw, newPyFloat(dw), iw, BigInteger.valueOf(iw),
                        newPyLong(iw), false, true);
                super.setup(PyFloat.TYPE, NAME, vList, wList);
            }
        }
    }

    @Nested
    @DisplayName("The slot wrapper '__rsub__'")
    class Slot__rsub__ extends SlotWrapperTestBase {

        final String NAME = "__rsub__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends BinaryTest<Object, Object> {

            @Override
            Object expected(Object s, Object o) {
                return PyLong.asBigInteger(o).subtract(PyLong.asBigInteger(s));
            }

            @Override
            void check(Object exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                Integer iv = 800, iw = 5000;
                // int and bool are both served by int.__rsub__
                List<Object> vList =
                        List.of(iv, BigInteger.valueOf(iv), newPyLong(iv), true, false);
                // other argument accepts same types
                List<Object> wList =
                        List.of(iw, BigInteger.valueOf(iw), newPyLong(iw), true, false);
                super.setup(PyLong.TYPE, NAME, vList, wList);
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends BinaryTest<Object, Boolean> {

            @Override
            Object expected(Boolean s, Object o) {
                return PyLong.asBigInteger(o).subtract(s ? BigInteger.ONE : BigInteger.ZERO);
            }

            @Override
            void check(Object exp, Object r) throws Throwable {
                checkInt(exp, r);  // even bool-bool is int
            }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                List<Boolean> vList = List.of(true, false);
                // other argument accepts int and bool types
                Integer iw = 4200;
                List<Object> wList =
                        List.of(true, false, iw, BigInteger.valueOf(iw), newPyLong(iw));
                super.setup(PyBool.TYPE, NAME, vList, wList);
            }

            @Test
            @Override
            void has_expected_fields() {
                super.has_expected_fields();
                // The descriptor should be *exactly* that from int
                assertSame(PyLong.TYPE.lookup(NAME), descr);
            }
        }

        @Nested
        @DisplayName("of 'float' objects")
        class OfFloat extends BinaryTest<Object, Object> {

            @Override
            Object expected(Object s, Object o) {
                try {
                    return PyFloat.asDouble(o) - PyFloat.asDouble(s);
                } catch (Throwable e) {
                    return fail("unconvertible");
                }
            }

            @Override
            void check(Object exp, Object r) throws Throwable { checkFloat(exp, r); }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                Integer iw = 5000;
                Double dv = 800.0, dw = iw.doubleValue();

                // self argument must be a float
                List<Object> vList = List.of(dv, newPyFloat(dv));
                // other argument accepts float, int, bool
                List<Object> wList = List.of(dw, newPyFloat(dw), iw, BigInteger.valueOf(iw),
                        newPyLong(iw), false, true);
                super.setup(PyFloat.TYPE, NAME, vList, wList);
            }
        }
    }

    @Nested
    @DisplayName("The slot wrapper '__and__'")
    class Slot__and__ extends SlotWrapperTestBase {

        final String NAME = "__and__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends BinaryTest<Object, Object> {

            @Override
            Object expected(Object s, Object o) {
                return PyLong.asBigInteger(s).and(PyLong.asBigInteger(o));
            }

            @Override
            void check(Object exp, Object r) throws Throwable { checkInt(exp, r); }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                Integer iv = 50, iw = 8;
                // not bool here as bool.__and__ is distinct
                List<Object> vList = List.of(iv, BigInteger.valueOf(iv), newPyLong(iv));
                // other argument accepts int or bool
                List<Object> wList =
                        List.of(iw, BigInteger.valueOf(iw), newPyLong(iw), true, false);
                super.setup(PyLong.TYPE, NAME, vList, wList);
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends BinaryTest<Object, Boolean> {

            @Override
            Object expected(Boolean s, Object o) {
                if (o instanceof Boolean)
                    return (s) && s.equals(o);
                else
                    return PyLong.asBigInteger(s).and(PyLong.asBigInteger(o));
            }

            @Override
            void check(Object exp, Object r) throws Throwable {
                if (exp instanceof Boolean)
                    checkBool(exp, r);
                else
                    checkInt(exp, r);
            }

            @BeforeEach
            void setup() throws PyAttributeError, Throwable {
                List<Boolean> vList = List.of(true, false);
                List<Object> wList =
                        List.of(true, false, 100, 101, BigInteger.valueOf(102), newPyLong(103));
                super.setup(PyBool.TYPE, NAME, vList, wList);
            }

            @Test
            @Override
            void has_expected_fields() {
                super.has_expected_fields();
                // The descriptor should not be that from int
                assertNotSame(PyLong.TYPE.lookup(NAME), descr);
            }
        }
    }

    /**
     * Test invocation of the {@code str.__add__} descriptor on the adopted
     * implementations of {@code str}. Note that CPython {@code str} defines
     * {@code str.__add__} but not {@code str.__radd__}.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void str_add() throws Throwable {

        PyWrapperDescr add = (PyWrapperDescr)PyUnicode.TYPE.lookup("__add__");

        String sv = "pets", sw = "hop";
        PyUnicode uv = newPyUnicode(sv), uw = newPyUnicode(sw);
        String exp = "petshop";

        // v is String, PyUnicode.
        for (Object v : List.of(sv, uv)) {
            // w is PyUnicode, String, and int types
            for (Object w : List.of(uw, sw)) {
                Object r = add.__call__(new Object[] {v, w}, null);
                assertPythonType(PyUnicode.TYPE, r);
                assertEquals(exp, toString(r));
            }
        }
    }

    /**
     * Test invocation of the {@code str.__mul__} descriptor on the adopted
     * implementations of {@code str} with the accepted implementations of
     * {@code int}. This is the one that implements {@code "hello" * 3}.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void str_mul() throws Throwable {

        PyWrapperDescr mul = (PyWrapperDescr)PyUnicode.TYPE.lookup("__mul__");

        String sv = "woof!";
        PyUnicode uv = newPyUnicode(sv);
        int iw = 3;

        List<Object> wList = List.of(newPyLong(iw), iw, BigInteger.valueOf(iw), false, true);

        // v is String, PyUnicode.
        for (Object v : List.of(sv, uv)) {
            // w is various int types
            for (Object w : wList) {
                Object r = mul.__call__(new Object[] {v, w}, null);
                assertPythonType(PyUnicode.TYPE, r);
                assertEquals(sv.repeat(toInt(w)), toString(r));
            }
        }
    }

    /**
     * Test invocation of the {@code str.__rmul__} descriptor on the adopted
     * implementations of {@code str} with the accepted implementations of
     * {@code int}. This is the one that implements {@code 3 * "hello"},
     * once {@code int} has realised it doesn't know how.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void str_rmul() throws Throwable {

        PyWrapperDescr rmul = (PyWrapperDescr)PyUnicode.TYPE.lookup("__rmul__");

        int iv = 3;
        String sw = "woof!";
        PyUnicode uw = newPyUnicode(sw);

        List<Object> vList = List.of(newPyLong(iv), iv, BigInteger.valueOf(iv), false, true);

        // v is various int types
        for (Object v : vList) {
            // w is PyUnicode, String
            for (Object w : List.of(sw, uw)) {
                Object r = rmul.__call__(new Object[] {w, v}, null);
                assertPythonType(PyUnicode.TYPE, r);
                assertEquals(sw.repeat(toInt(v)), toString(r));
            }
        }
    }

}
