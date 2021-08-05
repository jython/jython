// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link Abstract} API class on a variety of types. We are
 * looking for correct behaviour in the cases attempted but mostly
 * testing the invocation of special methods through the operations
 * objects of the particular implementation classes.
 * <p>
 * To reach our main goal, we need only try enough types to exercise
 * every abstract method once in some type.
 */
@DisplayName("The API class Abstract")
class AbstractAPITest extends UnitTestSupport {

    /** A shorthand for the Python {@code int} type. */
    static PyType INT = PyLong.TYPE;

    /**
     * This abstract base forms a check-list of methods we mean to test.
     */
    abstract static class Standard {

        abstract void supports_repr() throws Throwable;

        abstract void supports_str() throws Throwable;

        abstract void supports_hash() throws Throwable;

        abstract void supports_getAttr_String() throws Throwable;

        abstract void supports_lookupAttr_String() throws Throwable;
    }

    /** The {@code int} implementations all behave like this. */
    abstract static class IntLike extends Standard {

        // Working variables for the tests
        Object zero;
        Object small;
        Object large;
        Object negative;
        List<Object> all;

        void setup(Object zero, Object small, Object large, Object negative) {
            this.zero = zero;
            this.small = small;
            this.large = large;
            this.negative = negative;
            this.all = List.of(zero, small, large, negative);
        }

        @Override
        @Test
        void supports_repr() throws Throwable {
            for (Object v : all) { assertEquals(v.toString(), Abstract.repr(v)); }
        }

        @Override
        @Test
        void supports_str() throws Throwable {
            for (Object v : all) { assertEquals(v.toString(), Abstract.str(v)); }
        }

        @Override
        @Test
        void supports_hash() throws Throwable {
            for (Object v : all) { assertEquals(v.hashCode(), Abstract.hash(v)); }
        }

        @Override
        @Test
        void supports_getAttr_String() throws Throwable {
            // An int has a "real" attribute that is itself
            // But we haven't implemented it yet
            // for (Object v : all) {
            // assertSame(v, Abstract.getAttr(v, "real"));
            // }
            // An int doesn't have a "foo" attribute
            assertThrows(AttributeError.class, () -> Abstract.getAttr(small, "foo"));
        }

        @Override
        @Test
        void supports_lookupAttr_String() throws Throwable {
            // An int doesn't have a foo attribute
            assertNull(Abstract.lookupAttr(small, "foo"));
        }
    }

    @Nested
    @DisplayName("with Integer argument")
    class WithInteger extends IntLike {

        @BeforeEach
        void setup() throws Throwable { setup(0, 42, Integer.MAX_VALUE, Integer.MIN_VALUE); }
    }

    @Nested
    @DisplayName("with BigInteger argument")
    class WithBigInteger extends IntLike {

        @BeforeEach
        void setup() throws Throwable {
            setup(BigInteger.valueOf(0), BigInteger.valueOf(42),
                    BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN),
                    BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.TEN));
        }
    }

}
