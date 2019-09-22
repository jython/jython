package org.python.core.buffer;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import org.python.core.PyBUF;
import org.python.core.PyException;

/**
 * Base implementation of the Buffer API for when the storage implementation is
 * <code>java.nio.ByteBuffer</code>. The description of {@link BaseBuffer} mostly applies. Methods
 * provided or overridden here are appropriate to 1-dimensional arrays, of any item size, backed by
 * a <code>ByteBuffer</code>.
 */
public abstract class BaseNIOBuffer extends Base1DBuffer {

    /**
     * A {@link java.nio.ByteBuffer} (possibly a direct buffer) wrapping the storage that the
     * exporter is sharing with the consumer. The data to be exposed may be only a subset of the
     * bytes in the buffer, defined by the navigation information <code>index0</code>,
     * <code>shape</code>, <code>strides</code>, etc., usually defined in the constructor.
     * <p>
     * Implementations must not adjust the position and limit of <code>storage</code> after
     * construction. It will generally be a duplicate of (not a reference to) a ByteBuffer held by
     * the client code. The capacity and backing store are fixed in construction, and the position
     * will always be {@link #index0}. The limit is always higher than any valid data, and in the
     * case of a contiguous buffer (with positive stride), is exactly just beyond the last item, so
     * that a series of ByteBuffer.get operations will yield the data.
     */
    protected ByteBuffer storage;

    /**
     * Construct an instance of <code>BaseNIOBuffer</code> in support of a sub-class, specifying the
     * 'feature flags', or at least a starting set to be adjusted later. Also specify the navigation
     * ( {@link #index0}, number of elements, and stride. These 'feature flags' are the features of
     * the buffer exported, not the flags that form the consumer's request. The buffer will be
     * read-only unless {@link PyBUF#WRITABLE} is set. {@link PyBUF#FORMAT} and
     * {@link PyBUF#AS_ARRAY} are implicitly added to the feature flags.
     * <p>
     * To complete initialisation, the sub-class normally must call {@link #checkRequestFlags(int)}
     * passing the consumer's request flags.
     *
     * @param storage the <code>ByteBuffer</code> wrapping the exported object state. NOTE: this
     *            <code>PyBuffer</code> keeps a reference and may manipulate the position, mark and
     *            limit hereafter. Use {@link ByteBuffer#duplicate()} to give it an isolated copy.
     * @param featureFlags bit pattern that specifies the features allowed
     * @param index0 index into storage of <code>item[0]</code>
     * @param size number of elements in the view
     * @param stride byte-index step between successive elements
     */
    protected BaseNIOBuffer(ByteBuffer storage, int featureFlags, int index0, int size, int stride) {
        super(featureFlags & ~(WRITABLE | AS_ARRAY), index0, size, stride);
        this.storage = storage;

        // Deduce other feature flags from the client's ByteBuffer
        if (!storage.isReadOnly()) {
            addFeatureFlags(WRITABLE);
        }
        if (storage.hasArray()) {
            addFeatureFlags(AS_ARRAY);
        }
    }

    @Override
    protected byte byteAtImpl(int byteIndex) throws IndexOutOfBoundsException {
        return storage.get(byteIndex);
    }

    @Override
    protected void storeAtImpl(byte value, int byteIndex) throws PyException {
        try {
            storage.put(byteIndex, value);
        } catch (ReadOnlyBufferException rbe) {
            throw notWritable();
        }
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
     * The default implementation in <code>BaseNIOBuffer</code> deals with the general
     * one-dimensional case of arbitrary item size and stride.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int count)
            throws IndexOutOfBoundsException {
        // Wrap the destination, taking care to reflect the necessary range we shall write.
        ByteBuffer destBuf = ByteBuffer.wrap(dest, destPos, count * getItemsize());
        copyTo(srcIndex, destBuf, count);
    }

    /**
     * Copy all items in this buffer into a <code>ByteBuffer</code>, starting at its current
     * position.
     *
     * @param dest destination buffer
     * @throws BufferOverflowException
     * @throws ReadOnlyBufferException
     */
    // XXX Should this become part of the PyBUffer interface?
    public void copyTo(ByteBuffer dest) throws BufferOverflowException, ReadOnlyBufferException {
        // Note shape[0] is the number of items in the buffer
        copyTo(0, dest, shape[0]);
    }

