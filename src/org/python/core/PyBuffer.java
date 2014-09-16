package org.python.core;

import java.nio.ByteBuffer;

/**
 * The Jython buffer API for access to a byte array within an exporting object. This interface is
 * the counterpart of the CPython <code>Py_buffer</code> struct. Several concrete types implement
 * this interface in order to provide tailored support for different storage organisations.
 */
public interface PyBuffer extends PyBUF, BufferProtocol, AutoCloseable {

    /*
     * The different behaviours required as the actual structure of the buffer changes (from one
     * exporter to another, that is) should be dealt with using polymorphism. The implementation of
     * those types may then calculate indices etc. without checking e.g for whether the strides
     * array must be used, or the array is C or F contiguous, since they know the answer to these
     * questions already, and can just get on with the request in their own way.
     *
     * The issue of consumer requests via getBuffer(int) is greatly simplified relative to CPython
     * by the choice always to supply a full description of the buffer organisation, whether the
     * consumer asked for it in the flags or not. Of course, implementations don't actually have to
     * create (for example) a strides array until getStrides() is called.
     */

    // Informational methods inherited from PyBUF
    //
    // boolean isReadonly();
    // int getNdim();
    // int[] getShape();
    // int getLen();

    /**
     * Return the byte indexed from a one-dimensional buffer with item size one. This is part of the
     * fully-encapsulated API: the buffer implementation exported takes care of navigating the
     * structure of the buffer. Results are undefined where the number of dimensions is not one or
     * if <code>itemsize&gt;1</code>.
     *
     * @param index to retrieve from
     * @return the item at index, which is a byte
     */
    byte byteAt(int index) throws IndexOutOfBoundsException;

    /**
     * Return the unsigned byte value indexed from a one-dimensional buffer with item size one. This
     * is part of the fully-encapsulated API: the exporter takes care of navigating the structure of
     * the buffer. Results are undefined where the number of dimensions is not one or if
     * <code>itemsize&gt;1</code>.
     *
     * @param index to retrieve from
     * @return the item at index, treated as an unsigned byte, <code>=0xff & byteAt(index)</code>
     */
    int intAt(int index) throws IndexOutOfBoundsException;

    /**
     * Store the given byte at the indexed location in of a one-dimensional buffer with item size
     * one. This is part of the fully-encapsulated API: the buffer implementation exported takes
     * care of navigating the structure of the buffer. Results are undefined where the number of
     * dimensions is not one or if <code>itemsize&gt;1</code>.
     *
     * @param value to store
     * @param index to location
     */
    void storeAt(byte value, int index) throws IndexOutOfBoundsException;

    // Access to n-dimensional array
    //
    /**
     * Return the byte indexed from an N-dimensional buffer with item size one. This is part of the
     * fully-encapsulated API: the buffer implementation exported takes care of navigating the
     * structure of the buffer. The indices must be correct in number and range for the array shape.
     * Results are undefined where <code>itemsize&gt;1</code>.
     *
     * @param indices specifying location to retrieve from
     * @return the item at location, which is a byte
     */
    byte byteAt(int... indices) throws IndexOutOfBoundsException;

    /**
     * Return the unsigned byte value indexed from an N-dimensional buffer with item size one. This
     * is part of the fully-encapsulated API: the buffer implementation exported takes care of
     * navigating the structure of the buffer. The indices must be correct in number and range for
     * the array shape. Results are undefined where <code>itemsize&gt;1</code>.
     *
     * @param indices specifying location to retrieve from
     * @return the item at location, treated as an unsigned byte, <code>=0xff & byteAt(index)</code>
     */
    int intAt(int... indices) throws IndexOutOfBoundsException;

    /**
     * Store the given byte at the indexed location in of an N-dimensional buffer with item size
     * one. This is part of the fully-encapsulated API: the exporter takes care of navigating the
     * structure of the buffer. The indices must be correct in number and range for the array shape.
     * Results are undefined where <code>itemsize&gt;1</code>.
     *
     * @param value to store
     * @param indices specifying location to store at
     */
    void storeAt(byte value, int... indices) throws IndexOutOfBoundsException;

    // Bulk access in one dimension
    //
    /**
     * Copy the contents of the buffer to the destination byte array. The number of bytes will be
     * that returned by {@link #getLen()}, and the order is the storage order in the exporter.
     * (Note: Correct ordering for multidimensional arrays, including those with indirection needs
     * further study.)
     *
     * @param dest destination byte array
     * @param destPos index in the destination array of the byte [0]
     * @throws IndexOutOfBoundsException if the destination cannot hold it
     */
    void copyTo(byte[] dest, int destPos) throws IndexOutOfBoundsException, PyException;

