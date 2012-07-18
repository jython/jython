package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.BufferProtocol;

/**
 * Buffer API over a writable one-dimensional array of one-byte items.
 */
public class SimpleBuffer extends SimpleReadonlyBuffer {

    /**
     * <code>SimpleBuffer</code> allows consumer requests that are the same as
     * <code>SimpleReadonlyBuffer</code>, with the addition of WRITABLE.
     */
    protected static final int ALLOWED_FLAGS = WRITABLE | SimpleReadonlyBuffer.ALLOWED_FLAGS;

    /**
     * Provide an instance of <code>SimpleBuffer</code> in a default, semi-constructed state. The
     * sub-class constructor takes responsibility for completing construction with a call to
     * {@link #assignCapabilityFlags(int, int, int, int)}.
     *
     * @param exporter the exporting object
     * @param buf wrapping the array of bytes storing the implementation of the object
     */
    protected SimpleBuffer(BufferProtocol exporter, BufferPointer buf) {
        super(exporter, buf);
    }

    /**
     * Provide an instance of SimpleBuffer meeting the consumer's expectations as expressed in the
     * flags argument.
     *
     * @param exporter the exporting object
     * @param buf wrapping the array of bytes storing the implementation of the object
     * @param flags consumer requirements
     */
    public SimpleBuffer(BufferProtocol exporter, BufferPointer buf, int flags) {
        super(exporter, buf);
        assignCapabilityFlags(flags, REQUIRED_FLAGS, ALLOWED_FLAGS, IMPLIED_FLAGS);
        fillInfo();
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public void storeAt(byte value, int index) {
        buf.storage[buf.offset + index] = value;
    }

    @Override
    public void storeAt(byte value, int... indices) {
        if (indices.length != 1) {
            checkDimension(indices);
        }
        storeAt(value, indices[0]);
    }

    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int length) {
        System.arraycopy(src, srcPos, buf.storage, buf.offset + destIndex, length);
    }

}
