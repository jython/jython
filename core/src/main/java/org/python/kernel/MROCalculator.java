// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.python.core.PyErr;
import org.python.core.PyExc;
import org.python.core.PyType;
import org.python.internal.Util;

/**
 * Class to calculate one MRO and retain the result. We make this a
 * class (rather than just a static method) so that in the event of a
 * failure, the evidence is there about which bases it proved impossible
 * to merge.
 */
public class MROCalculator {
    private final Base[] bases;
    private Map<PyType, TypeSeen> typeIndex;

    private static class TypeSeen {
        int uses = 1;
    }

    /**
     * Create an object to store that MRO calculation.
     *
     * @param bases excluding the type under construction
     */
    MROCalculator(List<PyType> bases) {

        final int n = bases.size();
        this.typeIndex = new LinkedHashMap<>(4 * n);

        /*
         * Scan the MROs of the bases and record how many times each type has
         * appeared.
         */
        for (PyType base : bases) {
            for (PyType t : base.getMRO()) {
                TypeSeen seen = typeIndex.get(t);
                if (seen == null) {
                    // This is a type we have not seen before
                    typeIndex.put(t, new TypeSeen());
                } else {
                    // We know this type: count this use
                    seen.uses += 1;
                }
            }
        }

        /*
         * Create a table of base MROs (arrays of types) for the algorithm to
         * work with.
         */
        this.bases = new Base[n];
        for (int i = 0; i < n; i++) { this.bases[i] = new Base(bases.get(i).getMRO()); }
    }

    /**
     * Calculate an MRO from a type and an array of its bases, typically in
     * the definition of a type in Python. The algorithm is the C3
     * Linearisation (https://en.wikipedia.org/wiki/C3_linearization).
     *
     * @param type under construction
     * @param bases to analyse
     * @return the MRO as an array
     */
    // Compare CPython mro_implementation() in typeobject.c
    public static BaseType[] getMRO(BaseType type, BaseType[] bases) {
        if (bases.length == 1) {
            // Fast path when there is a single base.
            BaseType base = bases[0];
            if (base.getBase() == null) {
                // The one base is object
                return new BaseType[] {type, base};
            } else {
                // The one base has an MRO of its own: prepend type.
                BaseType[] baseMRO = base.getMRO();
                BaseType[] mro = Util.prepend(type, baseMRO);
                return mro;
            }
        } else {
            MROCalculator calc = new MROCalculator(Arrays.asList(bases));
            List<PyType> mro = calc.calculate();
            if (mro == null) {
                StringJoiner sj = new StringJoiner(",", "(", ")");
                for (PyType b : bases) { sj.add(b.getName()); }
                throw PyErr.format(PyExc.TypeError, NO_CONSISTENT_MRO, sj);
            }
            mro.add(0, type);
            return mro.toArray(new BaseType[mro.size()]);
        }
    }

    private static final String NO_CONSISTENT_MRO =
            "Cannot create a consistent method resolution order (MRO)" + " for bases %s";

    /**
     * Calculate the MRO using the tables created by the constructor.
     *
     * @return computed MRO or {@code null} if one not possible
     */
    List<PyType> calculate() {
        List<PyType> mro = new LinkedList<>();
        boolean done = bases.length == 0;
        while (!done) {
            // Find a head that is not in any tail
            PyType h = null;
            for (int i = 0; i < bases.length; i++) { if ((h = goodNextType(i)) != null) { break; } }
            // Now, if h is not still null, we found a type to add.
            if (h != null) {
                // Add to result
                mro.add(h);
                // Remove from every head (and check not done).
                done = true;
                for (int i = 0; i < bases.length; i++) {
                    Base b = bases[i];
                    if (b.peek() == h) { b.pop(); }
                    done &= b.empty();
                }
            } else {
                /*
                 * We're stuck: return empty array (or null?) State of bases array
                 * should be a clue, possibly the list of heads, since each head is
                 * behind another head somewhere on the list.
                 */
                mro = null;
                done = true;
            }
        }

        return mro;
    }

    /**
     * Return the types, found along the MRO of the original bases, that
     * failed to merge during {@link #calculate()}. (There will be at least
     * two.)
     *
     * @return the types that failed to merge.
     */
    Set<PyType> remainingHeads() {
        Set<PyType> remaining = new LinkedHashSet<>();
        for (Base b : bases) { if (!b.empty()) { remaining.add(b.peek()); } }
        return remaining;
    }

    /**
     * Inspect {@code bases[i]}, and if it has a head {@code h}, and that
     * head is not in the tail of any base, then return it.
     *
     * @param i position in the bases array to inspect
     * @return {@code bases[i].peek()} or -1
     */
    private PyType goodNextType(int i) {
        // The head of bases[i] is our candidate.
        PyType h = bases[i].peek();
        if (h != null) {
            // How many uses of h are there in total?
            int u = typeIndex.get(h).uses;
            // Count down each use of h in a head
            for (int j = 0; j < bases.length; j++) {
                if (bases[j].peek() == h) {
                    // If this accounts for the last use return h.
                    if (--u == 0) { return h; }
                }
            }
        }
        return null;
    }

    /**
     * A class that holds the MRO of one base class while we process it (see
     * {@link MROCalculator}, and an index to the first base we have not yet
     * dealt with.
     */
    private static class Base {
        private int head = 0;
        final PyType[] mro;

        /** Hold the residual MRO of a base. */
        Base(PyType[] mro) { this.mro = mro; }

        /** @return true iff no unconsumed types left. */
        boolean empty() {
            return head >= mro.length;
        }

        /** @return first remaining type or null on empty **/
        PyType peek() {
            return head < mro.length ? mro[head] : null;
        }

        /** Discard first in list. */
        void pop() { head++; }

        /** @return true iff {@code mro[head+1:]} contains type **/
        boolean tailContains(PyType t) {
            for (int i = head + 1; i < mro.length; i++) { if (mro[i] == t) { return true; } }
            return false;
        }

        @Override
        public String toString() { return Arrays.copyOfRange(mro, head, mro.length).toString(); }
    }
}
