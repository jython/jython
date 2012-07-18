package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.BufferProtocol;
import org.python.core.util.StringUtil;

/**
 * Buffer API that appears to be a one-dimensional array of one-byte items providing read-only API,
 * but which is actually backed by a Java String. Some of the buffer API absolutely needs access to
 * the data as a byte array (those parts that involve a {@link BufferPointer} result), and therefore
 * this class must create a byte array from the String for them. However, it defers creation of a
 * byte array until that part of the API is actually used. This class overrides those methods in
 * SimpleReadonlyBuffer that would access the <code>buf</code> attribute to work out their results
 * from the String instead.
 */
public class SimpleStringBuffer extends SimpleReadonlyBuffer {

    /**
     * The string backing this PyBuffer. A substitute for {@link #buf} until we can no longer avoid
     * creating it.
     */
    private String bufString;

    /**
     * Partial counterpart to CPython <code>PyBuffer_FillInfo()</code> specific to the simple type
     * of buffer and called from the constructor. The base constructor will already have been
     * called, filling {@link #bufString} and {@link #obj}. And the method
     * {@link #assignCapabilityFlags(int, int, int, int)} has set {@link #capabilityFlags}.
     */
    protected void fillInfo(String bufString) {
        /*
         * We will already have called: assignCapabilityFlags(flags, requiredFlags, allowedFlags,
         * impliedFlags); So capabilityFlags holds the requests for shape, strides, writable, etc..
         */
        // Save the backing string
        this.bufString = bufString;

        // Difference from CPython: never null, even when the consumer doesn't request it
        shape = new int[1];
        shape[0] = bufString.length();

        // Following CPython: provide strides only when the consumer requests it
        if ((capabilityFlags & STRIDES) == STRIDES) {
            strides = SIMPLE_STRIDES;
        }

        // Even when the consumer requests suboffsets, the exporter is allowed to supply null.
        // In theory, the exporter could require that it be requested and still supply null.
    }

    /**
     * Provide an instance of SimpleReadonlyBuffer meeting the consumer's expectations as expressed
     * in the flags argument.
     *
     * @param exporter the exporting object
     * @param bufString storing the implementation of the object
     * @param flags consumer requirements
     */
    public SimpleStringBuffer(BufferProtocol exporter, String bufString, int flags) {
        super(exporter, null);
        assignCapabilityFlags(flags, REQUIRED_FLAGS, ALLOWED_FLAGS, IMPLIED_FLAGS);
        fillInfo(bufString);
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
    public byte byteAt(int index) throws IndexOutOfBoundsException {
        // Avoid creating buf by using String.charAt
        return (byte)bufString.charAt(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses {@link String#charAt(int)} rather than create an actual byte buffer.
     */
    @Override
    public int intAt(int index) throws IndexOutOfBoundsException {
        // Avoid creating buf by using String.charAt
        return bufString.charAt(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses {@link String#charAt(int)} rather than create an actual byte buffer.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
            throws IndexOutOfBoundsException {
        // Avoid creating buf by using String.charAt
        int endIndex = srcIndex + length, p = destPos;
        for (int i = srcIndex; i < endIndex; i++) {
            dest[p++] = (byte)bufString.charAt(i);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an actual byte buffer from the String if none yet exists.
     */
    @Override
    public BufferPointer getBuf() {
        if (buf == null) {
            // We can't avoid creating buf any longer
            buf = new BufferPointer(StringUtil.toBytes(bufString));
        }
        return buf;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an actual byte buffer from the String if none yet exists.
     */
    @Override
    public BufferPointer getPointer(int index) {
        getBuf(); // Ensure buffer created
        return super.getPointer(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method creates an actual byte buffer from the String if none yet exists.
     */
    @Override
    public BufferPointer getPointer(int... indices) {
        getBuf(); // Ensure buffer created
        return super.getPointer(indices);
    }

}
