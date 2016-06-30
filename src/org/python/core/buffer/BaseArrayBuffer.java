package org.python.core.buffer;

import java.nio.ByteBuffer;

import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Base implementation of the Buffer API for when the storage implementation is <code>byte[]</code>.
 * The description of {@link BaseBuffer} mostly applies. Methods provided or overridden here are
 * appropriate to 1-dimensional arrays, of any item size, backed by <code>byte[]</code>.
 */
public abstract class BaseArrayBuffer extends Base1DBuffer {

    /**
     * Reference to the underlying <code>byte[]</code> storage that the exporter is sharing with the
     * consumer. The data need not occupy the whole array: in the constructor of a particular type
     * of buffer, the exporter usually indicates an offset to the first significant byte and length
     * (contiguous cases) or the index in <code>storage</code> that should be treated as the item
     * with index zero (retrieved say by <code>buf.byteAt(0)</code>).
     */
    protected byte[] storage;

    /**
     * Construct an instance of <code>BaseArrayBuffer</code> in support of a sub-class, specifying
     * the 'feature flags', or at least a starting set to be adjusted later. Also specify the
     * navigation ( {@link #index0}, number of elements, and stride. These 'feature flags' are the
     * features of the buffer exported, not the flags that form the consumer's request. The buffer
     * will be read-only unless {@link PyBUF#WRITABLE} is set. {@link PyBUF#FORMAT} and
     * {@link PyBUF#AS_ARRAY} are implicitly added to the feature flags.
     * <p>
     * To complete initialisation, the sub-class normally must call {@link #checkRequestFlags(int)}
     * passing the consumer's request flags.
     *
     * @param storage the array of bytes storing the implementation of the exporting object
     * @param featureFlags bit pattern that specifies the features allowed
     * @param index0 index into storage of <code>item[0]</code>
     * @param size number of elements in the view
     * @param stride byte-index distance from one element to the next
     */
    protected BaseArrayBuffer(byte[] storage, int featureFlags, int index0, int size, int stride) {
        super(featureFlags | AS_ARRAY, index0, size, stride);
        this.storage = storage;
    }

    @Override
    protected byte byteAtImpl(int byteIndex) throws IndexOutOfBoundsException {
        return storage[byteIndex];
    }

    @Override
    protected void storeAtImpl(byte value, int byteIndex) throws IndexOutOfBoundsException,
            PyException {
        checkWritable();
        storage[byteIndex] = value;
    }

    @Override
    public int byteIndex(int... indices) throws IndexOutOfBoundsException {
        // BaseBuffer implementation can be simplified since if indices.length!=1 we error.
        checkDimension(indices.length); // throws if != 1
        return byteIndex(indices[0]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in <code>BaseArrayBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int count)
            throws IndexOutOfBoundsException {

        if (count > 0) {
            // Pick up attributes necessary to choose an efficient copy strategy
            int itemsize = getItemsize();
            int stride = getStrides()[0];
            int skip = stride - itemsize;
            int s = byteIndex(srcIndex);

            // Strategy depends on whether items are laid end-to-end contiguously or there are gaps
            if (skip == 0) {
                // stride == itemsize: straight copy of contiguous bytes
                System.arraycopy(storage, s, dest, destPos, count * itemsize);
            } else {
                int limit = s + count * stride, d = destPos;
                if (itemsize == 1) {
                    // Non-contiguous copy: single byte items
                    for (; s != limit; s += stride) {
                        dest[d++] = storage[s];
                    }
                } else {
                    // Non-contiguous copy: each time, copy itemsize bytes then skip
                    for (; s != limit; s += skip) {
                        int t = s + itemsize;
                        while (s < t) {
                            dest[d++] = storage[s++];
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseArrayBuffer</code> deals with the general
     * one-dimensional case of arbitrary item size and stride.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int count)
            throws IndexOutOfBoundsException, PyException {
        copyFrom(src, srcPos, 1, destIndex, count);
    }

    /**
     * Generalisation of {@link PyBuffer#copyFrom(byte[], int, int, int)} to allow a stride within
     * the source array.
     *
     * @param src source byte array
     * @param srcPos byte-index location in source of first byte to copy
     * @param srcStride byte-index increment from one item to the next
     * @param destIndex starting item-index in the destination (i.e. <code>this</code>)
     * @param count number of items to copy in
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     * @throws PyException (TypeError) if read-only buffer
     */
    protected void copyFrom(byte[] src, int srcPos, int srcStride, int destIndex, int count)
            throws IndexOutOfBoundsException, PyException {

        checkWritable();

        if (count > 0) {
            // Pick up attributes necessary to choose an efficient copy strategy
            int itemsize = getItemsize();
            int stride = getStrides()[0];
            int skip = stride - itemsize;
            int d = byteIndex(destIndex);

            int srcSkip = srcStride - itemsize;

            // Strategy depends on whether items are laid end-to-end or there are gaps
            if (skip == 0 && srcSkip == 0) {
                // Straight copy of contiguous bytes
                System.arraycopy(src, srcPos, storage, d, count * itemsize);
            } else {
                int limit = d + count * stride, s = srcPos;
                if (itemsize == 1) {
                    // Non-contiguous copy: single byte items
                    for (; d != limit; d += stride) {
                        storage[d] = src[s];
                        s += srcStride;
                    }
                } else {
                    // Non-contiguous copy: itemsize bytes then skip to next item
                    for (; d != limit; d += skip) {
                        int t = d + itemsize;
                        while (d < t) {
                            storage[d++] = src[s++];
                        }
                        s += srcSkip;
                    }
                }
            }
        }
    }

    @Override
    public void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException {
        if (src instanceof BaseArrayBuffer && !this.overlaps((BaseArrayBuffer)src)) {
            // We can do this efficiently, copying between arrays.
            copyFromArrayBuffer((BaseArrayBuffer)src);
        } else {
            super.copyFrom(src);
        }
    }

    private boolean overlaps(BaseArrayBuffer src) {
        if (src.storage != this.storage) {
            return false;
        } else {
            int low = calcLeastIndex(), high = calcGreatestIndex();
            int srcLow = src.calcLeastIndex(), srcHigh = src.calcGreatestIndex();
            return (srcHigh >= low && high >= srcLow);
        }
    }

    private void copyFromArrayBuffer(BaseArrayBuffer src) throws IndexOutOfBoundsException,
            PyException {

        src.checkDimension(1);

        int itemsize = getItemsize();
        int count = getSize();

        // Block operation if different item or overall size
        if (src.getItemsize() != itemsize || src.getSize() != count) {
            throw differentStructure();
        }

        // We depend on the striding copyFrom() acting directly on the source storage
        copyFrom(src.storage, src.index0, src.strides[0], 0, count);
    }

    @Override
    protected ByteBuffer getNIOByteBufferImpl() {
        // The buffer spans the whole storage, which may include data not in the view
        ByteBuffer b = ByteBuffer.wrap(storage);
        // Return as read-only if it is.
        return isReadonly() ? b.asReadOnlyBuffer() : b;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <code>BaseArrayBuffer</code> provides a reference to the storage array even when the buffer
     * is intended not to be writable. There can be no enforcement of read-only character once a
     * reference to the byte array has been handed out.
     */
    @SuppressWarnings("deprecation")
    @Override
    public Pointer getBuf() {
        return new Pointer(storage, index0);
    }
}
