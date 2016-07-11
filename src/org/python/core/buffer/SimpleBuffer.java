package org.python.core.buffer;

import org.python.core.BufferProtocol;
import org.python.core.PyBuffer;
import org.python.core.PyException;
import org.python.core.util.StringUtil;

/**
 * Buffer API over a read-only one-dimensional array of one-byte items.
 */
public class SimpleBuffer extends BaseArrayBuffer {

    /**
     * Provide an instance of <code>SimpleBuffer</code> with navigation variables initialised, for
     * sub-class use. The buffer ({@link #storage}, {@link #index0}), and the {@link #shape} array
     * will be initialised from the arguments (which are not checked for range). The
     * {@link #strides} is set for a one-byte stride. Only the call to
     * {@link #checkRequestFlags(int)}, passing the consumer's request flags really remains for the
     * sub-class constructor to do.
     *
     * <pre>
     * super(storage, index0, size);
     * checkRequestFlags(flags);        // Check request is compatible with type
     * </pre>
     *
     * @param obj exporting object (or <code>null</code>)
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws NullPointerException if <code>storage</code> is null
     */
    protected SimpleBuffer(BufferProtocol obj, byte[] storage, int index0, int size)
            throws PyException, ArrayIndexOutOfBoundsException {
        super(storage, CONTIGUITY | SIMPLE, index0, size, 1);
        this.obj = obj;
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on a slice of a byte array, meeting the
     * consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code> and <code>size</code> are
     *             inconsistent with <code>storage.length</code>
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleBuffer(int flags, BufferProtocol obj, byte[] storage, int index0, int size)
            throws PyException, ArrayIndexOutOfBoundsException, NullPointerException {
        this(obj, storage, index0, size);   // Construct checked SimpleBuffer
        checkRequestFlags(flags);           // Check request is compatible with type
        // Check arguments using the "all non-negative" trick
        if ((index0 | size | storage.length - (index0 + size)) < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on the entirety of a byte array, with
     * navigation variables initialised, for sub-class use. The buffer ( {@link #storage},
     * {@link #index0}), and the navigation ({@link #shape} array) will be initialised from the
     * array argument.
     *
     * @param obj exporting object (or <code>null</code>)
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws NullPointerException if <code>storage</code> is null
     */
    protected SimpleBuffer(BufferProtocol obj, byte[] storage) throws NullPointerException {
        this(obj, storage, 0, storage.length);
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on the entirety of a byte array, meeting
     * the consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws NullPointerException if <code>storage</code> is null
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleBuffer(int flags, BufferProtocol obj, byte[] storage) throws PyException,
            NullPointerException {
        this(obj, storage);             // Construct SimpleBuffer on whole array
        checkRequestFlags(flags);       // Check request is compatible with type
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public int getLen() {
        // Simplify for one-dimensional contiguous bytes
        return shape[0];
    }

    /**
     * {@inheritDoc}
     * <p>
     * In <code>SimpleBuffer</code> the calculation is specialised for one dimension, no striding,
     * and an item size of 1.
     */
    @Override
    public int byteIndex(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= shape[0]) {
            throw new IndexOutOfBoundsException();
        }
        return index0 + index;
    }

    // XXX Consider moving to clauses in getBufferSlice(int, int, int, int)
    // to avoid delegation loop where that delegates to this but in BaseBuffer the reverse.
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
     * <code>SimpleBuffer</code> provides an implementation for slicing contiguous bytes in one
     * dimension. In that case, <i>x(i) = u(r+i)</i> for <i>i = 0..L-1</i> where u is the underlying
     * buffer, and <i>r</i> and <i>L</i> are the start and count with which <i>x</i> was created
     * from <i>u</i>. Thus <i>y(k) = u(r+s+km)</i>, that is, the composite offset is <i>r+s</i> and
     * the stride is <i>m</i>.
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
            return new Strided1DBuffer.SlicedView(getRoot(), flags, storage, compIndex0, count,
                    stride);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int index) throws IndexOutOfBoundsException {
        return new Pointer(storage, index0 + index);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int... indices) throws IndexOutOfBoundsException {
        checkDimension(indices.length);
        return getPointer(indices[0]);
    }

    @Override
    public String toString() {
        // For contiguous bytes in one dimension we can avoid the intAt() calls
        return StringUtil.fromBytes(storage, index0, shape[0]);
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
            super(flags, root.getObj(), storage, offset, size);
            // Get a lease on the root PyBuffer
            this.root = root.getBuffer(FULL_RO);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }
    }
}
