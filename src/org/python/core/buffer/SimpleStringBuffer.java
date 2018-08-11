package org.python.core.buffer;

import java.nio.ByteBuffer;

import org.python.core.BufferProtocol;
import org.python.core.PyBuffer;
import org.python.core.util.StringUtil;

/**
 * Buffer API that appears to be a one-dimensional array of one-byte items providing read-only API,
 * but which is actually backed by a Java String. Some of the buffer API absolutely needs access to
 * the data as a byte array (those parts that involve a {@link java.nio.ByteBuffer} or
 * {@link org.python.core.PyBuffer.Pointer} result), and therefore this class must create a byte array from the
 * String for them. However, it defers creation of a byte array until that part of the API is
 * actually used. Where possible, this class overrides those methods in SimpleBuffer that would
 * otherwise access the byte array attribute to use the String instead.
 */
public class SimpleStringBuffer extends SimpleBuffer {

    /**
     * The string backing this PyBuffer. A substitute for {@link #buf} until we can no longer avoid
     * creating it.
     */
    private String bufString;

    /**
     * Provide an instance of SimpleStringBuffer meeting the consumer's expectations as expressed in
     * the flags argument.
     *
     * @param flags consumer requirements
     * @param obj exporting object (or <code>null</code>)
     * @param bufString storing the implementation of the object
     */
    public SimpleStringBuffer(int flags, BufferProtocol obj, String bufString) {
        /*
         * Leaving storage=null is ok because we carefully override every method that uses it,
         * deferring creation of the storage byte array until we absolutely must have one.
         */
        super(obj, null, 0, bufString.length());
        // Save the backing string
        this.bufString = bufString;
        // Check request is compatible with type
        checkRequestFlags(flags);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses {@link String#length()} rather than create an actual byte buffer.
     */
    @Override
    public int getLen() {
        // Avoid creating buf by using String.length
        return bufString.length();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses {@link String#charAt(int)} rather than create an actual byte buffer.
     */
    @Override
    public final byte byteAtImpl(int index) {
        return (byte)bufString.charAt(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * In <code>SimpleStringBuffer</code> we can simply return the argument.
     */
    @Override
    public final int byteIndex(int index) {
        // We do not check the index because String will do it for us.
        return index;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses {@link String#charAt(int)} rather than create an actual byte buffer.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int count)
            throws IndexOutOfBoundsException {
        // Avoid creating buf by using String.charAt
        int endIndex = srcIndex + count, p = destPos;
        for (int i = srcIndex; i < endIndex; i++) {
            dest[p++] = (byte)bufString.charAt(i);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>SimpleStringBuffer</code> implementation avoids creation of a byte buffer.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int count) {
        if (count > 0) {
            // The new string content is just a sub-string.
            return new SimpleStringView(getRoot(), flags, bufString.substring(start, start + count));
        } else {
            // Special case for count==0 where start out of bounds sometimes raises exception.
            return new ZeroByteBuffer.View(getRoot(), flags);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>SimpleStringBuffer</code> implementation creates an actual byte buffer.
     */
    @Override
    public PyBuffer getBufferSlice(int flags, int start, int count, int stride) {
        if (stride == 1) {
            // Unstrided slice of a SimpleStringBuffer is itself a SimpleStringBuffer.
            return getBufferSlice(flags, start, count);
        } else {
            // Force creation of the actual byte array from the String.
            ensureHaveBytes();
            // Now we are effectively a SimpleBuffer, return the strided view.
            return super.getBufferSlice(flags, start, count, stride);
        }
    }

    @Override
    protected ByteBuffer getNIOByteBufferImpl() {
        // Force creation of the actual byte array from the String.
        ensureHaveBytes();
        // The buffer spans the whole storage, which may include data not in the view
        ByteBuffer b = ByteBuffer.wrap(storage);
        // Return as read-only.
        return b.asReadOnlyBuffer();
    }

    /**
     * This method creates an actual byte array from the underlying String if none yet exists.
     */
    private void ensureHaveBytes() {
        if (storage == null) {
            // We can't avoid creating the byte array any longer (index0 already correct)
            storage = StringUtil.toBytes(bufString);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an actual byte array from the underlying String if none yet exists.
     */
    @SuppressWarnings("deprecation")
    @Override
    public Pointer getBuf() {
        ensureHaveBytes();
        return super.getBuf();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an actual byte array from the underlying String if none yet exists.
     */
    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int index) {
        ensureHaveBytes();
        return super.getPointer(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an actual byte array from the underlying String if none yet exists.
     */
    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int... indices) {
        ensureHaveBytes();
        return super.getPointer(indices);
    }

    /**
     * The <code>toString()</code> method of a <code>SimpleStringBuffer</code> simply produces the
     * underlying <code>String</code>.
     */
    @Override
    public String toString() {
        return bufString;
    }

    /**
     * A <code>SimpleStringBuffer.SimpleStringView</code> represents a contiguous subsequence of
     * another <code>SimpleStringBuffer</code>.
     */
    static class SimpleStringView extends SimpleStringBuffer {

        /** The buffer on which this is a slice view */
        PyBuffer root;

        /**
         * Construct a slice of a SimpleStringBuffer.
         *
         * @param root buffer which will be acquired and must be released ultimately
         * @param flags the request flags of the consumer that requested the slice
         * @param buf becomes the buffer of bytes for this object
         */
        public SimpleStringView(PyBuffer root, int flags, String bufString) {
            // Create a new SimpleStringBuffer on the string passed in
            super(flags, root.getObj(), bufString);
            // Get a lease on the root PyBuffer
            this.root = root.getBuffer(FULL_RO);
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }
    }
}
