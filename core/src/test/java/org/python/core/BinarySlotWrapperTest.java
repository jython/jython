package org.python.core;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link PyWrapperDescr}s for binary special functions on
 * a variety of types. Unlike the companion call-site tests, a
 * descriptor is <b>the descriptor in a particular type</b>. The
 * particular operations are not the focus: we are testing the
 * mechanisms for creating and calling slot wrappers.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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
            void setup() throws AttributeError, Throwable {
                Integer iv = 50, iw = 8;
                List<Object> vList =
                        List.of(iv, BigInteger.valueOf(iv), newPyLong(iv), true, false);
                // other argument accepts same types
                List<Object> wList =
                        List.of(iw, BigInteger.valueOf(iw), newPyLong(iw), true, false);
                super.setup(PyLong.TYPE, NAME, vList, wList);
            }

            /**
             * As {@link #supports_call()} but with empty keyword array.
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
            void setup() throws AttributeError, Throwable {
                // self will bool (since bool sub-classes int)
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
            void setup() throws AttributeError, Throwable {
                Integer iv = 800, iw = 5000;
                // self may be int or bool (since bool sub-classes int)
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
            void setup() throws AttributeError, Throwable {
                // self will bool (since bool sub-classes int)
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
            void setup() throws AttributeError, Throwable {
                Integer iv = 50, iw = 8;
                List<Object> vList = List.of(iv, BigInteger.valueOf(iv), newPyLong(iv));
                // other argument accepts int or bool
                List<Object> wList =
                        List.of(iw, BigInteger.valueOf(iw), newPyLong(iw), true, false);
                super.setup(PyLong.TYPE, NAME, vList, wList);
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends BinaryTest<Boolean, Boolean> {

            @Override
            Boolean expected(Boolean s, Object o) { return s && s.equals(o); }

            @Override
            void check(Boolean exp, Object r) throws Throwable { checkBool(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                List<Boolean> vList = List.of(true, false);
                // other argument tested with bool types only
                List<Object> wList = List.of(true, false);
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
}
