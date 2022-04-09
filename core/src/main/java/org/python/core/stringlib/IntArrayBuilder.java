package org.python.core.stringlib;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.stream.IntStream;

/**
 * An elastic buffer of integer values, somewhat like the
 * {@code java.lang.StringBuilder}, but for arrays of integers. The
 * client appends data and may finally take the built array, often
 * without copying the data.
 */
public final class IntArrayBuilder extends AbstractIntArrayBuilder.Forward {
    private int[] value;
    private int len = 0;
    private int max = 0;

    /**
     * Create an empty buffer of a defined initial capacity.
     *
     * @param capacity initially
     */
    public IntArrayBuilder(int capacity) { value = new int[capacity]; }

    /** Create an empty buffer of a default initial capacity. */
    public IntArrayBuilder() {
        value = EMPTY_INT_ARRAY;
    }

    @Override
    protected void appendUnchecked(int v) {
        value[len++] = v;
        max = Math.max(max, v);
    }

    @Override
    public int length() { return len; }

    @Override
    public int max() { return max; }

    /**
     * Ensure there is room for another {@code n} elements.
     *
     * @param n to make space for
     */
    @Override
    protected void ensure(int n) {
        if (len > value.length - n) {
            if (len == 0) {
                // Adding to empty: try exact fit.
                value = new int[n];
            } else {
                // Not empty: grow storage and copy into it
                int newSize = Math.max(value.length * 2, MINSIZE);
                int[] newValue = new int[newSize];
                System.arraycopy(value, 0, newValue, 0, len);
                value = newValue;
            }
        }
    }

    /**
     * Append the {@code int}s from the given stream.
     *
     * @param s stream to append from
     * @return this builder
     */
    public IntArrayBuilder append(IntStream s) {
        Spliterator.OfInt iter = s.spliterator();
        long N = iter.estimateSize();
        int n = (int)Math.min(Integer.MAX_VALUE, N);
        if (n == N) {
            ensure(n);
            iter.forEachRemaining((int c) -> appendUnchecked(c));
        } else {
            // Maybe N is unknown, else will overflow eventually ...
            iter.forEachRemaining((int c) -> append(c));
        }
        return this;
    }

    @Override
    protected int[] value() { return Arrays.copyOf(value, len); }

    @Override
    public int[] take() {
        int[] v;
        if (len == value.length) {
            // The array is exactly filled: use it without copy.
            v = value;
            value = EMPTY_INT_ARRAY;
        } else {
            // The array is partly filled: copy it and re-use it.
            v = Arrays.copyOf(value, len);
        }
        len = 0;
        max = 0;
        return v;
    }
}
