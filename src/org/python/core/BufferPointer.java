package org.python.core;

/**
 * A class that references a contiguous slice of a <code>byte[]</code> array for use in the buffer
 * API. This class simply bundles together a refernce to an array, a starting offset within that
 * array, and specification of the number of bytes that may validly be accessed at that offset. It
 * is used by the Jython buffer API roughly where the CPython buffer API uses a C (char *) pointer,
 * or such a pointer and a length.
 */
public class BufferPointer {

    /**
     * Reference to the array holding the bytes. Usually this is the actual storage exported by a
     * Python object. In some contexts the consumer will be entitled to make changes to the contents
     * of this array, and in others, not. See {@link PyBuffer#isReadonly()}.
     */
    public final byte[] storage;
    /** Starting position within the array for the data being pointed to. */
    public final int offset;
    /** Number of bytes within the array comprising the data being pointed to. */
    public final int size;

    /**
     * Refer to a contiguous slice of the given array.
     * 
     * @param storage array at reference
     * @param offset index of the first byte
     * @param size number of bytes being referred to
     */
    public BufferPointer(byte[] storage, int offset, int size) {
        if ((offset | size | (storage.length-(offset + size))) < 0) {
            throw Py.BufferError("Indexing error in buffer API");
        }
        this.storage = storage;
        this.offset = offset;
        this.size = size;
    }

    /**
     * Refer to the whole of a byte array.
     * 
     * @param storage array at reference
     */
    public BufferPointer(byte[] storage) {
        this.storage = storage;
        this.offset = 0;
        this.size = storage.length;
    }
}