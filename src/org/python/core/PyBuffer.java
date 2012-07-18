package org.python.core;

/**
 * The Jython buffer API for access to a byte array within an exporting object. This interface is
 * the counterpart of the CPython <code>Py_buffer</code> struct. Several concrete types implement
 * this interface in order to provide tailored support for different storage organisations.
 */
public interface PyBuffer extends PyBUF {

    /*
     * The different behaviours required as the actual structure of the buffer changes (from one
     * exporter to another, that is) should be dealt with using polymorphism. The implementation of
     * those types may then calculate indices etc. without checking e.g for whether the strides
     * array must be used, or the array is C or F contiguous, since they know the answer to these
     * questions already, and can just get on with the request in their own way.
     *
     * The issue of consumer requests is different: the strides array will be present if the
     * consumer asked for it, yet the methods of the buffer implementation do not have to use it
     * (and won't).
     */

    // Informational methods inherited from PyBUF
    //
    // boolean isReadonly();
    // int getNdim();
    // int[] getShape();
    // int getLen();

    /**
     * Return the byte indexed from a one-dimensional buffer with item size one. This is part of the
     * fully-encapsulated API: the exporter takes care of navigating the structure of the buffer.
     * Results are undefined where the number of dimensions is not one or if
     * <code>itemsize&gt;1</code>.
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
     * one. This is part of the fully-encapsulated API: the exporter takes care of navigating the
     * structure of the buffer. Results are undefined where the number of dimensions is not one or
     * if <code>itemsize&gt;1</code>.
     *
     * @param value to store
     * @param index to location
     */
    void storeAt(byte value, int index) throws IndexOutOfBoundsException;

    // Access to n-dimensional array
    //
    /**
     * Return the byte indexed from an N-dimensional buffer with item size one. This is part of the
     * fully-encapsulated API: the exporter takes care of navigating the structure of the buffer.
     * The indices must be correct in length and value for the array shape. Results are undefined
     * where <code>itemsize&gt;1</code>.
     *
     * @param indices specifying location to retrieve from
     * @return the item at location, which is a byte
     */
    byte byteAt(int... indices) throws IndexOutOfBoundsException;

    /**
     * Return the unsigned byte value indexed from an N-dimensional buffer with item size one. This
     * is part of the fully-encapsulated API: the exporter takes care of navigating the structure of
     * the buffer. The indices must be correct in length and value for the array shape. Results are
     * undefined where <code>itemsize&gt;1</code>.
     *
     * @param index to retrieve from
     * @return the item at location, treated as an unsigned byte, <code>=0xff & byteAt(index)</code>
     */
    int intAt(int... indices) throws IndexOutOfBoundsException;

    /**
     * Store the given byte at the indexed location in of an N-dimensional buffer with item size
     * one. This is part of the fully-encapsulated API: the exporter takes care of navigating the
     * structure of the buffer. The indices must be correct in length and value for the array shape.
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
     * that returned by {@link #getLen()}, and the order is the natural ordering according to the
     * contiguity type.
     *
     * @param dest destination byte array
     * @param destPos index in the destination array of the byte [0]
     * @throws IndexOutOfBoundsException if the destination cannot hold it
     */
    void copyTo(byte[] dest, int destPos) throws IndexOutOfBoundsException;

    /**
     * Copy a simple slice of the buffer to the destination byte array, defined by a starting index
     * and length in the source buffer. This may validly be done only for a one-dimensional buffer,
     * as the meaning of the starting index is otherwise not defined.
     *
     * @param srcIndex starting index in the source buffer
     * @param dest destination byte array
     * @param destPos index in the destination array of the byte [0,...]
     * @param length number of bytes to copy
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     */
    void copyTo(int srcIndex, byte[] dest, int destPos, int length)     // mimic arraycopy args
            throws IndexOutOfBoundsException;

    /**
     * Copy bytes from a slice of a (Java) byte array into the buffer. This may validly be done only
     * for a one-dimensional buffer, as the meaning of the starting index is otherwise not defined.
     *
     * @param src source byte array
     * @param srcPos location in source of first byte to copy
     * @param destIndex starting index in the destination (i.e. <code>this</code>)
     * @param length number of bytes to copy in
     * @throws IndexOutOfBoundsException if access out of bounds in source or destination
     * @throws PyException (BufferError) if read-only buffer
     */
    void copyFrom(byte[] src, int srcPos, int destIndex, int length)    // mimic arraycopy args
            throws IndexOutOfBoundsException, PyException;

    // Bulk access in n-dimensions may be added here if desired semantics can be settled
    //

