package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.BufferProtocol;

/**
 * Buffer API over a one-dimensional array of one-byte items providing read-only API. A writable
 * simple buffer will extend this implementation.
 */
public class SimpleReadonlyBuffer extends BaseBuffer {

    /**
     * Using the PyBUF constants, express capabilities the consumer must request if it is to
     * navigate the storage successfully. (None.)
     */
    public static final int REQUIRED_FLAGS = 0;
    /**
     * Using the PyBUF constants, express capabilities the consumer may request so it can navigate
     * the storage in its chosen way. The buffer instance has to implement these mechanisms if and
     * only if they are requested. (FORMAT | ND | STRIDES | INDIRECT)
     */
    public static final int ALLOWED_FLAGS = FORMAT | ND | STRIDES | INDIRECT;
    /**
     * Using the PyBUF constants, express capabilities the consumer doesn't need to request because
     * they will be there anyway. (One-dimensional arrays (including those sliced with step size
     * one) are C- and F-contiguous.)
     */
    public static final int IMPLIED_FLAGS = CONTIGUITY;
    /**
     * The strides array for this type is always a single element array with a 1 in it.
     */
    protected static final int[] SIMPLE_STRIDES = {1};

    /**
     * Partial counterpart to CPython <code>PyBuffer_FillInfo()</code> specific to the simple type
     * of buffer and called from the constructor. The base constructor will already have been
     * called, filling {@link #buf} and {@link #obj}. And the method
     * {@link #assignCapabilityFlags(int, int, int, int)} has set {@link #capabilityFlags}.
     */
    protected void fillInfo() {
        /*
         * We will already have called: assignCapabilityFlags(flags, requiredFlags, allowedFlags,
         * impliedFlags); So capabilityFlags holds the requests for shape, strides, writable, etc..
         */
        // Difference from CPython: never null, even when the consumer doesn't request it
        shape = new int[1];
        shape[0] = getLen();

        // Following CPython: provide strides only when the consumer requests it
        if ((capabilityFlags & STRIDES) == STRIDES) {
            strides = SIMPLE_STRIDES;
        }

        // Even when the consumer requests suboffsets, the exporter is allowed to supply null.
        // In theory, the exporter could require that it be requested and still supply null.
    }

    /**
     * Provide an instance of <code>SimpleReadonlyBuffer</code> in a default, semi-constructed
     * state. The sub-class constructor takes responsibility for completing construction including a
     * call to {@link #assignCapabilityFlags(int, int, int, int)}.
     *
     * @param exporter the exporting object
     * @param buf wrapping the array of bytes storing the implementation of the object
     */
    protected SimpleReadonlyBuffer(BufferProtocol exporter, BufferPointer buf) {
        super(exporter, buf);
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
        super(exporter, buf);
        assignCapabilityFlags(flags, REQUIRED_FLAGS, ALLOWED_FLAGS, IMPLIED_FLAGS);
        fillInfo();
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
