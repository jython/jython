package org.python.core.buffer;

import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Buffer API over a zero length, one-dimensional array of one-byte items. The buffer is nominally
 * writable, but since there is nowhere to write to, any attempt to write or read throws an
 * <code>IndexOutOfBoundsException</code>. This class exists mostly to represent zero-length arrays,
 * and particularly, zero-length slices for which implementations of
 * {@link PyBuffer#getBufferSlice(int, int, int, int)} in any case need special logic. Bulk
 * operations like {@link #copyTo(byte[], int)}) and {@link #toString()} efficiently do nothing,
 * instead of calling complicated logic that finally does nothing.
 */
public class ZeroByteBuffer extends BaseBuffer {

    /** Shared instance of a zero-length storage. */
    private static final byte[] EMPTY = new byte[0];

    /** Array containing a single zero for the length */
    protected static final int[] SHAPE = {0};

    /**
     * Construct an instance of a zero-length buffer, choosing whether it should report itself to be
     * read-only through {@link #isReadonly()}. This is moot, as any attempt to write to it produces
     * an {@link IndexOutOfBoundsException}, but it is less surprising for client code that may ask,
     * if the readability follows that of the object from which the buffer is derived.
     *
     * @param flags consumer requirements
     * @param readonly set true if readonly
     * @throws PyException (BufferError) when expectations do not correspond with the type
     */
    public ZeroByteBuffer(int flags, boolean readonly) throws PyException {
        super(CONTIGUITY | SIMPLE | (readonly ? 0 : WRITABLE));
        this.storage = EMPTY;                       // Empty array
        this.shape = SHAPE;                         // {0}
        this.strides = SimpleBuffer.SIMPLE_STRIDES; // {1}
        checkRequestFlags(flags);
    }

    @Override
    public int getLen() {
        return 0;
    }

    /**
     * In a ZeroByteBuffer, the index is always out of bounds.
     */
    @Override
    protected int calcIndex(int index) throws IndexOutOfBoundsException {
        // This causes all access to the bytes in to throw (since BaseBuffer calls it).
        throw new IndexOutOfBoundsException();
    }

    /**
     * In a ZeroByteBuffer, if the dimensions are right, the index is out of bounds anyway.
     */
    @Override
    protected int calcIndex(int... indices) throws IndexOutOfBoundsException {
        // Bootless dimension check takes precedence (for consistency with other buffers)
        checkDimension(indices);
        // This causes all access to the bytes to throw (since BaseBuffer calls it).
        throw new IndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * In a ZeroByteBuffer, there is simply nothing to copy.
     */
    @Override
    public void copyTo(byte[] dest, int destPos) throws IndexOutOfBoundsException {
        // Nothing to copy
    }

    /**
     * {@inheritDoc}
     * <p>
     * In a ZeroByteBuffer, there is simply nothing to copy.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
            throws IndexOutOfBoundsException, PyException {
        // Nothing to copy
    }

    /**
     * In a ZeroByteBuffer, there is no room for anything, so this throws unless the source length
     * is zero.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int length)
            throws IndexOutOfBoundsException, PyException {
        if (length > 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * In a ZeroByteBuffer, there is no room for anything, so this throws unless the source length
     * is zero.
     */
    @Override
    public void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException {
        if (src.getLen() > 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Only a zero-length slice at zero is valid (in which case, the present buffer will do nicely
     * as a result, with the export count incremented.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length) {
        if (start == 0 && length <= 0) {
            return this.getBuffer(flags);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Only a zero-length slice at zero is valid (in which case, the present buffer will do nicely
     * as a result, with the export count incremented.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length, int stride) {
        // It can't matter what the stride is since length is zero, or there's an error.
        return getBufferSlice(flags, start, length);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in <code>ZeroByteBuffer</code> efficiently returns an empty buffer.
     */
    @Override
    public Pointer getBuf() {
        // Has to be new because the client is allowed to manipulate the contents.
        return new Pointer(EMPTY, 0);
    }

    /**
     * For a ZeroByteBuffer, it's the empty string.
     */
    @Override
    public String toString() {
        return "";
    }

    /**
     * A <code>ZeroByteBuffer.View</code> represents a contiguous subsequence of another
     * <code>PyBuffer</code>. We don't need it to make slices of the ZeroByteBuffer itself, but it
     * is useful for making zero-length slices of anything else. Lock-release semantics must still
     * be observed. In Python, a zero-length slice obtained from the memoryview of a bytearray still
     * counts as an export from the bytearray.
     */
    static class View extends ZeroByteBuffer {

        /** The buffer on which this is a slice view */
        PyBuffer root;

        /**
         * Construct a slice of a ZeroByteBuffer, which it goes without saying is of zero length at
         * position zero.
         *
         * @param root buffer which will be acquired and must be released ultimately
         * @param flags the request flags of the consumer that requested the slice
         */
        public View(PyBuffer root, int flags) {
            // Create a new ZeroByteBuffer on who-cares-what byte array
            super(flags, root.isReadonly());
            // But we still have to get a lease on the root PyBuffer
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