    /**
     * Copy a simple slice of the buffer to the destination byte array, defined by a starting index
     * and length in the source buffer. This may validly be done only for a one-dimensional buffer,
     * as the meaning of the starting index is otherwise not defined. The length (like the source
     * index) is in source buffer <b>items</b>: <code>length*itemsize</code> bytes will be occupied
     * in the destination.
     *
     * @param srcIndex starting index in the source buffer
     * @param dest destination byte array
     * @param destPos index in the destination array of the item [0,...]
     * @param length number of items to copy
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     */
    void copyTo(int srcIndex, byte[] dest, int destPos, int length)     // mimic arraycopy args
            throws IndexOutOfBoundsException, PyException;

    /**
     * Copy bytes from a slice of a (Java) byte array into the buffer. This may validly be done only
     * for a one-dimensional buffer, as the meaning of the starting index is not otherwise defined.
     * The length (like the destination index) is in buffer <b>items</b>:
     * <code>length*itemsize</code> bytes will be read from the source.
     *
     * @param src source byte array
     * @param srcPos location in source of first byte to copy
     * @param destIndex starting index in the destination (i.e. <code>this</code>)
     * @param length number of bytes to copy in
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     * @throws PyException (TypeError) if read-only buffer
     */
    void copyFrom(byte[] src, int srcPos, int destIndex, int length)    // mimic arraycopy args
            throws IndexOutOfBoundsException, PyException;

    /**
     * Copy the whole of another PyBuffer into this buffer. This may validly be done only for
     * buffers that are consistent in their dimensions. When it is necessary to copy partial
     * buffers, this may be achieved using a buffer slice on the source or destination.
     *
     * @param src source buffer
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     * @throws PyException (TypeError) if read-only buffer
     */
    void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException;

    // Bulk access in n-dimensions may be added here if desired semantics can be settled
    //

    // Buffer management
    //
    /**
     * {@inheritDoc}
     * <p>
     * When a <code>PyBuffer</code> is the target, the same checks are carried out on the consumer
     * flags, and a return will normally be a reference to that buffer. A Jython
     * <code>PyBuffer</code> keeps count of these re-exports in order to match them with the number
     * of calls to {@link #release()}. When the last matching <code>release()</code> arrives it is
     * considered "final", and release actions may then take place on the exporting object. After
     * the final release of a buffer, a call to <code>getBuffer</code> should raise an exception.
     */
    @Override
    PyBuffer getBuffer(int flags) throws PyException;

    /**
     * A buffer is (usually) a view onto to the internal state of an exporting object, and that
     * object may have to restrict its behaviour while the buffer exists. The consumer must
     * therefore say when it has finished with the buffer if the exporting object is to be released
     * from this constraint. Each consumer that obtains a reference to a buffer by means of a call
     * to {@link BufferProtocol#getBuffer(int)} or {@link PyBuffer#getBuffer(int)} should make a
     * matching call to {@link #release()}. The consumer may be sharing the <code>PyBuffer</code>
     * with other consumers and the buffer uses the pairing of <code>getBuffer</code> and
     * <code>release</code> to manage the lock on behalf of the exporter. It is an error to make
     * more than one call to <code>release</code> for a single call to <code>getBuffer</code>.
     */
    void release();

    /** An alias for {@link #release()} to satisfy {@link AutoCloseable}. */
    @Override
    void close();

    /**
     * True only if the buffer has been released with (the required number of calls to)
     * {@link #release()} or some equivalent operation. The consumer may be sharing the reference
     * with other consumers and the buffer only achieves the released state when all consumers who
     * called <code>getBuffer</code> have called <code>release</code>.
     */
    boolean isReleased();

    /**
     * Equivalent to {@link #getBufferSlice(int, int, int, int)} with stride 1.
     *
     * @param flags specifying features demanded and the navigational capabilities of the consumer
     * @param start index in the current buffer
     * @param length number of items in the required slice
     * @return a buffer representing the slice
     */
    public PyBuffer getBufferSlice(int flags, int start, int length);

