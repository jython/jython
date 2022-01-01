package org.python.core.stringlib;

import java.util.Arrays;

public final class IntArrayReverseBuilder
        extends AbstractIntArrayBuilder.Reverse {
    private int[] value;
    private int ptr = 0;

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
    protected Reverse prependUnchecked(int v) {
        value[--ptr] = v;
        return this;
    }

    @Override
    public int length() { return value.length - ptr; }

    @Override
    protected void ensure(int n) {
        if (n > ptr) {
            int len = value.length - ptr;
            int newSize = Math.max(value.length * 2, MINSIZE);
            int newPtr = newSize - len;
            int[] newValue = new int[newSize];
            System.arraycopy(value, ptr, newValue, newPtr, len);
            value = newValue;
            ptr = newPtr;
        }
    }

    @Override
    protected int[] value() {
        return Arrays.copyOfRange(value, ptr, value.length);
    }

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
        return v;
    }
}
