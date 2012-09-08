package org.python.core.buffer;

import org.python.core.BufferPointer;
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
     * (whatever the sign of <code>stride>0</code>), valid for <code>0&lt;=i&lt;length</code>.
     * <p>
     * The constructed <code>PyBuffer</code> meets the consumer's expectations as expressed in the
     * <code>flags</code> argument, or an exception will be thrown if these are incompatible with
     * the type (e.g. the consumer does not specify that it understands the strides array). Note
     * that the actual range in the <code>storage</code> array, the lowest and highest index, is not
     * explicitly passed, but is implicit in <code>index0</code>, <code>length</code> and
     * <code>stride</code>. The caller is responsible for checking these fall within the array, or
     * the sub-range the caller is allowed to use.
     *
     * @param flags consumer requirements
     * @param storage raw byte array containing exported data
     * @param index0 index into storage of item[0]
     * @param length number of items in the slice
     * @param stride in between successive elements of the new PyBuffer
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public Strided1DWritableBuffer(int flags, byte[] storage, int index0, int length, int stride)
            throws PyException {

        // Arguments programme the object directly
        // this();
        this.shape[0] = length;
        this.index0 = index0;
        this.stride = stride;

        // Calculate buffer offset and size: start with distance of last item from first
        int d = (length - 1) * stride;

        if (stride >= 0) {
            // Positive stride: indexing runs from first item
            this.buf = new BufferPointer(storage, index0, 1 + d);
            if (stride <= 1) {
                // Really this is a simple buffer
                addFeatureFlags(CONTIGUITY);
            }
        } else {
            // Negative stride: indexing runs from last item
            this.buf = new BufferPointer(storage, index0 + d, 1 - d);
        }

        checkRequestFlags(flags);   // Check request is compatible with type
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public void storeAt(byte value, int index) throws IndexOutOfBoundsException, PyException {
        buf.storage[index0 + index * stride] = value;
    }

    /**
     * {@inheritDoc} <code>Strided1DWritableBuffer</code> provides a version optimised for strided
     * bytes in one dimension.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int length)
            throws IndexOutOfBoundsException, PyException {

        // Data is here in the buffers
        int s = srcPos;
        int d = index0 + destIndex * stride;

        // Strategy depends on whether items are laid end-to-end or there are gaps
        if (stride == 1) {
            // Straight copy of contiguous bytes
            System.arraycopy(src, srcPos, buf.storage, d, length);

        } else {
            // Discontiguous copy: single byte items
            int limit = d + length * stride;
            for (; d != limit; d += stride) {
                buf.storage[d] = src[s++];
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>Strided1DWritableBuffer</code> provides an implementation that returns a writable
     * slice.
     */
    public PyBuffer getBufferSlice(int flags, int start, int length, int stride) {

        // Translate relative to underlying buffer
        int compStride = this.stride * stride;
        int compIndex0 = index0 + start * stride;

        // Check the slice sits within the present buffer (first and last indexes)
        checkInBuf(compIndex0, compIndex0 + (length - 1) * compStride);

        // Construct a view, taking a lock on the root object (this or this.root)
        return new SlicedView(getRoot(), flags, buf.storage, compIndex0, length, compStride);
    }

    /**
     * A <code>Strided1DWritableBuffer.SlicedView</code> represents a discontiguous subsequence of a
     * simple buffer.
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
         * @param len number of items in the slice
         * @param stride in between successive elements of the new PyBuffer
         * @throws PyException (BufferError) when expectations do not correspond with the type
         */
        public SlicedView(PyBuffer root, int flags, byte[] storage, int index0, int len, int stride)
                throws PyException {
            // Create a new on the buffer passed in (part of the root)
            super(flags, storage, index0, len, stride);
            // Get a lease on the root PyBuffer (writable)
            this.root = root.getBuffer(FULL);
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