    /**
     * Get a <code>PyBuffer</code> that represents a slice of the current one described in terms of
     * a start index, number of items to include in the slice, and the stride in the current buffer.
     * A consumer that obtains a <code>PyBuffer</code> with <code>getBufferSlice</code> must release
     * it with {@link PyBuffer#release} just as if it had been obtained with
     * {@link PyBuffer#getBuffer(int)}
     * <p>
     * Suppose that <i>x(i)</i> denotes the <i>i</i>th element of the current buffer, that is, the
     * byte retrieved by <code>this.byteAt(i)</code> or the unit indicated by
     * <code>this.getPointer(i)</code>. A request for a slice where <code>start</code> <i>= s</i>,
     * <code>length</code> <i>= N</i> and <code>stride</code> <i>= m</i>, results in a buffer
     * <i>y</i> such that <i>y(k) = x(s+km)</i> where <i>k=0..(N-1)</i>. In Python terms, this is
     * the slice <i>x[s : s+(N-1)m+1 : m]</i> (if <i>m&gt;0</i>) or the slice <i>x[s : s+(N-1)m-1 :
     * m]</i> (if <i>m&lt;0</i>). Implementations should check that this range is entirely within
     * the current buffer.
     * <p>
     * In a simple buffer backed by a contiguous byte array, the result is a strided PyBuffer on the
     * same storage but where the offset is adjusted by <i>s</i> and the stride is as supplied. If
     * the current buffer is already strided and/or has an item size larger than single bytes, the
     * new <code>start</code> index, <code>length</code> and <code>stride</code> will be translated
     * from the arguments given, through this buffer's stride and item size. The caller always
     * expresses <code>start</code> and <code>strides</code> in terms of the abstract view of this
     * buffer.
     *
     * @param flags specifying features demanded and the navigational capabilities of the consumer
     * @param start index in the current buffer
     * @param length number of items in the required slice
     * @param stride index-distance in the current buffer between consecutive items in the slice
     * @return a buffer representing the slice
     */
    public PyBuffer getBufferSlice(int flags, int start, int length, int stride);

    // java.nio access to actual storage
    //

    /**
     * Obtain a {@link java.nio.ByteBuffer} giving access to the bytes that hold the data being
     * exported to the consumer. For a one-dimensional contiguous buffer, assuming the following
     * client code where <code>obj</code> has type <code>BufferProtocol</code>:
     *
     * <pre>
     * PyBuffer a = obj.getBuffer(PyBUF.SIMPLE);
     * int itemsize = a.getItemsize();
     * ByteBuffer bb = a.getNIOBuffer();
     * </pre>
     *
     * the item with index <code>bb.pos()+k</code> is in the buffer <code>bb</code> at positions
     * <code>bb.pos()+k*itemsize</code> to <code>bb.pos()+(k+1)*itemsize - 1</code> inclusive. And
     * if <code>itemsize==1</code>, the item is simply the byte at position <code>bb.pos()+k</code>.
     * The buffer limit is set to the first byte beyond the valid data. A block read or write will
     * therefore access the contents sequentially.
     * <p>
     * If the buffer is multidimensional or non-contiguous (strided), the buffer position is still
     * the (first byte of) the item at index <code>[0]</code> or <code>[0,...,0]</code>, and the
     * limit is one item beyond the valid data. However, it is necessary to navigate <code>bb</code>
     * using the <code>shape</code>, <code>strides</code> and maybe <code>suboffsets</code> provided
     * by the API.
     *
     * @return a ByteBuffer equivalent to the exported data contents.
     */
    ByteBuffer getNIOByteBuffer();

    // Direct access to actual storage
    //

    /**
     * Determine whether the exporter is able to offer direct access to the exported storage as a
     * Java byte array (through the API that involves class {@link Pointer}), or only supports the
     * abstract API. See also {@link PyBUF#AS_ARRAY}.
     *
     * @return true if array access is supported, false if it is not.
     */
    boolean hasArray();

    /**
     * A class that references a <code>byte[]</code> array and a particular offset within it, as the
     * return type for methods that give direct access to byte-oriented data exported by a Python
     * object. In some contexts the consumer will be entitled to make changes to the contents of
     * this array, and in others not. See {@link PyBuffer#isReadonly()}. It is used by the Jython
     * buffer API roughly where the CPython buffer API uses a C (char *) pointer.
     */
    public static class Pointer {

        /** Reference to the array holding the bytes. */
        public byte[] storage;
        /** Starting position within the array for the data being pointed to. */
        public int offset;

        /**
         * Construct a reference to the given array and offset.
         *
         * @param storage array at reference
         * @param offset index of the reference byte
         */
        public Pointer(byte[] storage, int offset) {
            this.storage = storage;
            this.offset = offset;
        }
    }

