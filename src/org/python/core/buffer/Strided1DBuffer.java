package org.python.core.buffer;

import org.python.core.BufferProtocol;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Read-only buffer API over a one-dimensional array of one-byte items, that are evenly-spaced in a
 * storage array. The buffer has <code>storage</code>, <code>index0</code> and <code>count</code>
 * properties in the usual way, designating a slice (or all) of a byte array, but also a
 * <code>stride</code> property (equal to <code>getStrides()[0]</code>).
 * <p>
 * Let the underlying buffer be the byte array <i>u(i)</i> for <i>i=0..N-1</i>, let <i>x</i> be the
 * <code>Strided1DBuffer</code>, and let the stride be <i>p</i>. The storage works as follows.
 * Designate by <i>x(j)</i>, for <i>j=0..L-1</i>, the byte at index <i>j</i>, that is, the byte
 * retrieved by <code>x.byteAt(j)</code>. Thus, we store <i>x(j)</i> at <i>u(a+pj)</i>, that is,
 * <i>x(0) = u(a)</i>. When we construct such a buffer, we have to supply <i>a</i> =
 * <code>index0</code>, <i>L</i> = <code>count</code>, and <i>p</i> = <code>stride</code> as the
 * constructor arguments. The last item in the slice <i>x(L-1)</i> is stored at <i>u(a+p(L-1))</i>.
 * For the simple case of positive stride, constructor argument <code>index0</code> is the low index
 * of the range occupied by the data. When the stride is negative, that is to say <i>p&lt;0</i>, and
 * <i>L&gt;1</i>, this will be to the left of <i>u(a)</i>, and the constructor argument
 * <code>index0</code> is not then the low index of the range occupied by the data. Clearly both
 * these indexes must be in the range 0 to <i>N-1</i> inclusive, a rule enforced by the constructors
 * (unless <i>L=0</i>, when it is assumed no array access will take place).
 * <p>
 * The class may be used by exporters to create a strided slice (e.g. to export the diagonal of a
 * matrix) and in particular by other buffers to create strided slices of themselves, such as to
 * create the <code>memoryview</code> that is returned as an extended slice of a
 * <code>memoryview</code>.
 */
public class Strided1DBuffer extends BaseArrayBuffer {

    /**
     * Step size in the underlying buffer essential to correct translation of an index (or indices)
     * into an index into the storage. The value is returned by {@link #getStrides()} is an array
     * with this as the only element.
     */
    protected int stride;

    /**
     * Provide an instance of <code>Strided1DBuffer</code> with navigation variables initialised,
     * for sub-class use. The buffer ({@link #storage}, {@link #index0}), and the navigation (
     * {@link #shape} array and {@link #stride}) will be initialised from the arguments (which are
     * checked for range).
     * <p>
     * The sub-class constructor should check that the intended access is compatible with this
     * object by calling {@link #checkRequestFlags(int)}. (See the source of
     * {@link Strided1DWritableBuffer#Strided1DWritableBuffer(int, byte[], int, int, int)} for an
     * example of this use.)
     *
     * @param obj exporting object (or <code>null</code>)
     * @param storage raw byte array containing exported data
     * @param index0 index into storage of item[0]
     * @param count number of items in the slice
     * @param stride in between successive elements of the new PyBuffer
     * @throws NullPointerException if <code>storage</code> is null
     * @throws ArrayIndexOutOfBoundsException if <code>index0</code>, <code>count</code> and
     *             <code>stride</code> are inconsistent with <code>storage.length</code>
     */
    protected Strided1DBuffer(BufferProtocol obj, byte[] storage, int index0, int count, int stride)
            throws ArrayIndexOutOfBoundsException, NullPointerException {
        super(storage, STRIDES, index0, count, stride);
        this.obj = obj;
        this.stride = stride;           // Between items

        if (count == 0) {
            // Nothing to check as we'll make no accesses
            addFeatureFlags(CONTIGUITY);

        } else {
            // Need to check lowest and highest index against array
            int lo, hi;

            if (stride == 1) {
                lo = index0;                                // First byte of item[0]
                hi = index0 + count;                        // Last byte of item[L-1] + 1
                addFeatureFlags(CONTIGUITY);

            } else if (stride > 1) {
                lo = index0;                                // First byte of item[0]
                hi = index0 + (count - 1) * stride + 1;     // Last byte of item[L-1] + 1

            } else {
                hi = index0 + 1;                            // Last byte of item[0] + 1
                lo = index0 + (count - 1) * stride;         // First byte of item[L-1]
            }

            // Check indices using "all non-negative" trick
            if ((count | lo | (storage.length - lo) | hi | (storage.length - hi)) < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    /**
     * Provide an instance of <code>Strided1DBuffer</code> on a particular array of bytes specifying
     * a starting index, the number of items in the result, and a byte-indexing stride. The result
     * of <code>byteAt(i)</code> will be equal to <code>storage[index0+stride*i]</code> (whatever
     * the sign of <code>stride</code>), valid for <code>0&lt;=i&lt;count</code>. The constructor
     * checks that all these indices lie within the <code>storage</code> array (unless
     * <code>count=0</code>).
     * <p>
     * The constructed <code>PyBuffer</code> meets the consumer's expectations as expressed in the
     * <code>flags</code> argument, or an exception will be thrown if these are incompatible with
     * the type (e.g. the consumer does not specify that it understands the strides array). Note
     * that the actual range in the <code>storage</code> array, the lowest and highest index, is not
     * explicitly passed, but is implicit in <code>index0</code>, <code>count</code> and
     * <code>stride</code>. The constructor checks that these indices lie within the
     * <code>storage</code> array (unless <code>count=0</code>).
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
    public Strided1DBuffer(int flags, BufferProtocol obj, byte[] storage, int index0, int count, int stride)
            throws ArrayIndexOutOfBoundsException, NullPointerException, PyException {
        this(obj, storage, index0, count, stride);
        checkRequestFlags(flags);   // Check request is compatible with type

    }

    @Override
    public boolean isReadonly() {
        return true;
    }

    @Override
    public final int byteIndex(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= shape[0]) {
            throw new IndexOutOfBoundsException();
        }
        return index0 + index * stride;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>Strided1DBuffer</code> provides an implementation for slicing already-strided bytes in
     * one dimension. In that case, <i>x(i) = u(r+ip)</i> for <i>i = 0..L-1</i> where u is the
     * underlying buffer, and <i>r</i>, <i>p</i> and <i>L</i> are the start, stride and count with
     * which <i>x</i> was created from <i>u</i>. Thus <i>y(k) = u(r+sp+kmp)</i>, that is, the
     * composite <code>index0</code> is <i>r+sp</i> and the composite <code>stride</code> is
     * <i>mp</i>.
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

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int index) {
        return new Pointer(storage, index0 + index * stride);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int... indices) {
        // BaseBuffer implementation can be simplified since if indices.length!=1 we error.
        checkDimension(indices.length);
        return getPointer(indices[0]);
    }

    /**
     * A <code>Strided1DBuffer.SlicedView</code> represents a non-contiguous subsequence of a simple
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
         * @param count number of items in the sliced view
         * @param stride in between successive elements of the new PyBuffer
         * @throws PyException (BufferError) when expectations do not correspond with the type
         */
        public SlicedView(PyBuffer root, int flags, byte[] storage, int index0, int count,
                int stride) throws PyException {
            // Create a new on the buffer passed in (part of the root)
            super(flags, root.getObj(), storage, index0, count, stride);
            // Get a lease on the root PyBuffer (read-only)
            this.root = root.getBuffer(FULL_RO);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }
    }
}
