package org.python.core;

/**
 * This interface provides a base for the key interface of the buffer API, {@link PyBuffer},
 * including symbolic constants used by the consumer of a <code>PyBuffer</code> to specify its
 * requirements. The Jython buffer API emulates the CPython buffer API closely.
 * <ul>
 * <li>There are two reasons for separating parts of <code>PyBuffer</code> into this interface: The
 * constants defined in CPython have the names <code>PyBUF_SIMPLE</code>,
 * <code>PyBUF_WRITABLE</code>, etc., and the trick of defining ours here means we can write
 * {@link PyBUF#SIMPLE}, {@link PyBUF#WRITABLE}, etc. so source code looks similar.</li>
 * <li>It is not so easy in Java as it is in C to treat a <code>byte</code> array as storing
 * anything other than <code>byte</code>, and we prepare for the possibility of buffers with a
 * series of different primitive types by defining here, those methods that would be in common
 * between <code>(Byte)Buffer</code> and an assumed future <code>FloatBuffer</code> or
 * <code>TypedBuffer&lt;T&gt;</code>. (Compare <code>java.nio.Buffer</code>.)</li>
 * </ul>
 * Except for other interfaces, it is unlikely any classes would implement <code>PyBUF</code>
 * directly.
 */
public interface PyBUF {

    /**
     * Determine whether the consumer is entitled to write to the exported storage.
     *
     * @return true if writing is not allowed, false if it is.
     */
    boolean isReadonly();

    /**
     * The number of dimensions to the buffer. This number is the length of the <code>shape</code>
     * array.
     *
     * @return number of dimensions
     */
    int getNdim();

    /**
     * An array reporting the size of the buffer, considered as a multidimensional array, in each
     * dimension and (by its length) number of dimensions. The size is the size in "items". An item
     * is the amount of buffer content addressed by one index or set of indices. In the simplest
     * case an item is a single unit (byte), and there is one dimension. In complex cases, the array
     * is multi-dimensional, and the item at each location is multi-unit (multi-byte). The consumer
     * must not modify this array.
     *
     * @return the dimensions of the buffer as an array
     */
    int[] getShape();

    /**
     * The number of units (bytes) stored in each indexable item.
     *
     * @return the number of units (bytes) comprising each item.
     */
    int getItemsize();

    /**
     * The total number of units (bytes) stored, which will be the product of the elements of the
     * shape, and the item size.
     *
     * @return the total number of units stored.
     */
    int getLen();

    /**
     * A buffer is (usually) coupled to the internal state of an exporting Python object, and that
     * object may have to restrict its behaviour while the buffer exists. The consumer must
     * therefore say when it has finished.
     */
    void release();

    /**
     * The "strides" array gives the distance in the storage array between adjacent items (in each
     * dimension). If the rawest parts of the buffer API, the consumer of the buffer is able to
     * navigate the exported storage. The "strides" array is part of the support for interpreting
     * the buffer as an n-dimensional array of items. In the one-dimensional case, the "strides"
     * array is In more dimensions, it provides the coefficients of the "addressing polynomial".
     * (More on this in the CPython documentation.) The consumer must not modify this array.
     *
     * @return the distance in the storage array between adjacent items (in each dimension)
     */
    int[] getStrides();

    /**
     * The "suboffsets" array is a further part of the support for interpreting the buffer as an
     * n-dimensional array of items, where the array potentially uses indirect addressing (like a
     * real Java array of arrays, in fact). This is only applicable when there are more than 1
     * dimension and works in conjunction with the <code>strides</code> array. (More on this in the
     * CPython documentation.) When used, <code>suboffsets[k]</code> is an integer index, bit a byte
     * offset as in CPython. The consumer must not modify this array.
     *
     * @return
     */
    int[] getSuboffsets();

    /**
     * Enquire whether the array is represented contiguously in the backing storage, according to C
     * or Fortran ordering. A one-dimensional contiguous array is both.
     *
     * @param order 'C', 'F' or 'A', as the storage order is C, Fortran or either.
     * @return true iff the array is stored contiguously in the order specified
     */
    boolean isContiguous(char order);

    /* Constants taken from CPython object.h in v3.3.0a */

