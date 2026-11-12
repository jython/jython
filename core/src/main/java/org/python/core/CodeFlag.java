// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Characteristics of a {@code PyCode} (related to CPython co_flags).
 * These are significant characteristics of a function when constructed
 * with a code object as its body. These are not all relevant to all
 * code types.
 */
public enum CodeFlag {
    /** The code uses fast local local variables, not a map. */
    OPTIMIZED(PyCF.CO_OPTIMIZED),
    /** A new {@code dict} should be created for local variables. */
    // Never acted on in CPython, but set for functions.
    NEWLOCALS(PyCF.CO_NEWLOCALS),
    /** The function has a collector for positional arguments */
    VARARGS(PyCF.CO_VARARGS),
    /** The function has a collector for keyword arguments */
    VARKEYWORDS(PyCF.CO_VARKEYWORDS),
    /** The code is for a nested function. */
    NESTED(PyCF.CO_NESTED),
    /**
     * The code is for a generator function, i.e. a generator object is
     * returned when the code object is executed.
     */
    GENERATOR(PyCF.CO_GENERATOR),
    /**
     * The code is for a coroutine function (defined with
     * {@code async def}). When the code object is executed it returns a
     * coroutine object.
     */
    COROUTINE(PyCF.CO_COROUTINE),
    /**
     * The flag is used to transform generators into generator-based
     * coroutines. Generator objects with this flag can be used in
     * {@code await} expression, and can {@code yield from} coroutine
     * objects. See PEP 492 for more details.
     */
    ITERABLE_COROUTINE(PyCF.CO_ITERABLE_COROUTINE),
    /**
     * The code object is an asynchronous generator function. When the code
     * object is executed it returns an asynchronous generator object. See
     * PEP 525 for more details.
     */
    ASYNC_GENERATOR(PyCF.CO_ASYNC_GENERATOR);

    private CodeFlag(int flagbit) {
        assert Integer.bitCount(flagbit) == 1;
        this.co_flag = flagbit;
    }

    /**
     * CPython equivalent bit-mask for use with {@code code.co_flags}.
     */
    public final int co_flag;

    /**
     * Convert a CPython-style {@code co_flags} specifier to
     * {@code CodeFlag}s. We need this conversion because these bits are
     * Python API.
     *
     * @param bits bits specifying code flags
     * @return corresponding code flags (as a set)
     */
    static EnumSet<CodeFlag> setFromBits(int bits) {
        // List the bits set as code flags.
        ArrayList<CodeFlag> flags = new ArrayList<>();
        for (CodeFlag t : CodeFlag.values()) {
            int m = t.co_flag;
            if ((bits & m) != 0) { flags.add(t); bits &= ~m; }
        }
        // Check we translated all the bits
        if (bits != 0) {
            String msg = String.format("Undefined bits 0x%04x specified for code object", bits);
            throw new IllegalArgumentException(msg);
        }
        // Return as a set
        return flags.isEmpty() ? EnumSet.noneOf(CodeFlag.class) : EnumSet.copyOf(flags);
    }

    /**
     * CPython-style {@code co_flags} constamnts. We need these conversion
     * because these bit values are Python API (through the attribute
     * {@code code.co_flags}, and also in the compiler. In Jython
     * implementation and extensions, however, we use the enumeration
     * {@code CodeFlag} and its {@code EnumSet}.
     */
    // Compare CPython CO_* constants in code.h
    static class PyCF {
        /** The code uses fast local local variables, not a map. */
        static final int CO_OPTIMIZED = 0x0001;
        /** A new {@code dict} should be created for local variables. */
        // NEWLOCALS is never acted on in CPython (but set for
        // functions)
        static final int CO_NEWLOCALS = 0x0002;
        /** The function has a collector for positional arguments. */
        static final int CO_VARARGS = 0x0004;
        /** The function has a collector for keyword arguments */
        static final int CO_VARKEYWORDS = 0x0008;
        /** The code is for a nested function. */
        static final int CO_NESTED = 0x0010;
        /**
         * The code is for a generator function, i.e. a generator object is
         * returned when the code object is executed.
         */
        static final int CO_GENERATOR = 0x0020;
        /**
         * The code is for a coroutine function (defined with
         * {@code async def}). When the code object is executed it returns a
         * coroutine object.
         */
        static final int CO_COROUTINE = 0x0080;
        /**
         * The flag is used to transform generators into generator-based
         * coroutines. Generator objects with this flag can be used in
         * {@code await} expression, and can {@code yield from} coroutine
         * objects. See PEP 492 for more details.
         */
        static final int CO_ITERABLE_COROUTINE = 0x0100;
        /**
         * The code object is an asynchronous generator function. When the code
         * object is executed it returns an asynchronous generator object. See
         * PEP 525 for more details.
         */
        static final int CO_ASYNC_GENERATOR = 0x0200;

        /**
         * Convert a set of code flags to a CPython-style {@code co_flags}
         * bitmap.
         *
         * @param flags specifying code properties
         * @return corresponding Python API bit map
         */
        static int from(EnumSet<CodeFlag> flags) {
            // Reduce the set to bits.
            int bits = 0;
            for (CodeFlag f : flags) { bits |= f.co_flag; }
            return bits;
        }
    }
}
