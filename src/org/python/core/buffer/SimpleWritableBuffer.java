package org.python.core.buffer;

import org.python.core.BufferProtocol;
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
     * @param obj exporting object (or <code>null</code>)
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws PyException {@code BufferError} when expectations do not correspond with the type
     */
    public SimpleWritableBuffer(int flags, BufferProtocol obj, byte[] storage, int index0, int size)
            throws PyException, NullPointerException {
        super(obj, storage, index0, size);      // Construct checked SimpleBuffer
        addFeatureFlags(WRITABLE);
        checkRequestFlags(flags);               // Check request is compatible with type
    }

    /**
     * Provide an instance of <code>SimpleWritableBuffer</code>, on the entirety of a byte array,
     * meeting the consumer's expectations as expressed in the <code>flags</code> argument, which is
     * checked against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws PyException {@code BufferError} when expectations do not correspond with the type
     */
    public SimpleWritableBuffer(int flags, BufferProtocol obj, byte[] storage) throws PyException,
            NullPointerException {
        this(flags, obj, storage, 0, storage.length);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Declared <code>final</code> returning <code>true</code> in <code>SimpleWritableBuffer</code>
     * to make checks unnecessary.
     */
    @Override
    public final boolean isReadonly() {
        return false;
    }

    /** Do nothing: the buffer is writable. */
    @Override
    protected final void checkWritable() {}

    @Override
    protected void storeAtImpl(byte value, int byteIndex) {
        // Implement directly and don't ask whether read-only
        storage[byteIndex] = value;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleWritableBuffer</code> provides an implementation ensuring the returned slice is
     * writable.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int count) {
        if (count > 0) {
            // Translate relative to underlying buffer
            int compIndex0 = index0 + start;
            // Create the slice from the sub-range of the buffer
            return new SimpleView(getRoot(), flags, storage, compIndex0, count);
        } else {
            // Special case for count==0 where above logic would fail. Efficient too.
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
    public PyBuffer getBufferSlice(int flags, int start, int count, int stride) {

        if (stride == 1 || count < 2) {
            // Unstrided slice of simple buffer is itself simple
            return getBufferSlice(flags, start, count);

        } else {
            // Translate relative to underlying buffer
            int compIndex0 = index0 + start;
            // Construct a view, taking a lock on the root object (this or this.root)
            return new Strided1DWritableBuffer.SlicedView(getRoot(), flags, storage, compIndex0,
                    count, stride);
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
            super(flags, root.getObj(), storage, index0, size);
            // Get a lease on the root PyBuffer
            this.root = root.getBuffer(FULL_RO);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }
    }
}
