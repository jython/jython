package org.python.core.stringlib;

import java.util.Arrays;

public final class IntArrayBuilder extends AbstractIntArrayBuilder.Forward {
    private int[] value;
    private int len = 0;

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
    protected Forward appendUnchecked(int v) {
        value[len++] = v;
        return this;
    }

    @Override
    public int length() { return len; }

    /**
     * Ensure there is room for another {@code n} elements.
     *
     * @param n to make space for
     */
    @Override
    protected void ensure(int n) {
        if (len + n > value.length) {
            int newSize = Math.max(value.length * 2, MINSIZE);
            int[] newValue = new int[newSize];
            System.arraycopy(value, 0, newValue, 0, len);
            value = newValue;
        }
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
        return v;
    }
}
