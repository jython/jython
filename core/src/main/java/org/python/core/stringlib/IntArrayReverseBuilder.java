package org.python.core.stringlib;

import java.util.Arrays;

/**
 * An elastic buffer of integer values, somewhat like the
 * {@code java.lang.StringBuilder}, but for arrays of integers. The
 * client prepends data, so the array builds right to left, and may
 * finally take the built array, often without copying the data.
 */
public final class IntArrayReverseBuilder extends AbstractIntArrayBuilder.Reverse {
    private int[] value;
    private int ptr = 0;
    private int max = 0;

    /**
     * Create an empty buffer of a defined initial capacity.
     *
     * @param capacity initially
     */
    public IntArrayReverseBuilder(int capacity) {
        value = new int[capacity];
        ptr = value.length;
    }

    /** Create an empty buffer of a default initial capacity. */
    public IntArrayReverseBuilder() {
        value = EMPTY_INT_ARRAY;
    }

    @Override
    protected void prependUnchecked(int v) {
        value[--ptr] = v;
        max = Math.max(max, v);
    }

    @Override
    public int length() { return value.length - ptr; }

    @Override
    public int max() { return max; }

    @Override
    protected void ensure(int n) {
        if (n > ptr) {
            if (ptr == value.length) {
                // Adding to empty: try exact fit.
                value = new int[n];
                ptr = n;
            } else {
                int len = value.length - ptr;
                int newSize = Math.max(value.length * 2, MINSIZE);
                int newPtr = newSize - len;
                int[] newValue = new int[newSize];
                System.arraycopy(value, ptr, newValue, newPtr, len);
                value = newValue;
                ptr = newPtr;
            }
        }
    }

    @Override
    protected int[] value() { return Arrays.copyOfRange(value, ptr, value.length); }

    @Override
    public int[] take() {
        int[] v;
        if (ptr == 0) {
            // The array is exactly filled: use it without copy.
            v = value;
            value = EMPTY_INT_ARRAY;
        } else {
            // The array is partly filled: copy it and re-use it.
            v = Arrays.copyOfRange(value, ptr, value.length);
            ptr = value.length;
        }
        max = 0;
        return v;
    }
}
