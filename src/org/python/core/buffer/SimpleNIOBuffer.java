package org.python.core.buffer;

import java.nio.ByteBuffer;

import org.python.core.BufferProtocol;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Buffer API over a read-only one-dimensional <code>java.nio.ByteBuffer</code> of one-byte items.
 */
public class SimpleNIOBuffer extends BaseNIOBuffer {

    /**
     * Provide an instance of <code>SimpleNIOBuffer</code> with navigation variables initialised,
     * for sub-class use. The buffer ({@link #storage}, {@link #index0}), and the {@link #shape}
     * array will be initialised from the arguments (which are checked for range). The
     * {@link #strides} is set for (one-byte) unit stride. Only the call to
     * {@link #checkRequestFlags(int)}, passing the consumer's request flags, really remains for the
     * sub-class constructor to do.
     *
     * <pre>
     * super(storage.duplicate(), index0, size);
     * checkRequestFlags(flags);        // Check request is compatible with type
     * </pre>
     *
     * @param obj exporting object (or <code>null</code>)
     * @param storage the <code>ByteBuffer</code> wrapping the exported object state. NOTE: this
     *            <code>PyBuffer</code> keeps a reference and may manipulate the position, mark and
     *            limit hereafter. Use {@link ByteBuffer#duplicate()} to give it an isolated copy.
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code> and <code>size</code> are
     *             inconsistent with <code>storage.capacity()</code>
     */
    protected SimpleNIOBuffer(BufferProtocol obj, ByteBuffer storage, int index0, int size)
            throws PyException, ArrayIndexOutOfBoundsException {
        super(storage, CONTIGUITY | SIMPLE, index0, size, 1);
        this.obj = obj;
        // Check arguments using the "all non-negative" trick
        if ((index0 | size | storage.capacity() - (index0 + size)) < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Provide an instance of <code>SimpleNIOBuffer</code>, on a slice of a {@link ByteBuffer},
     * meeting the consumer's expectations as expressed in the <code>flags</code> argument, which is
     * checked against the capabilities of the buffer type. No reference will be kept to the
     * <code>ByteBuffer</code> passed in. (It is duplicated.)
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param storage the <code>ByteBuffer</code> wrapping the exported object state
     * @param index0 offset where the data starts in that buffer (item[0])
     * @param size the number of bytes occupied
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code> and <code>size</code> are
     *             inconsistent with <code>storage.length</code>
     * @throws PyException {@code BufferError} when expectations do not correspond with the type
     */
    public SimpleNIOBuffer(int flags, BufferProtocol obj, ByteBuffer storage, int index0, int size)
            throws PyException, ArrayIndexOutOfBoundsException, NullPointerException {
        this(obj, storage.duplicate(), index0, size);   // Construct checked SimpleNIOBuffer
        checkRequestFlags(flags);                       // Check request is compatible with type
    }

    /**
     * Provide an instance of <code>SimpleNIOBuffer</code>, on the entirety of a {@link ByteBuffer},
     * with navigation variables initialised, for sub-class use. The buffer ( {@link #storage},
     * {@link #index0}), and the navigation ({@link #shape} array) will be initialised from the
     * argument.
     *
     * @param obj exporting object (or <code>null</code>)
     * @param storage the <code>ByteBuffer</code> wrapping the exported object state. NOTE: this
     *            <code>PyBuffer</code> keeps a reference and may manipulate the position, mark and
     *            limit hereafter. Use {@link ByteBuffer#duplicate()} to give it an isolated copy.
     * @throws NullPointerException if <code>storage</code> is null
     */
    protected SimpleNIOBuffer(BufferProtocol obj, ByteBuffer storage) throws NullPointerException {
        this(obj, storage, 0, storage.capacity());
    }

    /**
     * Provide an instance of <code>SimpleNIOBuffer</code>, on the entirety of a {@link ByteBuffer},
     * meeting the consumer's expectations as expressed in the <code>flags</code> argument, which is
     * checked against the capabilities of the buffer type. No reference will be kept to the
     * <code>ByteBuffer</code> passed in. (It is duplicated.)
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param storage the <code>ByteBuffer</code> wrapping the exported object state
     * @throws NullPointerException if <code>storage</code> is null
     * @throws PyException {@code BufferError} when expectations do not correspond with the type
     */
    public SimpleNIOBuffer(int flags, BufferProtocol obj, ByteBuffer storage) throws PyException,
            NullPointerException {
        this(obj, storage.duplicate()); // Construct SimpleNIOBuffer on whole ByteBuffer
        checkRequestFlags(flags);       // Check request is compatible with type
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>SimpleNIOBuffer</code> provides an implementation optimised for contiguous bytes in
     * one-dimension.
     */
    @Override
    public int getLen() {
        // Simplify for one-dimensional contiguous bytes
        return shape[0];
    }

    @Override
    public final int byteIndex(int index) throws IndexOutOfBoundsException {
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
     * <code>SimpleNIOBuffer</code> provides an implementation for slicing contiguous bytes in one
     * dimension. In that case, <i>x(i) = u(r+i)</i> for <i>i = 0..L-1</i> where u is the underlying
     * buffer, and <i>r</i> and <i>L</i> are the start and count with which <i>x</i> was created
     * from <i>u</i>. Thus <i>y(k) = u(r+s+km)</i>, that is, the composite offset is <i>r+s</i> and
     * the stride is <i>m</i>.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int count, int stride) {

        if (stride == 1 || count < 2) {
            // Unstrided slice of simple buffer is special case
            return getBufferSlice(flags, start, count);

        } else {
            // Translate relative to underlying buffer
            int compIndex0 = index0 + start;
            // Construct a view, taking a lock on the root object (this or this.root)
            return new Strided1DNIOBuffer.SlicedView(getRoot(), flags, storage, compIndex0, count,
                    stride);
        }
    }

    /**
     * A <code>SimpleNIOBuffer.SimpleView</code> represents a contiguous subsequence of another
     * <code>SimpleNIOBuffer</code>.
     */
    static class SimpleView extends SimpleNIOBuffer {

        /** The buffer on which this is a slice view */
        PyBuffer root;

        /**
         * Construct a slice of a SimpleNIOBuffer.
         *
         * @param root buffer which will be acquired and must be released ultimately
         * @param flags the request flags of the consumer that requested the slice
         * @param storage <code>ByteBuffer</code> wrapping exported data (no reference kept)
         * @param offset where the data starts in that buffer (item[0])
         * @param count the number of items in the sliced view
         */
        public SimpleView(PyBuffer root, int flags, ByteBuffer storage, int offset, int count) {
            // Create a new SimpleNIOBuffer on the buffer passed in (part of the root)
            super(flags, root.getObj(), storage, offset, count);
            // Get a lease on the root PyBuffer
            this.root = root.getBuffer(FULL_RO);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }
    }
}
