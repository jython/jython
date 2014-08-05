package org.python.core.buffer;

import java.nio.ByteBuffer;

import org.python.core.PyBuffer;
import org.python.core.PyException;
import org.python.core.util.StringUtil;

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
     * initialised, for sub-class use. One-dimensional arrays without strides are C- and
     * F-contiguous. To complete initialisation, the sub-class must normally assign the buffer (
     * {@link #storage}, {@link #index0}), and the navigation ({@link #shape} array), and then call
     * {@link #checkRequestFlags(int)} passing the consumer's request flags.
     */
    protected SimpleBuffer() {
        super(CONTIGUITY | SIMPLE);
        // Initialise navigation
        shape = new int[1];
        strides = SIMPLE_STRIDES;
        // suboffsets is always null for this type.
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code> with navigation variables initialised, for
     * sub-class use. The buffer ({@link #storage}, {@link #index0}), and the {@link #shape} array
     * will be initialised from the arguments (which are checked for range). The {@link #strides} is
     * set for (one-byte) unit stride. Only the call to {@link #checkRequestFlags(int)}, passing the
     * consumer's request flags really remains for the sub-class constructor to do.
     *
     * <pre>
     * super(storage, index0, size);
     * checkRequestFlags(flags);        // Check request is compatible with type
     * </pre>
     *
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code> and <code>size</code> are
     *             inconsistent with <code>storage.length</code>
     */
    // XXX: "for sub-class use" = should be protected?
    public SimpleBuffer(byte[] storage, int index0, int size) throws PyException,
            ArrayIndexOutOfBoundsException {
        this();
        this.storage = storage;         // Exported data
        this.index0 = index0;           // Index to be treated as item[0]
        this.shape[0] = size;           // Number of items in exported data

        // Check arguments using the "all non-negative" trick
        if ((index0 | size | storage.length - (index0 + size)) < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on a slice of a byte array, meeting the
     * consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param index0 offset where the data starts in that array (item[0])
     * @param size the number of bytes occupied
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code> and <code>size</code> are
     *             inconsistent with <code>storage.length</code>
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleBuffer(int flags, byte[] storage, int index0, int size) throws PyException,
            ArrayIndexOutOfBoundsException, NullPointerException {
        this(storage, index0, size);    // Construct checked SimpleBuffer
        checkRequestFlags(flags);       // Check request is compatible with type
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on the entirety of a byte array, with
     * navigation variables initialised, for sub-class use. The buffer ( {@link #storage},
     * {@link #index0}), and the navigation ({@link #shape} array) will be initialised from the
     * array argument.
     *
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws NullPointerException if <code>storage</code> is null
     */
    // XXX: "for sub-class use" = should be protected?
    public SimpleBuffer(byte[] storage) throws NullPointerException {
        this();
        this.storage = storage;         // Exported data (index0=0 from initialisation)
        this.shape[0] = storage.length; // Number of units in whole array
    }

    /**
     * Provide an instance of <code>SimpleBuffer</code>, on the entirety of a byte array, meeting
     * the consumer's expectations as expressed in the <code>flags</code> argument, which is checked
     * against the capabilities of the buffer type.
     *
     * @param flags consumer requirements
     * @param storage the array of bytes storing the implementation of the exporting object
     * @throws NullPointerException if <code>storage</code> is null
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public SimpleBuffer(int flags, byte[] storage) throws PyException, NullPointerException {
        this(storage);                  // Construct SimpleBuffer on whole array
        checkRequestFlags(flags);       // Check request is compatible with type
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
    public int getLen() {
        // Simplify for one-dimensional contiguous bytes
        return shape[0];
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
        return storage[index0 + index];
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
        return 0xff & storage[index0 + index];
    }

    @Override
    protected int calcIndex(int index) throws IndexOutOfBoundsException {
        return index0 + index;
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
        System.arraycopy(storage, index0 + srcIndex, dest, destPos, length);
    }

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
     * <code>SimpleBuffer</code> provides an implementation for slicing contiguous bytes in one
     * dimension. In that case, <i>x(i) = u(r+i)</i> for <i>i = 0..L-1</i> where u is the underlying
     * buffer, and <i>r</i> and <i>L</i> are the start and length with which <i>x</i> was created
     * from <i>u</i>. Thus <i>y(k) = u(r+s+km)</i>, that is, the composite offset is <i>r+s</i> and
     * the stride is <i>m</i>.
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
            return new Strided1DBuffer.SlicedView(getRoot(), flags, storage, compIndex0, length,
                    stride);
        }
    }

    @Override
    public ByteBuffer getNIOByteBuffer() {
        // Simplify for one-dimensional contiguous bytes
        ByteBuffer b = ByteBuffer.wrap(storage, index0, shape[0]);
        return isReadonly() ? b.asReadOnlyBuffer() : b;
    }

    @Override
    public Pointer getPointer(int index) throws IndexOutOfBoundsException {
        return new Pointer(storage, index0 + index);
    }

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
