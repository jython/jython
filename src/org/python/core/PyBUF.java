package org.python.core;

/**
 * This interface provides a base for the key interface of the buffer API, {@link PyBuffer},
 * including symbolic constants used by the consumer of a <code>PyBuffer</code> to specify its
 * requirements and assumptions. The Jython buffer API emulates the CPython buffer API.
 * <ul>
 * <li>There are two reasons for separating parts of <code>PyBuffer</code> into this interface: The
 * constants defined in CPython have the names <code>PyBUF_SIMPLE</code>,
 * <code>PyBUF_WRITABLE</code>, etc., and the trick of defining ours here means we can write
 * {@link PyBUF#SIMPLE}, {@link PyBUF#WRITABLE}, etc. so source code looks similar.</li>
 * <li>It is not so easy in Java as it is in C to treat a <code>byte</code> array as storing
 * anything other than <code>byte</code>, and we prepare for the possibility of buffers with a
 * series of different primitive types by defining here those methods that would be in common
 * between <code>(Byte)Buffer</code> and an assumed future <code>FloatBuffer</code> or
 * <code>TypedBuffer&lt;T&gt;</code>. (Compare <code>java.nio.Buffer</code>.)</li>
 * </ul>
 * Except for other interfaces, it is unlikely any classes would implement <code>PyBUF</code>
 * directly. Users of the Jython buffer API can mostly overlook the distinction and just use
 * <code>PyBuffer</code>.
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
     * array. The actual storage may be a linear array, but this is the number of dimensions in the
     * interpretation that the exporting object gives the data.
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
     * must not modify this array. A valid <code>shape</code> array is always returned (difference
     * from CPython).
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
     * <code>shape</code> array, and the item size.
     *
     * @return the total number of units stored.
     */
    int getLen();

    /**
     * The <code>strides</code> array gives the distance in the storage array between adjacent items
     * (in each dimension). In the rawest parts of the buffer API, the consumer of the buffer is
     * able to navigate the exported storage. The "strides" array is part of the support for
     * interpreting the buffer as an n-dimensional array of items. It provides the coefficients of
     * the "addressing polynomial". (More on this in the CPython documentation.) The consumer must
     * not modify this array. A valid <code>strides</code> array is always returned (difference from
     * CPython).
     *
     * @return the distance in the storage array between adjacent items (in each dimension)
     */
    int[] getStrides();

    /**
     * The <code>suboffsets</code> array is a further part of the support for interpreting the
     * buffer as an n-dimensional array of items, where the array potentially uses indirect
     * addressing (like a real Java array of arrays, in fact). This is only applicable when there
     * are more than 1 dimension and works in conjunction with the <code>strides</code> array. (More
     * on this in the CPython documentation.) When used, <code>suboffsets[k]</code> is an integer
     * index, bit a byte offset as in CPython. The consumer must not modify this array. When not
     * needed for navigation <code>null</code> is returned (as in CPython).
     *
     * @return suboffsets array or null in not necessary for navigation
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

    /* Constants taken from CPython object.h in v3.3 */

    /**
     * The maximum allowed number of dimensions (CPython restriction).
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
     * specify that it requires {@link PyBuffer#getFormat()} to return a <code>String</code>
     * indicating the type of the unit. This exists for compatibility with CPython, as Jython as the
     * format is always provided by <code>getFormat()</code>.
     */
    static final int FORMAT = 0x0004;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it is prepared to navigate the buffer as multi-dimensional using the
     * <code>shape</code> array. <code>getBuffer</code> will raise an exception if consumer does not
     * specify the flag but the exporter's buffer cannot be navigated without taking into account
     * its multiple dimensions.
     */
    static final int ND = 0x0008;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it expects to use the <code>strides</code> array. <code>getBuffer</code> will
     * raise an exception if consumer does not specify the flag but the exporter's buffer cannot be
     * navigated without using the <code>strides</code> array.
     */
    static final int STRIDES = 0x0010 | ND;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume C-order organisation of the units. <code>getBuffer</code> will
     * raise an exception if the exporter's buffer is not C-ordered. <code>C_CONTIGUOUS</code>
     * implies <code>STRIDES</code>.
     */
    // It is possible this should have been (0x20|ND) expressing the idea that C-order addressing
    // will be assumed *instead of* using a strides array.
    static final int C_CONTIGUOUS = 0x0020 | STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume Fortran-order organisation of the units. <code>getBuffer</code>
     * will raise an exception if the exporter's buffer is not Fortran-ordered.
     * <code>F_CONTIGUOUS</code> implies <code>STRIDES</code>.
     */
    static final int F_CONTIGUOUS = 0x0040 | STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it will assume a contiguous organisation of the units, but will enquire which
     * organisation it actually is.
     *
     * getBuffer will raise an exception if the exporter's buffer is not contiguous.
     * <code>ANY_CONTIGUOUS</code> implies <code>STRIDES</code>.
     */
    // Further CPython strangeness since it uses the strides array to answer the enquiry.
    static final int ANY_CONTIGUOUS = 0x0080 | STRIDES;
    /**
     * A constant used by the consumer in its call to {@link BufferProtocol#getBuffer(int)} to
     * specify that it understands the <code>suboffsets</code> array. <code>getBuffer</code> will
     * raise an exception if consumer does not specify the flag but the exporter's buffer cannot be
     * navigated without understanding the <code>suboffsets</code> array. <code>INDIRECT</code>
     * implies <code>STRIDES</code>.
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
     * Equivalent to <code>(INDIRECT | WRITABLE | FORMAT)</code>. Also use this as the request flags
     * if you plan only to use the fully-encapsulated API (<code>byteAt</code>, <code>storeAt</code>
     * , <code>copyTo</code>, <code>copyFrom</code>, etc.), without ever calling
     * {@link PyBuffer#getBuf()}.
     */
    static final int FULL = INDIRECT | WRITABLE | FORMAT;
    /**
     * Equivalent to <code>(INDIRECT | FORMAT)</code> Also use this as the request flags if you plan
     * only to use the fully-encapsulated API (<code>byteAt</code>, <code>copyTo</code>, etc.),
     * without ever calling {@link PyBuffer#getBuf()}.
     */
    static final int FULL_RO = INDIRECT | FORMAT;

    /* Constants for readability, not standard for CPython */

    /**
     * Field mask, use as in <code>if ((flags&NAVIGATION) == STRIDES) ...</code>. The importance of
     * the subset of flags defined by this mask is not so much in their "navigational" character as
     * in the way they are treated in a buffer request.
     * <p>
     * The <code>NAVIGATION</code> set are used to specify which navigation arrays the consumer will
     * use, and therefore the consumer must ask for all those necessary to use the buffer
     * successfully (which is a function of the buffer's actual type). Asking for extra ones is not
     * an error, since all are supplied (in Jython): asking for too few is an error.
     * <p>
     * Flags outside the <code>NAVIGATION</code> set, work the other way round. Asking for one the
     * buffer cannot match is an error: not asking for a feature the buffer does not have is an
     * error.
     */
    static final int NAVIGATION = SIMPLE | ND | STRIDES | INDIRECT;
    /**
     * A constant used by the exporter in processing {@link BufferProtocol#getBuffer(int)} to check
     * for assumed C-order organisation of the units.
     * <code>C_CONTIGUOUS = IS_C_CONTIGUOUS | STRIDES</code>.
     */
    static final int IS_C_CONTIGUOUS = C_CONTIGUOUS & ~STRIDES;
    /**
     * A constant used by the exporter in processing {@link BufferProtocol#getBuffer(int)} to check
     * for assumed C-order Fortran-order organisation of the units.
     * <code>F_CONTIGUOUS = IS_F_CONTIGUOUS | STRIDES</code>.
     */
    static final int IS_F_CONTIGUOUS = F_CONTIGUOUS & ~STRIDES;
    /**
     * Field mask, use as in <code>if ((flags&CONTIGUITY)== ... ) ...</code>.
     */
    static final int CONTIGUITY = (C_CONTIGUOUS | F_CONTIGUOUS | ANY_CONTIGUOUS) & ~STRIDES;
}