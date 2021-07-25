package org.python.core;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

import org.python.core.PyObjectUtil.NoConversion;

// $OBJECT_GENERATOR$ PyLongGenerator

/**
 * This class contains static methods implementing operations on the
 * Python {@code int} object, supplementary to those defined in
 * {@link PyLong}.
 * <p>
 * These methods may cause creation of descriptors in the dictionary of
 * the type. Those with reserved names in the data model will also fill
 * slots in the {@code Operations} object for the type.
 * <p>
 * Implementations of binary operations defined here will have
 * {@code Object} as their second argument, and should return
 * {@link Py#NotImplemented} when the type in that position is not
 * supported.
 */
class PyLongMethods {

    private PyLongMethods() {}  // no instances

    // $SPECIAL_METHODS$ ---------------------------------------------

    // plumbing ------------------------------------------------------

    /**
     * Convert an {@code int} or its sub-class to a Java
     * {@code BigInteger}. Conversion may raise an exception that is
     * propagated to the caller. If the Java type of the {@code int} is
     * declared, generally there is a better option than this method. We
     * only use it for {@code Object} arguments. If the method throws
     * the special exception {@link NoConversion}, the caller must catch
     * it, and will normally return {@link Py#NotImplemented}.
     * 
     * @param v to convert
     * @return converted to {@code BigInteger}
     * @throws NoConversion v is not an {@code int}
     */
    private static BigInteger toBig(Object v) throws NoConversion {
        // Check against supported types, most likely first
        if (v instanceof Integer)
            return BigInteger.valueOf(((Integer) v).longValue());
        else if (v instanceof BigInteger)
            return (BigInteger) v;
        else if (v instanceof PyLong)
            return ((PyLong) v).value;
        else if (v instanceof Boolean)
            return (Boolean) v ? ONE : ZERO;

        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Reduce a {@code BigInteger} result to {@code Integer} if
     * possible. This makes it more likely the next operation will be
     * 32-bit.
     * 
     * @param r to reduce
     * @return equal value
     */
    static Object toInt(BigInteger r) {
        /*
         * Implementation note: r.intValueExact() is for exactly this
         * purpose, but building the ArithmeticException is a huge cost.
         * (2900ns is added to a 100ns __add__.) The compiler (as tested
         * in JDK 11.0.9) doesn't recognise that it can be optimised
         * to a jump. This version of toInt() adds around 5ns.
         */
        if (r.bitLength() < 32)
            return r.intValue();
        else
            return r;
    }
}
