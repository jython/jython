package org.python.core.buffer;

import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Buffer API over a writable one-dimensional array of one-byte items.
 */
public class SimpleWritableBuffer extends SimpleBuffer {

    /**
     * Provide an instance of <code>SimpleWritableBuffer</code>, on a slice of a byte array, meeting
     * the consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleWritableBuffer(int flags, byte[] storage, int index0, int size)
            throws PyException, NullPointerException {
        super(storage, index0, size);   // Construct checked SimpleBuffer
        addFeatureFlags(WRITABLE);
        checkRequestFlags(flags);       // Check request is compatible with type
    }

    /**
     * Provide an instance of <code>SimpleWritableBuffer</code>, on the entirety of a byte array,
     * meeting the consumer's expectations as expressed in the <code>flags</code> argument, which is
     * checked against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleWritableBuffer(int flags, byte[] storage) throws PyException, NullPointerException {
        super(storage);                 // Construct SimpleBuffer on whole array
        addFeatureFlags(WRITABLE);
        checkRequestFlags(flags);       // Check request is compatible with type
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public void storeAt(byte value, int index) {
        // Implement directly and don't ask whether read-only
        storage[index0 + index] = value;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public void storeAt(byte value, int... indices) {
        checkDimension(indices.length);
        storeAt(value, indices[0]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int length) {
        System.arraycopy(src, srcPos, storage, index0 + destIndex, length);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException {
        if (src.getLen() != getLen()) {
            throw differentStructure();
        }
        // Get the source to deliver efficiently to our byte storage
        src.copyTo(storage, index0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleWritableBuffer</code> provides an implementation ensuring the returned slice is
     * writable.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length) {
        if (length > 0) {
            // Translate relative to underlying buffer
            int compIndex0 = index0 + start;
            // Create the slice from the sub-range of the buffer
            return new SimpleView(getRoot(), flags, storage, compIndex0, length);
        } else {
            // Special case for length==0 where above logic would fail. Efficient too.
            return new ZeroByteBuffer.View(getRoot(), flags);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleWritableBuffer</code> provides an implementation ensuring the returned slice is
     * writable.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length, int stride) {

        if (stride == 1 || length < 2) {
            // Unstrided slice of simple buffer is itself simple
            return getBufferSlice(flags, start, length);

        } else {
            // Translate relative to underlying buffer
            int compIndex0 = index0 + start;
            // Construct a view, taking a lock on the root object (this or this.root)
            return new Strided1DWritableBuffer.SlicedView(getRoot(), flags, storage, compIndex0,
                    length, stride);
        }
    }

    /**
     * A <code>SimpleWritableBuffer.SimpleView</code> represents a contiguous subsequence of another
     * <code>SimpleWritableBuffer</code>.
     */
    static class SimpleView extends SimpleWritableBuffer {

        /** The buffer on which this is a slice view */
        PyBuffer root;

        /**
         * Construct a slice of a SimpleBuffer.
         *
         * @param root buffer which will be acquired and must be released ultimately
         * @param flags the request flags of the consumer that requested the slice
         * @param storage the array of bytes storing the implementation of the exporting object
         * @param index0 offset where the data starts in that array (item[0])
         * @param size the number of bytes occupied
         */
        public SimpleView(PyBuffer root, int flags, byte[] storage, int index0, int size) {
            // Create a new SimpleBuffer on the buffer passed in (part of the root)
            super(flags, storage, index0, size);
            // Get a lease on the root PyBuffer
            this.root = root.getBuffer(FULL_RO);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }

        @Override
        public void releaseAction() {
            // We have to release the root too if ours was final.
            root.release();
        }

    }

}
