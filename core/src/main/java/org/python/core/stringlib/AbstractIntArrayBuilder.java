package org.python.core.stringlib;

import java.util.Iterator;
import java.util.ListIterator;

import org.python.core.PySequence;

/**
 * The base of two classes that that provide elastic buffers of
 * integer values, somewhat like the
 * {@code java.lang.StringBuilder}, but for arrays of integers.
 * There is an abstract base for arrays to which a client appends,
 * and one for arrays to which the client prepends new values.
 * <p>
 * The particular virtue of these classes is that, if the ultimate
 * size of the built array may be known in advance, then the result
 * may be returned without a copy, using {@link #take()}.
 */
public abstract class AbstractIntArrayBuilder {

    /** An empty array of int for builder initial state, etc.. */
    protected static final int[] EMPTY_INT_ARRAY = new int[0];

    /** Number of elements by default. */
    protected static final int MINSIZE = 16;

    /**
     * The number of elements currently
     *
     * @return the number of elements currently.
     */
    public abstract int length();

    /**
     * The maximum value stored.
     *
     * @implNote The motivation for this is to know the range of code
     *     point values when representing a string. An over-estimate
     *     would be ok.)
     *
     * @return The maximum int stored.
     */
    public abstract int max();

    /**
     * Ensure there is room for another {@code n} elements. In general,
     * this will mean allocating new storage (of a carefully-chosen size
     * &ge; {@code n+length()}) and copying the existing contents to it.
     *
     * @param n to make additional space for
     */
    protected abstract void ensure(int n);

    /**
     * An array of the elements in the buffer (not modified by methods
     * on this object hereafter).
     *
     * @return the elements in the buffer
     */
    protected abstract int[] value();

    /**
     * Provide the contents as an array and reset the builder to empty.
     * (This is a "destructive read".) The return type is Object in
     * order that sub-classes may define the specific type (for example,
     * {@code int[]} or {@code byte[]}).
     *
     * @implNote In many implementations, if the client has chosen an
     *     initial capacity exactly matching {@link #length()} at the
     *     time this method is called, then the result may be returned
     *     without a copy. (This must be the last remaining reference to
     *     the array originally allocated: it must be impossible for the
     *     the builder to re-use it.) Otherwise, returning a new array
     *     is inevitable.
     *
     * @return the contents as a Python {@code str}
     */
    public abstract Object take();

    /**
     * Provide the contents as a Java {@code String} (non-destructively,
     * but inefficiently).
     */
    @Override
    public String toString() {
        int[] v = value();
        return new String(v, 0, v.length);
    }

    /**
     * Abstract base of integer array builders that append to their
     * content, building the result left to right. Implementations need
     * only define {@link #appendUnchecked(int)}.
     */
    public static abstract class Forward extends AbstractIntArrayBuilder {
        /**
         * Append one element without ensuring that there is space. This
         * method is for use when it is known that there is space for the
         * element, for example, inside a loop before which when
         * {@link #ensure(int)} has been called.
         *
         * @param v to append
         */
        abstract protected void appendUnchecked(int v);

        /**
         * Append one element.
         *
         * @param v to append
         * @return this builder
         */
        public Forward append(int v) {
            ensure(1);
            appendUnchecked(v);
            return this;
        }

        /**
         * Append all the elements from a sequence.
         *
         * @param seq from which to take items
         * @return this builder
         */
        public Forward append(PySequence.OfInt seq) {
            // Make sure there is room: do it once
            int n = seq.length();
            ensure(n);
            // Fill (forwards) from the current position
            for (int i = 0; i < n; i++) { appendUnchecked(seq.getInt(i)); }
            return this;
        }

        /**
         * Append all the elements available from an iterator.
         *
         * @param iter from which to take items
         * @return this builder
         */
        public Forward append(Iterator<Integer> iter) {
            // We don't know what capacity to ensure.
            while (iter.hasNext()) { append(iter.next()); }
            return this;
        }
    }

    /**
     * Abstract base of integer array builders that prepend to their
     * content, building the result right to left. Implementations need
     * only define {@link #prependUnchecked(int)}.
     */
    public static abstract class Reverse extends AbstractIntArrayBuilder {
        /**
         * Prepend one element without ensuring that there is space. This
         * method is for use when it is known that there is space for the
         * element, for example, inside a loop before which when
         * {@link #ensure(int)} has been called.
         *
         * @param v to prepend
         */
        protected abstract void prependUnchecked(int v);

        /**
         * Prepend one element.
         *
         * @param v to prepend
         * @return this builder
         */
        public Reverse prepend(int v) {
            ensure(1);
            prependUnchecked(v);
            return this;
        }

        /**
         * Prepend all the elements from a sequence. The sequence is not
         * reversed by this: it is prepended the right way around. After the
         * call {@code seq[0]} is first in the buffer.
         *
         * @param seq from which to take items
         * @return this builder
         */
        public Reverse prepend(PySequence.OfInt seq) {
            // Make sure there is room: do it once
            int n = seq.length();
            ensure(n);
            // Fill (backwards) from the current position
            while (n > 0) { prependUnchecked(seq.getInt(--n)); }
            return this;
        }

        /**
         * Prepend all the elements available from an iterator, working
         * backwards with {@code iter.previous()}.
         *
         * @param iter from which to take items
         * @return this builder
         */
        public Reverse prepend(ListIterator<Integer> iter) {
            // We don't know what capacity to ensure.
            while (iter.hasPrevious()) { prepend(iter.previous()); }
            return this;
        }
    }
}