    /**
     * Copy a specified number of items from a particular location in this buffer into a
     * <code>ByteBuffer</code>, starting at its current position. .
     *
     * @param srcIndex index of the first item to copy
     * @param dest destination buffer
     * @param count number of items to copy
     * @throws BufferOverflowException
     * @throws ReadOnlyBufferException
     * @throws IndexOutOfBoundsException
     */
    // XXX Should this become part of the PyBuffer interface?
    protected void copyTo(int srcIndex, ByteBuffer dest, int count) throws BufferOverflowException,
            ReadOnlyBufferException, IndexOutOfBoundsException {

        if (count > 0) {

            ByteBuffer src = getNIOByteBuffer();
            int pos = byteIndex(srcIndex);

            // Pick up attributes necessary to choose an efficient copy strategy
            int itemsize = getItemsize();
            int stride = getStrides()[0];

            // Strategy depends on whether items are laid end-to-end contiguously or there are gaps
            if (stride == itemsize) {
                // stride == itemsize: straight copy of contiguous bytes
                src.limit(pos + count * itemsize).position(pos);
                dest.put(src);

            } else if (itemsize == 1) {
                // Non-contiguous copy: single byte items
                for (int i = 0; i < count; i++) {
                    src.position(pos);
                    dest.put(src.get());
                    pos += stride;
                }

            } else {
                // Non-contiguous copy: each time, copy itemsize bytes then skip
                for (int i = 0; i < count; i++) {
                    src.limit(pos + itemsize).position(pos);
                    dest.put(src);
                    pos += stride;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseNIOBuffer</code> deals with the general
     * one-dimensional case of arbitrary item size and stride.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int count)
            throws IndexOutOfBoundsException, PyException {
        // Wrap the source, taking care to reflect the range we shall read.
        ByteBuffer srcBuf = ByteBuffer.wrap(src, srcPos, count * getItemsize());
        copyFrom(srcBuf, destIndex, count);
    }

    /**
     * Copy a specified number of items from a <code>ByteBuffer</code> into this buffer at a
     * particular location.
     *
     * @param src source <code>ByteBuffer</code>
     * @param destIndex starting item-index in the destination (i.e. <code>this</code>)
     * @param count number of items to copy in
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     * @throws PyException {@code TypeError} if read-only buffer
     */
    // XXX Should this become part of the PyBUffer interface?
    protected void copyFrom(ByteBuffer src, int destIndex, int count)
            throws IndexOutOfBoundsException, PyException {

        checkWritable();

        if (count > 0) {

            ByteBuffer dest = getNIOByteBuffer();
            int pos = byteIndex(destIndex);

            // Pick up attributes necessary to choose an efficient copy strategy
            int itemsize = getItemsize();
            int stride = getStrides()[0];
            int skip = stride - itemsize;
            int size = getSize();

            // Check indexes in destination (this) using the "all non-negative" trick
            if ((destIndex | count | size - (destIndex + count)) < 0) {
                throw new IndexOutOfBoundsException();
            }

            // Strategy depends on whether items are laid end-to-end or there are gaps
            if (skip == 0) {
                // Straight copy of contiguous bytes
                dest.position(pos);
                dest.put(src);

            } else if (itemsize == 1) {
                // Non-contiguous copy: single byte items
                for (int i = 0; i < count; i++) {
                    dest.position(pos);
                    dest.put(src.get());
                    // Next byte written will be here
                    pos += stride;
                }

            } else {
                // Non-contiguous copy: each time, copy itemsize bytes at a time
                for (int i = 0; i < count; i++) {
                    dest.position(pos);
                    // Delineate the next itemsize bytes in the src
                    src.limit(src.position() + itemsize);
                    dest.put(src);
                    // Next byte written will be here
                    pos += stride;
                }
            }
        }
    }

    @Override
    protected ByteBuffer getNIOByteBufferImpl() {
        return storage.duplicate();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getBuf() {
        checkHasArray();
        return new Pointer(storage.array(), index0);
    }
}
