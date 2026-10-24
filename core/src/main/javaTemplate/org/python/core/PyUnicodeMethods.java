// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.core.PyUtil.NoConversion;
import org.python.core.PyUnicode.CodepointDelegate;
import static org.python.core.PyUnicode.adapt;

import java.util.Iterator;

// $OBJECT_GENERATOR$ PyUnicodeGenerator

/**
 * This class contains static methods implementing operations on the
 * Python {@code str} object, supplementary to those defined in
 * {@link PyUnicode}, and used internally by the run-time system. The
 * class is {@code public} only for technical reasons.
 */
public class PyUnicodeMethods {
    /*
     * These methods may cause creation of descriptors in the dictionary
     * of the type. Those with reserved names in the data model will
     * also contribute to the definition of special methods in the type.
     * 
     * Implementations of binary operations defined here will have
     * Object as their second argument, and should return
     * NotImplemented} when the type in that position is not supported.
     */
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
    private static boolean eq(CodepointDelegate a,
            CodepointDelegate b) {
        // Lengths must be equal
        if (a.length() != b.length()) { return false; }
        // Scan the code points in a and b
        Iterator<Integer> ib = b.iterator();
        for (int c : a) { if (c != ib.next()) { return false; } }
        return true;
    }
}
