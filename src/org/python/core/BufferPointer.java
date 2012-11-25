package org.python.core;

/**
 * A class that references a specified <code>byte[]</code> array and an offset in it to be treated
 * as "index zero", for use in the buffer API. This class simply bundles together a reference to an
 * array and a particular offset within that array. It is used by the Jython buffer API roughly
 * where the CPython buffer API uses a C (char *) pointer.
 */
public class BufferPointer {

    /**
     * Reference to the backing array. Usually this is the actual storage exported by a Python
     * object. In some contexts the consumer will be entitled to make changes to the contents of
     * this array, and in others, not. See {@link PyBuffer#isReadonly()}.
     */
    public final byte[] storage;
    /** Starting position within the array for index calculations: "index zero". */
    public final int offset;

    /**
     * Refer to an offset in the given array.
     *
     * @param storage array at reference
     * @param offset index of the first byte
     */
    public BufferPointer(byte[] storage, int offset) {
        // No checks: keep it simple
        this.storage = storage;
        this.offset = offset;
    }

    /**
     * Refer to the whole of a byte array.
     *
     * @param storage array at reference
     */
    public BufferPointer(byte[] storage) {
        this.storage = storage;
        this.offset = 0;
    }
}