    // Buffer management inherited from PyBUF
    //
    // void release();

    // Direct access to actual storage
    //
    /**
     * Return a structure describing the slice of a byte array that holds the data being exported to
     * the consumer. For a one-dimensional contiguous buffer, assuming the following client code
     * where <code>obj</code> has type <code>BufferProtocol</code>:
     *
     * <pre>
     *
     * PyBuffer a = obj.getBuffer();
     * int itemsize = a.getItemsize();
     * BufferPointer b = a.getBuf();
     * </pre>
     *
     * the item with index <code>k</code> is in the array <code>b.storage</code> at index
     * <code>[b.offset + k*itemsize]</code> to <code>[b.offset + (k+1)*itemsize - 1]</code>
     * inclusive. And if <code>itemsize==1</code>, the item is simply the byte
     * <code>b.storage[b.offset + k]</code>
     * <p>
     * If the buffer is multidimensional or non-contiguous, <code>b.storage[b.offset]</code> is
     * still the (first byte of) the item at index [0] or [0,...,0]. However, it is necessary to
     * navigate <code>b</code> using the shape, strides and sub-offsets provided by the API.
     *
     * @return structure defining the byte[] slice that is the shared data
     */
    BufferPointer getBuf();

    /**
     * Return a structure describing the slice of a byte array that holds a single item from the
     * data being exported to the consumer. For a one-dimensional contiguous buffer, assuming the
     * following client code where <code>obj</code> has type <code>BufferProtocol</code>:
     *
     * <pre>
     * int k = ... ;
     * PyBuffer a = obj.getBuffer();
     * int itemsize = a.getItemsize();
     * BufferPointer b = a.getPointer(k);
     * </pre>
     *
     * the item with index <code>k</code> is in the array <code>b.storage</code> at index
     * <code>[b.offset]</code> to <code>[b.offset + itemsize - 1]</code> inclusive. And if
     * <code>itemsize==1</code>, the item is simply the byte <code>b.storage[b.offset]</code>
     * <p>
     * Essentially this is a method for computing the offset of a particular index. Although
     * <code>b.size==itemsize</code>, the client is free to navigate the underlying buffer
     * <code>b.storage</code> without respecting these boundaries.
     *
     * @param index in the buffer to position the pointer
     * @return structure defining the byte[] slice that is the shared data
     */
    BufferPointer getPointer(int index);

    /**
     * Return a structure describing the slice of a byte array that holds a single item from the
     * data being exported to the consumer, in the case that array may be multi-dimensional. For an
     * 3-dimensional contiguous buffer, assuming the following client code where <code>obj</code>
     * has type <code>BufferProtocol</code>:
     *
     * <pre>
     * int i, j, k = ... ;
     * PyBuffer a = obj.getBuffer();
     * int itemsize = a.getItemsize();
     * BufferPointer b = a.getPointer(i,j,k);
     * </pre>
     *
     * the item with index <code>[i,j,k]</code> is in the array <code>b.storage</code> at index
     * <code>[b.offset]</code> to <code>[b.offset + itemsize - 1]</code> inclusive. And if
     * <code>itemsize==1</code>, the item is simply the byte <code>b.storage[b.offset]</code>
     * <p>
     * Essentially this is a method for computing the offset of a particular index. Although
     * <code>b.size==itemsize</code>, the client is free to navigate the underlying buffer
     * <code>b.storage</code> without respecting these boundaries.
     * <p>
     * If the buffer is also non-contiguous, <code>b.storage[b.offset]</code> is still the (first
     * byte of) the item at index [0,...,0]. However, it is necessary to navigate <code>b</code>
     * using the shape, strides and sub-offsets provided by the API.
     *
     * @param indices multidimensional index at which to position the pointer
     * @return structure defining the byte[] slice that is the shared data
     */
    BufferPointer getPointer(int... indices);

    // Inherited from PyBUF and belonging here
    //
    // int[] getStrides();
    // int[] getSuboffsets();
    // boolean isContiguous(char order);

    // Interpretation of bytes as items
    /**
     * A format string in the language of Python structs describing how the bytes of each item
     * should be interpreted (or null if {@link PyBUF#FORMAT} was not part of the consumer's flags).
     * <p>
     * This is provided for compatibility with CPython. Jython only implements "B" so far, and it is
     * debatable whether anything fancier than "&lt;n&gt;B" can be supported in Java.
     *
     * @return the format string
     */
    String getFormat();

    // Inherited from PyBUF and belonging here
    //
    // int getItemsize();
}
