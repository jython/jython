// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.stringlib;

import static org.python.stringlib.ByteArrayBuilder.EMPTY_BYTE_ARRAY;

/**
 * An elastic buffer of byte values, somewhat like the
 * {@code java.lang.StringBuilder}, but for arrays of bytes. The client
 * prepends data, so the array builds right to left, and may finally
 * take the built array, often without copying the data.
 */
public final class ByteArrayReverseBuilder extends AbstractIntArrayBuilder.Reverse {
    private byte[] value;
    private int ptr = 0;
    private byte max = 0;

    /**
     * Create an empty buffer of a defined initial capacity.
     *
     * @param capacity initially
     */
    public ByteArrayReverseBuilder(int capacity) {
        value = new byte[capacity];
        ptr = value.length;
    }

    /** Create an empty buffer of a default initial capacity. */
    public ByteArrayReverseBuilder() {
        value = EMPTY_BYTE_ARRAY;
    }

    @Override
    protected void prependUnchecked(int v) {
        value[--ptr] = (byte)v;
        max |= v;
    }

    @Override
    public int length() { return value.length - ptr; }

    @Override
    public int max() { return 0xff & max; }

    @Override
    protected void ensure(int n) {
        if (n > ptr) {
            int len = value.length - ptr;
            int newSize = Math.max(value.length * 2, MINSIZE);
            int newPtr = newSize - len;
            byte[] newValue = new byte[newSize];
            System.arraycopy(value, ptr, newValue, newPtr, len);
            value = newValue;
            ptr = newPtr;
        }
    }

    @Override
    protected int[] value() {
        int len = value.length - ptr;
        int[] v = new int[len];
        for (int i = 0; i < len; i++) { v[i] = 0xff & value[ptr + i]; }
        return v;
    }

    @Override
    public byte[] take() {
        byte[] v;
        if (ptr == 0) {
            // The array is exactly filled: use it without copy.
            v = value;
            value = EMPTY_BYTE_ARRAY;
        } else {
            // The array is partly filled: copy it and re-use it.
            int len = value.length - ptr;
            v = new byte[len];
            System.arraycopy(value, ptr, v, 0, len);
            ptr = value.length;
        }
        max = 0;
        return v;
    }

    @Override
    public void reset() {
        ptr = value.length;
        max = 0;
    }

}
