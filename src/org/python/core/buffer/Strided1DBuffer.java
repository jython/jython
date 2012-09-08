package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Read-only buffer API over a one-dimensional array of one-byte items, that are evenly-spaced in a
 * storage array. The buffer has a <code>buf</code> property in the usual way, designating a slice
 * (or all) of a byte array, but also a <code>stride</code> property (equal to
 * <code>getStrides()[0]</code>).
 * <p>
 * Let this underlying buffer be the byte array <i>u(i)</i> for <i>i=a..a+N</i>, let <i>x</i> be the
 * <code>Strided1DBuffer</code>, and let the stride be <i>p</i>. The storage works as follows.
 * Designate by <i>x(j)</i>, for <i>j=0..L-1</i>, the byte at index <i>j</i>, that is, the byte
 * retrieved by <code>x.byteAt(j)</code>. Then,
 * <ul>
 * <li>when <i>p&gt;0</i>, we store <i>x(j)</i> at <i>u(a+pj)</i>, that is, <i>x(0)</i> is at
 * <i>u(a)</i> and the byte array slice size should be <i>N = (L-1)p+1</i>.</li>
 * <li>when <i>p&lt;0</i>, we store <i>x(j)</i> at <i>u((a+N-1)+pj)</i>, that is, <i>x(0)</i> is at
 * <i>u(a+N-1)</i>, and the byte array slice size should be <i>N = (L-1)(-p)+1</i>.</li>
 * <li><i>p=0</i> is not a useful stride.</li>
 * </ul>
 * <p>
 * The class may be used by exporters to create a strided slice (e.g. to export the diagonal of a
 * matrix) and in particular by other buffers to create strided slices of themselves, such as to
 * create the memoryview that is returned as an extended slice of a memoryview.
 */
public class Strided1DBuffer extends BaseBuffer {

    /**
     * Step size in the underlying buffer essential to correct translation of an index (or indices)
     * into an index into the storage. The value is returned by {@link #getStrides()} is an array
     * with this as the only element.
     */
    protected int stride;

    /**
     * Absolute index in <code>buf.storage</code> of <code>item[0]</code>. For a positive
     * <code>stride</code> this is equal to <code>buf.offset</code>, and for a negative
     * <code>stride</code> it is <code>buf.offset+buf.size-1</code>. It has to be used in most of
     * the places that buf.offset would appear in the index calculations of simpler buffers (that
     * have unit stride).
     */
    protected int index0;

    /**
     * Provide an instance of <code>Strided1DBuffer</code> with navigation variables partly
     * initialised, for sub-class use. To complete initialisation, the sub-class normally must
     * assign: {@link #buf}, {@link #shape}[0], and {@link #stride}, and call
     * {@link #checkRequestFlags(int)} passing the consumer's request flags.
     *
     * <pre>
     * this.buf = buf;              // Wraps exported data
     * setStride(stride);           // Stride, shape[0] and index0 all set consistently
     * checkRequestFlags(flags);    // Check request is compatible with type
     * </pre>
     *
     * The pre-defined {@link #strides} field remains <code>null</code> until {@link #getStrides} is
     * called.
     */
    protected Strided1DBuffer() {
        super(STRIDES);
        // Initialise navigation
        shape = new int[1];
        // strides is created on demand;
        // suboffsets is always null for this type.
    }

    /**
     * Provide an instance of <code>Strided1DBuffer</code> on a particular array of bytes specifying
     * a starting index, the number of items in the result, and a byte-indexing stride. The result
     * of <code>byteAt(i)</code> will be equal to <code>storage[index0+stride*i]</code> (whatever
     * the sign of <code>stride>0</code>), valid for <code>0&lt;=i&lt;length</code>.
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
    public Strided1DBuffer(int flags, byte[] storage, int index0, int length, int stride)
            throws PyException {

        // Arguments programme the object directly
        this();
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
        return true;
    }

    @Override
    public byte byteAt(int index) throws IndexOutOfBoundsException {
        return buf.storage[index0 + index * stride];
    }

    @Override
    protected int calcIndex(int index) throws IndexOutOfBoundsException {
        return index0 + index * stride;
    }

    @Override
    protected int calcIndex(int... indices) throws IndexOutOfBoundsException {
        // BaseBuffer implementation can be simplified since if indices.length!=1 we error.
        checkDimension(indices.length); // throws if != 1
        return calcIndex(indices[0]);
    }

    /**
     * {@inheritDoc} <code>Strided1DBuffer</code> provides a version optimised for strided bytes in
     * one dimension.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
            throws IndexOutOfBoundsException {
        // Data is here in the buffers
        int s = index0 + srcIndex * stride;
        int d = destPos;

        // Strategy depends on whether items are laid end-to-end contiguously or there are gaps
        if (stride == 1) {
            // stride == itemsize: straight copy of contiguous bytes
            System.arraycopy(buf.storage, s, dest, d, length);

        } else {
            // Discontiguous copy: single byte items
            int limit = s + length * stride;
            for (; s != limit; s += stride) {
                dest[d++] = buf.storage[s];
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>Strided1DBuffer</code> provides an implementation for slicing already-strided bytes in
     * one dimension. In that case, <i>x(i) = u(r+ip)</i> for <i>i = 0..L-1</i> where u is the
     * underlying buffer, and <i>r</i>, <i>p</i> and <i>L</i> are the start, stride and length with
     * which <i>x</i> was created from <i>u</i>. Thus <i>y(k) = u(r+sp+kmp)</i>, that is, the
     * composite offset is <i>r+sp</i> and the composite stride is <i>mp</i>.
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

    @Override
    public BufferPointer getPointer(int index) {
        return new BufferPointer(buf.storage, index0 + index, 1);
    }

    @Override
    public BufferPointer getPointer(int... indices) {
        // BaseBuffer implementation can be simplified since if indices.length!=1 we error.
        checkDimension(indices.length);
        return getPointer(indices[0]);
    }

    @Override
    public int[] getStrides() {
        if (strides == null) {
            strides = new int[1];
            strides[0] = stride;
        }
        return strides;
    }

    /**
     * A <code>Strided1DBuffer.SlicedView</code> represents a discontiguous subsequence of a simple
     * buffer.
     */
    static class SlicedView extends Strided1DBuffer {

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
            // Get a lease on the root PyBuffer (read-only)
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
