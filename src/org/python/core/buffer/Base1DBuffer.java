package org.python.core.buffer;

import org.python.core.PyBUF;

/**
 * Base implementation of the Buffer API appropriate to 1-dimensional arrays, of any item size,
 * independent of the storage implementation. The description of {@link BaseBuffer} mostly applies.
 */
public abstract class Base1DBuffer extends BaseBuffer {

    /** The strides array for a contiguous 1D byte buffer. */
    protected static final int[] ONE = {1};

    /** The shape array for a zero length 1D buffer. */
    protected static final int[] ZERO = {0};

    /**
     * Construct an instance of <code>Base1DBuffer</code> in support of a sub-class, specifying the
     * 'feature flags', or at least a starting set to be adjusted later. Also specify the navigation
     * ( {@link #index0}, number of elements, and {@link #strides} array. These 'feature flags' are
     * the features of the buffer exported, not the flags that form the consumer's request. The
     * buffer will be read-only unless {@link PyBUF#WRITABLE} is set. {@link PyBUF#FORMAT} is
     * implicitly added to the feature flags.
     * <p>
     * To complete initialisation, the sub-class normally must create its own wrapped byte-storage,
     * and call {@link #checkRequestFlags(int)} passing the consumer's request flags.
     *
     * @param featureFlags bit pattern that specifies the features allowed
     * @param index0 index into storage of <code>item[0]</code>
     * @param size number of elements in the view
     * @param strides an array of length 1 providing index stride between successive elements
     */
    protected Base1DBuffer(int featureFlags, int index0, int size, int[] strides) {
        super(featureFlags, index0, size == 0 ? ZERO : new int[] {size}, strides);
    }

    /**
     * Construct an instance of <code>Base1DBuffer</code> in support of a sub-class, specifying the
     * 'feature flags', or at least a starting set to be adjusted later. Also specify the navigation
     * ( {@link #index0}, number of elements, and byte-index distance from one to the next. These
     * 'feature flags' are the features of the buffer exported, not the flags that form the
     * consumer's request. The buffer will be read-only unless {@link PyBUF#WRITABLE} is set.
     * {@link PyBUF#FORMAT} is implicitly added to the feature flags.
     * <p>
     * To complete initialisation, the sub-class normally must create its own wrapped byte-storage,
     * and call {@link #checkRequestFlags(int)} passing the consumer's request flags.
     *
     * @param featureFlags bit pattern that specifies the features allowed
     * @param index0 index into storage of <code>item[0]</code>
     * @param size number of elements in the view
     * @param stride byte-index distance from one element to the next
     */
    protected Base1DBuffer(int featureFlags, int index0, int size, int stride) {
        this(featureFlags, index0, size, stride == 1 ? ONE : new int[] {stride});
    }

    @Override
    protected int getSize() {
        return shape[0];
    }

    @Override
    public int getLen() {
        return shape[0] * getItemsize();
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
            return index0;
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
