package org.python.core;

import java.math.BigInteger;
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

    @Nested
    @DisplayName("The slot wrapper '__neg__'")
    class Slot__neg__ extends SlotWrapperTestBase {

        final String NAME = "__neg__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends UnaryTest<Object, Object> {

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
        class OfBool extends UnaryTest<Object, Boolean> {

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
    class Slot__repr__ extends SlotWrapperTestBase {

        final String NAME = "__repr__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends UnaryTest<String, Object> {

            @Override
            String expected(Object x) { return Integer.toString(toInt(x)); }

            @Override
            void check(String exp, Object r) throws Throwable { checkStr(exp, r); }

            @BeforeEach
            void setup() throws AttributeError, Throwable {
                // x is Integer, BigInteger, PyLong but not Boolean
                Integer ix = 42;
                super.setup(PyLong.TYPE, NAME, List.of(ix, BigInteger.valueOf(ix), newPyLong(ix)));
            }
        }

        @Nested
        @DisplayName("of 'bool' objects")
        class OfBool extends UnaryTest<String, Boolean> {

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
    class Slot__hash__ extends SlotWrapperTestBase {

        final String NAME = "__hash__";

        @Nested
        @DisplayName("of 'int' objects")
        class OfInt extends LenTest<Integer, Object> {

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
        class OfBool extends LenTest<Integer, Boolean> {

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
