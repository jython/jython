package org.python.core;

import org.python.core.PyObjectUtil.NoConversion;
import static org.python.core.PyUnicode.adapt;

import java.util.Iterator;

// $OBJECT_GENERATOR$ PyUnicodeGenerator

/**
 * This class contains static methods implementing operations on the
 * Python {@code str} object, supplementary to those defined in
 * {@link PyUnicode}.
 * <p>
 * Implementations of binary operations defined here will have
 * {@code Object} as their second argument, and should return
 * {@link Py#NotImplemented} when the type in that position is not
 * supported.
 */
class PyUnicodeMethods {

    private PyUnicodeMethods() {}  // no instances

    // $SPECIAL_METHODS$ ---------------------------------------------

    // plumbing ------------------------------------------------------

    /**
     * Compare sequences for equality. This is a little simpler than
     * {@code compareTo}.
     * 
     * @param a sequence
     * @param b another
     * @return whether values equal
     */
    private static boolean eq(PySequence.OfInt a, PySequence.OfInt b) {
        // Lengths must be equal
        if (a.length() != b.length()) { return false; }
        // Scan the code points in a and b
        Iterator<Integer> ib = b.iterator();
        for (int c : a) { if (c != ib.next()) { return false; } }
        return true;
    }
}
