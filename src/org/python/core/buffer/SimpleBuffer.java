package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Buffer API over a read-only one-dimensional array of one-byte items.
 */
public class SimpleBuffer extends BaseBuffer {

    /**
     * The strides array for this type is always a single element array with a 1 in it.
     */
    protected static final int[] SIMPLE_STRIDES = {1};

    /**
     * Provide an instance of <code>SimpleBuffer</code> with navigation variables partly
     * initialised, for sub-class use. One-dimensional arrays without slicing are C- and
     * F-contiguous. To complete initialisation, the sub-class normally must assign: {@link #buf}
     * and {@link #shape}[0], and call {@link #checkRequestFlags(int)} passing the consumer's
     * request flags.
     *
     * <pre>
     * this.buf = buf;              // Wraps exported data
     * this.shape[0] = n;           // Number of units in exported data
     * checkRequestFlags(flags);    // Check request is compatible with type
     * </pre>
     */
    protected SimpleBuffer() {
        super(CONTIGUITY | SIMPLE);
        // Initialise navigation
        shape = new int[1];
        strides = SIMPLE_STRIDES;
        // suboffsets is always null for this type.
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on a slice of a byte array, meeting the
     * consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleBuffer(int flags, byte[] storage, int offset, int size) throws PyException {
        this();
        // Wrap the exported data on a BufferPointer object
        this.buf = new BufferPointer(storage, offset, size);
        this.shape[0] = size;        // Number of units in exported data
        checkRequestFlags(flags);    // Check request is compatible with type
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on the entirety of a byte array, meeting
     * the consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleBuffer(int flags, byte[] storage) throws PyException {
        this(flags, storage, 0, storage.length);
    }

    @Override
    public boolean isReadonly() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public byte byteAt(int index) throws IndexOutOfBoundsException {
        // Implement directly: a bit quicker than the default
        return buf.storage[buf.offset + index];
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public int intAt(int index) throws IndexOutOfBoundsException {
        // Implement directly: a bit quicker than the default
        return 0xff & buf.storage[buf.offset + index];
    }

    @Override
    protected int calcIndex(int index) throws IndexOutOfBoundsException {
        return buf.offset + index;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public byte byteAt(int... indices) throws IndexOutOfBoundsException {
        checkDimension(indices.length);
        return byteAt(indices[0]);
    }

    @Override
    protected int calcIndex(int... indices) throws IndexOutOfBoundsException {
        // BaseBuffer implementation can be simplified since if indices.length!=1 we error.
        checkDimension(indices.length); // throws if != 1
        return calcIndex(indices[0]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
            throws IndexOutOfBoundsException {
        System.arraycopy(buf.storage, buf.offset + srcIndex, dest, destPos, length);
    }

    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length) {
        // Translate relative to underlying buffer
        int compIndex0 = buf.offset + start;
        // Check the arguments define a slice within this buffer
        checkInBuf(compIndex0, compIndex0 + length - 1);
        // Create the slice from the sub-range of the buffer
        return new SimpleView(getRoot(), flags, buf.storage, compIndex0, length);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation for slicing contiguous bytes in one
     * dimension. In that case, <i>x(i) = u(r+i)</i> for <i>i = 0..L-1</i> where u is the underlying
     * buffer, and <i>r</i> and <i>L</i> are the start and length with which <i>x</i> was created
     * from <i>u</i>. Thus <i>y(k) = u(r+s+km)</i>, that is, the composite offset is <i>r+s</i> and
     * the stride is <i>m</i>.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length, int stride) {

        if (stride == 1) {
            // Unstrided slice of simple buffer is itself simple
            return getBufferSlice(flags, start, length);

        } else {
            // Translate relative to underlying buffer
            int compIndex0 = buf.offset + start;
            // Check the slice sits within the present buffer (first and last indexes)
            checkInBuf(compIndex0, compIndex0 + (length - 1) * stride);
            // Construct a view, taking a lock on the root object (this or this.root)
            return new Strided1DBuffer.SlicedView(getRoot(), flags, buf.storage, compIndex0,
                                                  length, stride);
        }
    }

    @Override
    public BufferPointer getPointer(int index) {
        return new BufferPointer(buf.storage, buf.offset + index, 1);
    }

    @Override
    public BufferPointer getPointer(int... indices) {
        checkDimension(indices.length);
        return getPointer(indices[0]);
    }

    /**
     * A <code>SimpleBuffer.SimpleView</code> represents a contiguous subsequence of another
     * <code>SimpleBuffer</code>.
     */
    static class SimpleView extends SimpleBuffer {

        /** The buffer on which this is a slice view */
        PyBuffer root;

        /**
         * Construct a slice of a SimpleBuffer.
         *
         * @param root buffer which will be acquired and must be released ultimately
         * @param flags the request flags of the consumer that requested the slice
         * @param storage the array of bytes storing the implementation of the exporting object
         * @param offset where the data starts in that array (item[0])
         * @param size the number of bytes occupied
         */
        public SimpleView(PyBuffer root, int flags, byte[] storage, int offset, int size) {
            // Create a new SimpleBuffer on the buffer passed in (part of the root)
            super(flags, storage, offset, size);
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
