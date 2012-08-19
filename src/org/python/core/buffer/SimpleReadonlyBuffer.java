package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.BufferProtocol;

/**
 * Buffer API over a one-dimensional array of one-byte items providing read-only API. A writable
 * simple buffer extends this implementation.
 */
public class SimpleReadonlyBuffer extends BaseBuffer {

    /**
     * Using the PyBUF constants, express capabilities implied by the type, therefore ok for the
     * consumer to request. (One-dimensional arrays, including those sliced with step size one, are
     * C- and F-contiguous.) Also express capabilities the consumer must request if it is to
     * navigate the storage successfully. (None required for simple buffers.)
     */
    static final int FEATURE_FLAGS = CONTIGUITY | FORMAT | 0;
    /**
     * The strides array for this type is always a single element array with a 1 in it.
     */
    protected static final int[] SIMPLE_STRIDES = {1};

    /**
     * Provide an instance of <code>SimpleReadonlyBuffer</code> in a default, semi-constructed
     * state. The sub-class constructor takes responsibility for checking flags and
     * completing construction. If the passed in <code>buf</code> is null,
     * fields <code>buf</code> and <code>shape[0]</code> must be set by the sub-class.
     *
     * @param exporter the exporting object
     * @param buf wrapping the array of bytes storing the implementation of the object
     */
    protected SimpleReadonlyBuffer(BufferProtocol exporter, BufferPointer buf) {
        super(exporter);
        // Difference from CPython: shape and strides are always provided
        shape = new int[1];
        if (buf != null) {
            this.buf = buf;
            shape[0] = buf.size;
        }
        strides = SIMPLE_STRIDES;
        // suboffsets is always null for this type.
    }

    /**
     * Provide an instance of SimpleReadonlyBuffer meeting the consumer's expectations as expressed
     * in the flags argument.
     *
     * @param exporter the exporting object
     * @param buf wrapping the array of bytes storing the implementation of the object
     * @param flags consumer requirements
     */
    public SimpleReadonlyBuffer(BufferProtocol exporter, BufferPointer buf, int flags) {
        this(exporter, buf);
        setFeatureFlags(FEATURE_FLAGS);
        checkRequestFlags(flags);
    }

    @Override
    public int getNdim() {
        return 1;
    }

    @Override
    public byte byteAt(int index) throws IndexOutOfBoundsException {
        // offset is not necessarily zero
        return buf.storage[buf.offset + index];
    }

    @Override
    public int intAt(int index) throws IndexOutOfBoundsException {
        // Implement directly: a bit quicker than the default
        return 0xff & buf.storage[buf.offset + index];
    }

    @Override
    public byte byteAt(int... indices) throws IndexOutOfBoundsException {
        if (indices.length != 1) {
            checkDimension(indices);
        }
        return byteAt(indices[0]);
    }

    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
            throws IndexOutOfBoundsException {
        System.arraycopy(buf.storage, buf.offset + srcIndex, dest, destPos, length);
    }

    @Override
    public BufferPointer getPointer(int index) {
        return new BufferPointer(buf.storage, buf.offset + index, 1);
    }

    @Override
    public BufferPointer getPointer(int... indices) {
        if (indices.length != 1) {
            checkDimension(indices);
        }
        return getPointer(indices[0]);
    }

}
