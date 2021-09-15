// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

// $OBJECT_GENERATOR$ PyFloatGenerator

import org.python.core.PyObjectUtil.NoConversion;
import static org.python.core.PyFloat.nonzero;
import static org.python.core.PyFloat.floordiv;
import static org.python.core.PyFloat.mod;
import static org.python.core.PyFloat.divmod;

/**
 * This class contains static methods implementing operations on the
 * Python {@code float} object, supplementary to those defined in
 * {@link PyFloat}.
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
class PyFloatMethods {

    PyFloatMethods() {}  // no instances

    // $SPECIAL_METHODS$ ---------------------------------------------

    // plumbing ------------------------------------------------------

    /**
     * Convert an object to a Java double. Conversion to a double may
     * raise an exception that is propagated to the caller. If the
     * method throws the special exception {@link NoConversion}, the
     * caller must catch it, and will normally return
     * {@link Py#NotImplemented}.
     * 
     * @param v to convert
     * @return converted to {@code double}
     * @throws NoConversion v is not a {@code float} or {@code int}
     * @throws OverflowError v is an {@code int} too large to be a
     *     {@code float}
     */
    static double toDouble(Object v) throws NoConversion, OverflowError {
        // Check against supported types, most likely first
        if (v instanceof Double)
            return ((Double) v).doubleValue();
        else if (v instanceof PyFloat)
            return ((PyFloat) v).value;
        else
            // BigInteger, PyLong, Boolean, etc.
            // or throw PyObjectUtil.NO_CONVERSION;
            return PyLong.convertToDouble(v);
    }
}