    /**
     * The maximum allowed number of dimensions (NumPy restriction?).
     */
    static final int MAX_NDIM = 64;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it expects to write to the buffer contents. getBuffer will raise an exception if
     * the exporter's buffer cannot meet this requirement.
     */
    static final int WRITABLE = 0x0001;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it assumes a simple one-dimensional organisation of the exported storage with
     * item size of one. getBuffer will raise an exception if the consumer sets this flag and the
     * exporter's buffer cannot be navigated that simply.
     */
    static final int SIMPLE = 0;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it requires {@link PyBuffer#getFormat()} to return the type of the unit (rather
     * than return <code>null</code>).
     */
    // I don't understand why we need this, or why format MUST be null of this is not set.
    static final int FORMAT = 0x0004;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it it is prepared to navigate the buffer as multi-dimensional.
     * <code>getBuffer</code> will raise an exception if consumer does not specify the flag but the
     * exporter's buffer cannot be navigated without taking into account its multiple dimensions.
     */
    static final int ND = 0x0008 | SIMPLE;    // Differs from CPython by or'ing in SIMPLE
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it it expects to use the "strides" array. <code>getBuffer</code> will raise an
     * exception if consumer does not specify the flag but the exporter's buffer cannot be navigated
     * without using the "strides" array.
     */
    static final int STRIDES = 0x0010 | ND;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume C-order organisation of the units. <code>getBuffer</code> will raise an
     * exception if the exporter's buffer is not C-ordered. <code>C_CONTIGUOUS</code> implies
     * <code>STRIDES</code>.
     */
    static final int C_CONTIGUOUS = 0x0020 | STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume Fortran-order organisation of the units. <code>getBuffer</code> will raise an
     * exception if the exporter's buffer is not Fortran-ordered. <code>F_CONTIGUOUS</code> implies
     * <code>STRIDES</code>.
     */
    static final int F_CONTIGUOUS = 0x0040 | STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it
     *
     * getBuffer will raise an exception if the exporter's buffer is not contiguous.
     * <code>ANY_CONTIGUOUS</code> implies <code>STRIDES</code>.
     */
    static final int ANY_CONTIGUOUS = 0x0080 | STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it understands the "suboffsets" array. <code>getBuffer</code> will raise an
     * exception if consumer does not specify the flag but the exporter's buffer cannot be navigated
     * without understanding the "suboffsets" array. <code>INDIRECT</code> implies
     * <code>STRIDES</code>.
     */
    static final int INDIRECT = 0x0100 | STRIDES;
    /**
     * Equivalent to <code>(ND | WRITABLE)</code>
     */
    static final int CONTIG = ND | WRITABLE;
    /**
     * Equivalent to <code>ND</code>
     */
    static final int CONTIG_RO = ND;
    /**
     * Equivalent to <code>(STRIDES | WRITABLE)</code>
     */
    static final int STRIDED = STRIDES | WRITABLE;
    /**
     * Equivalent to <code>STRIDES</code>
     */
    static final int STRIDED_RO = STRIDES;
    /**
     * Equivalent to <code>(STRIDES | WRITABLE | FORMAT)</code>
     */
    static final int RECORDS = STRIDES | WRITABLE | FORMAT;
    /**
     * Equivalent to <code>(STRIDES | FORMAT)</code>
     */
    static final int RECORDS_RO = STRIDES | FORMAT;
    /**
     * Equivalent to <code>(INDIRECT | WRITABLE | FORMAT)</code>
     */
    static final int FULL = INDIRECT | WRITABLE | FORMAT;
    /**
     * Equivalent to <code>(INDIRECT | FORMAT)</code>
     */
    static final int FULL_RO = INDIRECT | FORMAT;

    /* Constants for readability, not standard for CPython */

    /**
     * Field mask, use as in <code>if ((capabilityFlags&ORGANISATION) == STRIDES) ...</code>.
     */
    static final int ORGANISATION = SIMPLE | ND | STRIDES | INDIRECT;
    /**
     * Field mask, use as in if <code>((capabilityFlags&ORGANIZATION) == STRIDES) ...</code>.
     *
     * @see #ORGANISATION
     */
    static final int ORGANIZATION = ORGANISATION;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume C-order organisation of the units, irrespective of whether
     * the strides array is to be provided. <code>getBuffer</code> will raise an
     * exception if the exporter's buffer is not C-ordered. <code>C_CONTIGUOUS = IS_C_CONTIGUOUS | STRIDES</code>.
     */
    static final int IS_C_CONTIGUOUS = C_CONTIGUOUS & ~STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume Fortran-order organisation of the units, irrespective of whether
     * the strides array is to be provided. <code>getBuffer</code> will raise an
     * exception if the exporter's buffer is not Fortran-ordered. <code>F_CONTIGUOUS = IS_F_CONTIGUOUS | STRIDES</code>.
     */
    static final int IS_F_CONTIGUOUS = F_CONTIGUOUS & ~STRIDES;
    /**
     * Field mask, use as in <code>if (capabilityFlags&CONTIGUITY== ... ) ...</code>.
     */
    static final int CONTIGUITY = (C_CONTIGUOUS | F_CONTIGUOUS | ANY_CONTIGUOUS) & ~STRIDES;
}