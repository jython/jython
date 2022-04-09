package org.python.core.stringlib;

/**
 * An elastic buffer of byte values, somewhat like the
 * {@code java.lang.StringBuilder}, but for arrays of bytes. The
 * client appends data and may finally take the built array, often
 * without copying the data.
 */
public final class ByteArrayBuilder extends AbstractIntArrayBuilder.Forward {
    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private byte[] value;
    private int len = 0;
    private byte max = 0;

    /**
     * Create an empty buffer of a defined initial capacity.
     *
     * @param capacity initially
     */
    public ByteArrayBuilder(int capacity) { value = new byte[capacity]; }

    /** Create an empty buffer of a default initial capacity. */
    public ByteArrayBuilder() {
        value = EMPTY_BYTE_ARRAY;
    }

    @Override
    protected void appendUnchecked(int v) {
        value[len++] = (byte)v;
        max |= v;
    }

    @Override
    public int length() { return len; }

    @Override
    public int max() { return 0xff & max; }

    /**
     * Ensure there is room for another {@code n} elements.
     *
     * @param n to make space for
     */
    @Override
    protected void ensure(int n) {
        if (len + n > value.length) {
            int newSize = Math.max(value.length * 2, MINSIZE);
            byte[] newValue = new byte[newSize];
            System.arraycopy(value, 0, newValue, 0, len);
            value = newValue;
        }
    }

    @Override
    protected int[] value() {
        int[] v = new int[len];
        for (int i = 0; i < len; i++) { v[i] = 0xff & value[i]; }
        return v;
    }

    @Override
    public byte[] take() {
        byte[] v;
        if (len == value.length) {
            // The array is exactly filled: use it without copy.
            v = value;
            value = EMPTY_BYTE_ARRAY;
        } else {
            // The array is partly filled: copy it and re-use it.
            v = new byte[len];
            System.arraycopy(value, 0, v, 0, len);
        }
        len = 0;
        max = 0;
        return v;
    }

    /**
     * Append the 2 bytes of a {@code short} value big-endian.
     *
     * @param v the value
     */
    public void appendShortBE(int v) {
        ensure(2);
        appendUnchecked(v >>> 8);
        appendUnchecked(v);
    }

    /**
     * Append the 2 bytes of a {@code short} value little-endian.
     *
     * @param v the value
     */
    public void appendShortLE(int v) {
        ensure(4);
        appendUnchecked(v);
        appendUnchecked(v >>> 8);
    }

    /**
     * Append the 4 bytes of a {@code int} value big-endian.
     *
     * @param v the value
     */
    public void appendIntBE(int v) {
        ensure(4);
        appendUnchecked(v >>> 24);
        appendUnchecked(v >>> 16);
        appendUnchecked(v >>> 8);
        appendUnchecked(v);
    }

    /**
     * Append the 4 bytes of a {@code int} value little-endian.
     *
     * @param v the value
     */
    public void appendIntLE(int v) {
        ensure(4);
        appendUnchecked(v);
        appendUnchecked(v >>> 8);
        appendUnchecked(v >>> 16);
        appendUnchecked(v >>> 24);
    }

    /**
     * Append the 8 bytes of a {@code long} value big-endian.
     *
     * @param v the value
     */
    public void appendLongBE(long v) {
        appendIntBE((int)(v >>> 32));
        appendIntBE((int)v);
    }

    /**
     * Append the 8 bytes of a {@code long} value little-endian.
     *
     * @param v the value
     */
    public void appendLongLE(long v) {
        appendIntLE((int)v);
        appendIntLE((int)(v >>> 32));
    }

    /**
     * Append a specified number of bytes from a given offset in a
     * {@code byte} array.
     *
     * @param b the value
     * @param off index of the first byte written
     * @param n number of bytes to write
     */
    public void append(byte[] b, int off, int n) {
        ensure(n);
        for (int i = off; n > 0; n--) { appendUnchecked(b[i++]); }
    }
}
