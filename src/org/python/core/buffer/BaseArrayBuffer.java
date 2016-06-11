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
public abstract class BaseArrayBuffer extends BaseBuffer implements PyBuffer {

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
     * the 'feature flags', or at least a starting set to be adjusted later. These are the features
     * of the buffer exported, not the flags that form the consumer's request. The buffer will be
     * read-only unless {@link PyBUF#WRITABLE} is set in the feature flags. {@link PyBUF#FORMAT} and
     * {@link PyBUF#AS_ARRAY} are implicitly added to the feature flags. The navigation arrays are
     * all null, awaiting action by the sub-class constructor. To complete initialisation, the
     * sub-class normally must assign: the buffer ( {@link #storage}, {@link #index0}), and the
     * navigation arrays ({@link #shape}, {@link #strides}), and call
     * {@link #checkRequestFlags(int)} passing the consumer's request flags.
     *
     * @param featureFlags bit pattern that specifies the actual features allowed/required
     */
    protected BaseArrayBuffer(int featureFlags) {
        super(featureFlags | AS_ARRAY);
    }

    @Override
    protected int getSize() {
        return shape[0];
    }

    @Override
    public int getLen() {
        return shape[0] * getItemsize();
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
    protected int byteIndex(int... indices) throws IndexOutOfBoundsException {
        // BaseBuffer implementation can be simplified since if indices.length!=1 we error.
        checkDimension(indices.length); // throws if != 1
        return byteIndex(indices[0]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Specialised to one-dimensional, possibly strided buffer.
     */
    @Override
    protected int calcGreatestIndex() {
        int stride = strides[0];
        if (stride == 1) {
            return index0 + shape[0] - 1;
        } else if (stride > 0) {
            return index0 + (shape[0] - 1) * stride;
        } else {
            return index0 - 1;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Specialised to one-dimensional, possibly strided buffer.
     */
    @Override
    protected int calcLeastIndex() {
        int stride = strides[0];
        if (stride < 0) {
            return index0 + (shape[0] - 1) * stride;
        } else {
            return index0;
        }
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

        checkWritable();

        if (count > 0) {
            // Pick up attributes necessary to choose an efficient copy strategy
            int itemsize = getItemsize();
            int stride = getStrides()[0];
            int skip = stride - itemsize;
            int d = byteIndex(destIndex);

            // Strategy depends on whether items are laid end-to-end or there are gaps
            if (skip == 0) {
                // Straight copy of contiguous bytes
                System.arraycopy(src, srcPos, storage, d, count * itemsize);
            } else {
                // Non-contiguous copy: single byte items
                int limit = d + count * stride, s = srcPos;
                if (itemsize == 1) {
                    for (; d != limit; d += stride) {
                        storage[d] = src[s++];
                    }
                } else {
                    // Non-contiguous copy: itemsize bytes then skip to next item
                    for (; d != limit; d += skip) {
                        int t = d + itemsize;
                        while (d < t) {
                            storage[d++] = src[s++];
                        }
                    }
                }
            }
        }
    }

    @Override
    public void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException {
        if (src instanceof BaseArrayBuffer) {
            copyFromArrayBuffer((BaseArrayBuffer)src);
        } else {
            super.copyFrom(src);
        }
    }

    private void copyFromArrayBuffer(BaseArrayBuffer src) throws IndexOutOfBoundsException,
            PyException {

        checkWritable();
        src.checkDimension(1);

        int itemsize = getItemsize();
        int count = getSize();

        // Block operation if different item or overall size (permit reshape)
        if (src.getItemsize() != itemsize || src.getSize() != count) {
            throw differentStructure();
        }

        for (int i = 0; i < count; i++) {
            int s = src.byteIndex(i), d = byteIndex(i);
            for (int j = 0; j < itemsize; j++) {
                storage[d++] = src.byteAtImpl(s++);
            }
        }
    }

    /**
     * Copy blocks of bytes, equally spaced in the source array, to locations equally spaced in the
     * destination array, which may be the same array. The byte at
     * <code>src[srcPos+k*srcStride+j]</code> will be copied to
     * <code>dst[dstPos+k*dstStride+j]</code> for <code>0&le;k&lt;count</code> and
     * <code>0&le;j&lt;size</code>. When the source and destination are the same array, the method
     * deals correctly with the risk that a byte gets written under the alias <code>dst[x]</code>
     * before it should have been copied referenced as <code>src[y]</code>.
     *
     * @param size of the blocks of bytes
     * @param src the source array
     * @param srcPos the position of the first block in the source
     * @param srcStride the interval between the start of each block in the source
     * @param dst the destination array
     * @param dstPos the position of the first block in the destination
     * @param dstStride the interval between the start of each block in the destination
     * @param count the number of blocks to copy
     */
    private static void slicedArrayCopy(int size, byte[] src, int srcPos, int srcStride,
            byte[] dst, int dstPos, int dstStride, int count) {}

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

    /**
     * {@inheritDoc}
     * <p>
     * Specialised in <code>BaseArrayBuffer</code> to one dimension.
     */
    @Override
    public boolean isContiguous(char order) {
        if ("CFA".indexOf(order) < 0) {
            return false;
        } else {
            if (getShape()[0] < 2) {
                return true;
            } else {
                return getStrides()[0] == getItemsize();
            }
        }
    }
}