    /**
     * Return a structure describing the slice of a byte array that holds the data being exported to
     * the consumer. For a one-dimensional contiguous buffer, assuming the following client code
     * where <code>obj</code> has type <code>BufferProtocol</code>:
     *
     * <pre>
     * PyBuffer a = obj.getBuffer(PyBUF.SIMPLE);
     * int itemsize = a.getItemsize();
     * PyBuffer.Pointer b = a.getBuf();
     * </pre>
     *
     * the item with index <code>k</code> is in the array <code>b.storage</code> at index
     * <code>[b.offset + k*itemsize]</code> to <code>[b.offset + (k+1)*itemsize - 1]</code>
     * inclusive. And if <code>itemsize==1</code>, the item is simply the byte
     * <code>b.storage[b.offset + k]</code>
     * <p>
     * If the buffer is multidimensional or non-contiguous, <code>storage[offset]</code> is still
     * the (first byte of) the item at index [0] or [0,...,0]. However, it is necessary to navigate
     * <code>b.storage</code> using the <code>shape</code>, <code>strides</code> and maybe
     * <code>suboffsets</code> provided by the API.
     *
     * @return structure defining the byte[] slice that is the shared data
     */
    PyBuffer.Pointer getBuf();

    /**
     * Return a structure describing the position in a byte array of a single item from the data
     * being exported to the consumer. For a one-dimensional contiguous buffer, assuming the
     * following client code where <code>obj</code> has type <code>BufferProtocol</code>:
     *
     * <pre>
     * int k = ... ;
     * PyBuffer a = obj.getBuffer(PyBUF.FULL);
     * int itemsize = a.getItemsize();
     * PyBuffer.Pointer b = a.getPointer(k);
     * </pre>
     *
     * the item with index <code>k</code> is in the array <code>b.storage</code> at index
     * <code>[b.offset]</code> to <code>[b.offset + itemsize - 1]</code> inclusive. And if
     * <code>itemsize==1</code>, the item is simply the byte <code>b.storage[b.offset]</code>
     * <p>
     * Essentially this is a method for computing the offset of a particular index. The client is
     * free to navigate the underlying buffer <code>b.storage</code> without respecting these
     * boundaries.
     *
     * @param index in the buffer to position the pointer
     * @return structure defining the byte[] slice that is the shared data
     */
    PyBuffer.Pointer getPointer(int index);

    /**
     * Return a structure describing the position in a byte array of a single item from the data
     * being exported to the consumer, in the case that array may be multi-dimensional. For a
     * 3-dimensional contiguous buffer, assuming the following client code where <code>obj</code>
     * has type <code>BufferProtocol</code>:
     *
     * <pre>
     * int i, j, k;
     * // ... calculation that assigns i, j, k
     * PyBuffer a = obj.getBuffer(PyBUF.FULL);
     * int itemsize = a.getItemsize();
     * PyBuffer.Pointer b = a.getPointer(i,j,k);
     * </pre>
     *
     * the item with index <code>[i,j,k]</code> is in the array <code>b.storage</code> at index
     * <code>[b.offset]</code> to <code>[b.offset + itemsize - 1]</code> inclusive. And if
     * <code>itemsize==1</code>, the item is simply the byte <code>b.storage[b.offset]</code>
     * <p>
     * Essentially this is a method for computing the offset of a particular index. The client is
     * free to navigate the underlying buffer <code>b.storage</code> without respecting these
     * boundaries. If the buffer is non-contiguous, the above description is still valid (since a
     * multi-byte item must itself be contiguously stored), but in any additional navigation of
     * <code>b.storage[]</code> to other units, the client must use the shape, strides and
     * sub-offsets provided by the API. Normally one starts <code>b = a.getBuf()</code> in order to
     * establish the offset of index [0,...,0].
     *
     * @param indices multidimensional index at which to position the pointer
     * @return structure defining the byte[] slice that is the shared data
     */
    PyBuffer.Pointer getPointer(int... indices);

    // Inherited from PyBUF and belonging here
    //
    // int[] getStrides();
    // int[] getSuboffsets();
    // boolean isContiguous(char order);

    // Interpretation of bytes as items
    /**
     * A format string in the language of Python structs describing how the bytes of each item
     * should be interpreted. Irrespective of the {@link PyBUF#FORMAT} bit in the consumer's call to
     * <code>getBuffer</code>, a valid <code>format</code> string is always returned (difference
     * from CPython).
     * <p>
     * Jython only implements "B" so far, and it is debatable whether anything fancier than
     * "&lt;n&gt;B" can be supported in Java.
     *
     * @return the format string
     */
    String getFormat();

    // Inherited from PyBUF and belonging here
    //
    // int getItemsize();

    /**
     * The toString() method of a buffer reproduces the byte values in the buffer (treated as
     * unsigned integers) as the character codes of a <code>String</code>.
     */
    @Override
    public String toString();

}
