package org.python.core.buffer;

import org.python.core.BufferProtocol;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Read-write buffer API over a one-dimensional array of one-byte items, that are evenly-spaced in a
 * storage array. The storage conventions are described in {@link Strided1DBuffer} of which this is
 * an extension providing write operations and a writable slice.
 */
public class Strided1DWritableBuffer extends Strided1DBuffer {

    /**
     * Provide an instance of <code>Strided1DWritableBuffer</code> on a particular array of bytes
     * specifying a starting index, the number of items in the result, and a byte-indexing stride.
     * The result of <code>byteAt(i)</code> will be equal to <code>storage[index0+stride*i]</code>
     * (whatever the sign of <code>stride>0</code>), valid for <code>0&lt;=i&lt;count</code>.
     * <p>
     * The constructed <code>PyBuffer</code> meets the consumer's expectations as expressed in the
     * <code>flags</code> argument, or an exception will be thrown if these are incompatible with
     * the type (e.g. the consumer does not specify that it understands the strides array). Note
     * that the actual range in the <code>storage</code> array, the lowest and highest index, is not
     * explicitly passed, but is implicit in <code>index0</code>, <code>count</code> and
     * <code>stride</code>. The caller is responsible for checking these fall within the array, or
     * the sub-range the caller is allowed to use.
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param storage raw byte array containing exported data
     * @param index0 index into storage of item[0]
     * @param count number of items in the slice
     * @param stride byte-index distance from one element to the next in the new PyBuffer
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code>, <code>count</code> and
     *             <code>stride</code> are inconsistent with <code>storage.length</code>
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public Strided1DWritableBuffer(int flags, BufferProtocol obj, byte[] storage, int index0,
            int count, int stride) throws ArrayIndexOutOfBoundsException, NullPointerException,
            PyException {
        super(obj, storage, index0, count, stride);
        addFeatureFlags(WRITABLE);
        checkRequestFlags(flags);   // Check request is compatible with type
    }

    /**
     * {@inheritDoc}
     * <p>
     * Declared <code>final</code> returning <code>true</code> in
     * <code>Strided1DWritableBuffer</code> to make checks unnecessary.
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
     * <code>Strided1DWritableBuffer</code> provides an implementation that returns a writable
     * slice.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int count, int stride) {

        if (count > 0) {
            // Translate start relative to underlying buffer
            int compStride = this.stride * stride;
            int compIndex0 = index0 + start * this.stride;
            // Construct a view, taking a lock on the root object (this or this.root)
            return new SlicedView(getRoot(), flags, storage, compIndex0, count, compStride);

        } else {
            // Special case for count==0 where above logic would fail. Efficient too.
            return new ZeroByteBuffer.View(getRoot(), flags);
        }
    }

    /**
     * A <code>Strided1DWritableBuffer.SlicedView</code> represents a non-contiguous subsequence of
     * a simple buffer.
     */
    static class SlicedView extends Strided1DWritableBuffer {

        /** The buffer on which this is a slice view */
        PyBuffer root;

        /**
         * Construct a slice of a one-dimensional byte buffer.
         *
         * @param root on which release must be called when this is released
         * @param flags consumer requirements
         * @param storage raw byte array containing exported data
         * @param index0 index into storage of item[0]
         * @param count number of items in the sliced view
         * @param stride in between successive elements of the new PyBuffer
         * @throws PyException (BufferError) when expectations do not correspond with the type
         */
        public SlicedView(PyBuffer root, int flags, byte[] storage, int index0, int count,
                int stride) throws PyException {
            // Create a new on the buffer passed in (part of the root)
            super(flags, root.getObj(), storage, index0, count, stride);
            // Get a lease on the root PyBuffer (writable)
            this.root = root.getBuffer(FULL);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }
    }
}
