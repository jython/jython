package org.python.core.buffer;

import java.nio.ByteBuffer;

import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Base implementation of the Buffer API providing variables and accessors for the navigational
 * arrays, methods for expressing and checking the buffer request flags, methods and mechanism for
 * get-release counting, boilerplate error checks and their associated exceptions, and default
 * implementations of some methods for access to the buffer content. The design aim is to ensure
 * unglamorous common code need only be implemented once.
 * <p>
 * Where provided, the buffer access methods are appropriate to 1-dimensional arrays where the units
 * are single bytes, stored contiguously. Sub-classes that deal with N-dimensional arrays,
 * non-contiguous storage and items that are not single bytes must override the default
 * implementations.
 * <p>
 * This base implementation is writable only if {@link PyBUF#WRITABLE} is in the feature flags
 * passed to the constructor. Otherwise, all methods for write access raise a
 * <code>BufferError</code> read-only exception and {@link #isReadonly()} returns <code>true</code>.
 * Sub-classes can follow the same pattern, setting {@link PyBUF#WRITABLE} in the constructor and,
 * if they have to, overriding the operations that write (<code>storeAt</code> and
 * <code>copyFrom</code>). The recommended pattern is:
 *
 * <pre>
 * if (isReadonly()) {
 *     throw notWritable();
 * }
 * // ... implementation of the write operation
 * </pre>
 * Another approach, used in the standard library, is to have distinct classes for the writable and
 * read-only variants. The implementors of simple buffers will find it efficient to override the
 * generic access methods to which performance might be sensitive, with a calculation specific to
 * their actual type.
 * <p>
 * At the time of writing, only one-dimensional buffers of item size one are used in the Jython
 * core.
 */
public abstract class BaseBuffer implements PyBuffer {

    /**
     * The dimensions of the array represented by the buffer. The length of the <code>shape</code>
     * array is the number of dimensions. The <code>shape</code> array should always be created and
     * filled (difference from CPython). This value is returned by {@link #getShape()}.
     */
    protected int[] shape;

    /**
     * Step sizes in the underlying buffer essential to correct translation of an index (or indices)
     * into an index into the storage. The <code>strides</code> array should always be created and
     * correctly filled to at least the length of the <code>shape</code> array (difference from
     * CPython). This value is returned by {@link #getStrides()}.
     */
    protected int[] strides;

    /**
     * Reference to the underlying <code>byte[]</code> storage that the exporter is sharing with the
     * consumer. The data need not occupy the whole array: in the constructor of a particular type
     * of buffer, the exporter usually indicates an offset to the first significant byte and length
     * (contiguous cases) or the index in <code>storage</code> that should be treated as the item
     * with index zero (retrieved say by {@link #byteAt(int)}).
     */
    protected byte[] storage;

    /**
     * Absolute index in <code>storage</code> of <code>item[0]</code>. In one dimension, for a
     * positive <code>stride</code> this is equal to the offset of the first byte used in
     * {@link #storage}, and for a negative <code>stride</code> it is the last. In an N-dimensional
     * buffer with strides of mixed sign, it could be anywhere in the data.
     */
    protected int index0;

    /**
     * Count the number of times {@link #release()} must be called before actual release actions
     * need to take place. Equivalently, this is the number of calls to
     * {@link BufferProtocol#getBuffer(int)} that have returned this object: one for the call on the
     * original exporting object that constructed <code>this</code>, and one for each subsequent
     * call to {@link PyBuffer#getBuffer(int)} that returned <code>this</code>.
     */
    protected int exports = 1;

    /**
     * Bit pattern using the constants defined in {@link PyBUF} that records the actual features
     * this buffer offers. When checking consumer flags against the features of the buffer, it is an
     * error if the consumer requests a capability the buffer does not offer, and it is an error if
     * the consumer does not specify that it will use a navigation array the buffer requires.
     * <p>
     * In order to support efficient checking with {@link #checkRequestFlags(int)} we store a
     * mutilated version of the apparent <code>featureFlags</code> in which the non-navigational
     * flags are inverted. The syndrome <code>S</code> of the error is computed as follows. Let
     * <code>N=1</code> where we are dealing with a navigation flag, let <code>F</code> be a buffer
     * feature flag, and let <code>X</code> be the consumer request flags.
     *
     * <pre>
     * A = F N X'
     * B = F'N'X
     * S = A + B = F N X' + F'N'X
     * </pre>
     *
     * In the above, <code>A=0</code> only if all the navigation flags set in <code>F</code> are
     * also set in <code>X</code>, and <code>B=0</code> only if all the non-navigation flags clear
     * in <code>F</code> are also clear in <code>X</code>. <code>S=0</code> only if both these
     * conditions are true and furthermore the positions of the <code>1</code>s in the syndrome
     * <code>S</code> tell us which bits in <code>X</code> are at fault. Now if we define:
     * <code>G = N F + N'F'</code> then the syndrome is:
     *
     * <pre>
     * S = G (N X' + N'X)
     * </pre>
     *
     * Which permits the check in one XOR and one AND operation instead of four ANDs and an OR. The
     * down-side is that we have to provide methods for setting and getting the actual flags in
     * terms a client might expect them to be expressed. We can recover the original <code>F</code>
     * since:
     *
     * <pre>
     * N G + N'G' = F
     * </pre>
     */
    private int gFeatureFlags = ~NAVIGATION; // featureFlags = 0

    /**
     * Construct an instance of BaseBuffer in support of a sub-class, specifying the 'feature
     * flags', or at least a starting set to be adjusted later. These are the features of the buffer
     * exported, not the flags that form the consumer's request. The buffer will be read-only unless
     * {@link PyBUF#WRITABLE} is set in the feature flags. {@link PyBUF#FORMAT} and
     * {@link PyBUF#AS_ARRAY} are implicitly added to the feature flags. The navigation arrays are
     * all null, awaiting action by the sub-class constructor. To complete initialisation, the
     * sub-class normally must assign: the buffer ( {@link #storage}, {@link #index0}), and the
     * navigation arrays ({@link #shape}, {@link #strides}), and call
     * {@link #checkRequestFlags(int)} passing the consumer's request flags.
     *
     * @param featureFlags bit pattern that specifies the actual features allowed/required
     */
    protected BaseBuffer(int featureFlags) {
        setFeatureFlags(featureFlags | FORMAT | AS_ARRAY);
    }

    /**
     * Get the features of this buffer expressed using the constants defined in {@link PyBUF}. A
     * client request may be tested against the consumer's request flags with
     * {@link #checkRequestFlags(int)}.
     *
     * @return capabilities of and navigation required by the exporter/buffer
     */
    protected final int getFeatureFlags() {
        return NAVIGATION ^ (~gFeatureFlags);
    }

    /**
     * Set the features of this buffer expressed using the constants defined in {@link PyBUF},
     * replacing any previous set. Set individual flags or add to those already set by using
     * {@link #addFeatureFlags(int)}.
     *
     * @param flags new value for the feature flags
     */
    protected final void setFeatureFlags(int flags) {
        gFeatureFlags = (~NAVIGATION) ^ flags;
    }

    /**
     * Add to the features of this buffer expressed using the constants defined in {@link PyBUF},
     * setting individual flags specified while leaving those already set. Equivalent to
     * <code>setFeatureFlags(flags | getFeatureFlags())</code>.
     *
     * @param flags to set within the feature flags
     */
    protected final void addFeatureFlags(int flags) {
        setFeatureFlags(flags | getFeatureFlags());
    }

    /**
     * General purpose method to check the consumer request flags (typically the argument to
     * {@link BufferProtocol#getBuffer(int)}) against the feature flags (see
     * {@link #getFeatureFlags()}) that characterise the features of the buffer, and to raise an
     * exception (Python <code>BufferError</code>) with an appropriate message in the case of a
     * mismatch. The flags are defined in the interface {@link PyBUF} and are used in two ways.
     * <p>
     * In a subset of the flags, the consumer specifies assumptions it makes about the index order
     * (contiguity) of the buffer, and whether it is writable. When the buffer implementation calls
     * this check method, it has already specified in {@link #setFeatureFlags(int)} what
     * capabilities this type (or instance) buffer actually has. It is an error, for the consumer to
     * specify in its request a feature that the buffer does not offer.
     * <p>
     * In a subset of the flags, the consumer specifies the set of navigational arrays (
     * <code>shape</code>, <code>strides</code>, and <code>suboffsets</code>) it intends to use in
     * navigating the buffer. When the buffer implementation calls this check method, it has already
     * specified in {@link #setFeatureFlags(int)} what navigation is necessary for the consumer to
     * make sense of the buffer. It is an error for the consumer <i>not to specify</i> the flag
     * corresponding to an array that the buffer deems necessary.
     *
     * @param flags capabilities of and navigation assumed by the consumer
     * @throws PyException (BufferError) when expectations do not correspond with the buffer
     */
    protected void checkRequestFlags(int flags) throws PyException {
        /*
         * It is an error if any of the navigation flags is 0 when it should be 1, or if any of the
         * non-navigation flags is 1 when it should be 0.
         */
        int syndrome = gFeatureFlags & (flags ^ NAVIGATION);
        if (syndrome != 0) {
            throw bufferErrorFromSyndrome(syndrome);
        }
    }

    @Override
    public boolean isReadonly() {
        // WRITABLE is a non-navigational flag, so is inverted in gFeatureFlags
        return (gFeatureFlags & WRITABLE) != 0;
    }

    @Override
    public boolean hasArray() {
        // AS_ARRAY is a non-navigational flag, so is inverted in gFeatureFlags
        return (gFeatureFlags & AS_ARRAY) != 0;
    }

    @Override
    public int getNdim() {
        return shape.length;
    }

    @Override
    public int[] getShape() {
        // Difference from CPython: never null, even when the consumer doesn't request it.
        return shape;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case, with any item size and stride.
     */
    @Override
    public int getLen() {
        // Correct if one-dimensional. Override with itemsize*product(shape).
        return shape[0] * getItemsize();
    }

    @Override
    public byte byteAt(int index) throws IndexOutOfBoundsException {
        return storage[calcIndex(index)];
    }

    @Override
    public int intAt(int index) throws IndexOutOfBoundsException {
        return 0xff & byteAt(index);
    }

    @Override
    public void storeAt(byte value, int index) throws IndexOutOfBoundsException, PyException {
        if (isReadonly()) {
            throw notWritable();
        }
        storage[calcIndex(index)] = value;
    }

    /**
     * Convert an item index (for a one-dimensional buffer) to an absolute byte index in the actual
     * storage being shared by the exporter. See {@link #calcIndex(int...)} for discussion.
     *
     * @param index from consumer
     * @return index in actual storage
     */
    protected int calcIndex(int index) throws IndexOutOfBoundsException {
        // Treat as one-dimensional
        return index0 + index * getStrides()[0];
    }

    @Override
    public byte byteAt(int... indices) throws IndexOutOfBoundsException {
        return storage[calcIndex(indices)];
    }

    @Override
    public int intAt(int... indices) throws IndexOutOfBoundsException {
        return 0xff & byteAt(indices);
    }

    @Override
    public void storeAt(byte value, int... indices) throws IndexOutOfBoundsException, PyException {
        if (isReadonly()) {
            throw notWritable();
        }
        storage[calcIndex(indices)] = value;
    }

    /**
     * Convert a multi-dimensional item index (if we are not using indirection) to an absolute byte
     * index in the actual storage array being shared by the exporter. The purpose of this method is
     * to allow a sub-class to define, in one place, an indexing calculation that maps the index as
     * provided by the consumer into an index in the storage known to the buffer.
     * <p>
     * In the usual case where the storage is referenced via the {@link #storage} and
     * {@link #index0} members, the buffer implementation may use <code>storage[calcIndex(i)]</code>
     * to reference the (first byte of) the item x[i]. This is what the default implementation of
     * accessors in <code>BaseBuffer</code> will do. In the simplest cases, calling
     * <code>calcIndex</code> may be an overhead to avoid, and an implementation will specialise the
     * accessors. The default implementation here is suited to N-dimensional arrays.
     *
     * @param indices of the item from the consumer
     * @return corresponding absolute index in storage
     */
    protected int calcIndex(int... indices) throws IndexOutOfBoundsException {
        final int N = checkDimension(indices);
        // In general: index0 + sum(k=0,N-1) indices[k]*strides[k]
        int index = index0;
        if (N > 0) {
            int[] strides = getStrides();
            for (int k = 0; k < N; k++) {
                index += indices[k] * strides[k];
            }
        }
        return index;
    }

    /**
     * Calculate the absolute byte index in the storage array of the last item of the exported data
     * (if we are not using indirection). This is the greatest value attained by
     * {@link #calcIndex(int...)}. The first byte not used will be one <code>itemsize</code> more
     * than the returned value.
     *
     * @return greatest absolute index in storage
     */
    protected int calcGreatestIndex() throws IndexOutOfBoundsException {
        final int N = shape.length;
        // If all the strides are positive, the maximal value is found from:
        // index = index0 + sum(k=0,N-1) (shape[k]-1)*strides[k]
        // but in general, for any k where strides[k]<=0, the term should be zero.
        int index = index0;
        if (N > 0) {
            int[] strides = getStrides();
            for (int k = 0; k < N; k++) {
                int stride = strides[k];
                if (stride > 0) {
                    index += (shape[k] - 1) * stride;
                }
            }
        }
        return index;
    }

    /**
     * Calculate the absolute byte index in the storage array of the first item of the exported data
     * (if we are not using indirection). This is the least value attained by
     * {@link #calcIndex(int...)}.
     *
     * @return least absolute index in storage
     */
    protected int calcLeastIndex() throws IndexOutOfBoundsException {
        final int N = shape.length;
        // If all the strides are positive, the maximal value is just index0,
        // but in general, we must allow strides[k]<=0 for some k:
        // index = index0 + sum(k=0,N-1) (strides[k]<0) ? (shape[k]-1)*strides[k] : 0
        int index = index0;
        if (N > 0) {
            int[] strides = getStrides();
            for (int k = 0; k < N; k++) {
                int stride = strides[k];
                if (stride < 0) {
                    index += (shape[k] - 1) * stride;
                }
            }
        }
        return index;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride.
     */
    @Override
    public void copyTo(byte[] dest, int destPos) throws IndexOutOfBoundsException {
        // Note shape[0] is the number of items in the array
        copyTo(0, dest, destPos, shape[0]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
            throws IndexOutOfBoundsException {

        // Data is here in the buffers
        int s = calcIndex(srcIndex);
        int d = destPos;

        // Pick up attributes necessary to choose an efficient copy strategy
        int itemsize = getItemsize();
        int stride = getStrides()[0];
        int skip = stride - itemsize;

        // Strategy depends on whether items are laid end-to-end contiguously or there are gaps
        if (skip == 0) {
            // stride == itemsize: straight copy of contiguous bytes
            System.arraycopy(storage, s, dest, d, length * itemsize);

        } else if (itemsize == 1) {
            // Non-contiguous copy: single byte items
            int limit = s + length * stride;
            for (; s < limit; s += stride) {
                dest[d++] = storage[s];
            }

        } else {
            // Non-contiguous copy: each time, copy itemsize bytes then skip
            int limit = s + length * stride;
            for (; s < limit; s += skip) {
                int t = s + itemsize;
                while (s < t) {
                    dest[d++] = storage[s++];
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int length)
            throws IndexOutOfBoundsException, PyException {

        // Block operation if read-only
        if (isReadonly()) {
            throw notWritable();
        }

        // Data is here in the buffers
        int s = srcPos;
        int d = calcIndex(destIndex);

        // Pick up attributes necessary to choose an efficient copy strategy
        int itemsize = getItemsize();
        int stride = getStrides()[0];
        int skip = stride - itemsize;

        // Strategy depends on whether items are laid end-to-end or there are gaps
        if (skip == 0) {
            // Straight copy of contiguous bytes
            System.arraycopy(src, srcPos, storage, d, length * itemsize);

        } else if (itemsize == 1) {
            // Non-contiguous copy: single byte items
            int limit = d + length * stride;
            for (; d != limit; d += stride) {
                storage[d] = src[s++];
            }

        } else {
            // Non-contiguous copy: each time, copy itemsize bytes then skip
            int limit = d + length * stride;
            for (; d != limit; d += skip) {
                int t = d + itemsize;
                while (d < t) {
                    storage[d++] = src[s++];
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case.
     */
    @Override
    public void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException {

        // Block operation if read-only and same length
        if (isReadonly()) {
            throw notWritable();
        } else if (src.getLen() != getLen() || src.getItemsize() != getItemsize()) {
            throw differentStructure();
        }

        // Data is here in the buffers
        int s = 0;
        int d = calcIndex(0);

        // Pick up attributes necessary to choose an efficient copy strategy
        int itemsize = getItemsize();
        int stride = getStrides()[0];

        // Strategy depends on whether items are laid end-to-end or there are gaps
        if (stride == itemsize) {
            // Straight copy to contiguous bytes
            src.copyTo(storage, d);

        } else if (itemsize == 1) {
            // Non-contiguous copy: single byte items
            int limit = d + src.getLen() * stride;
            for (; d != limit; d += stride) {
                storage[d] = src.byteAt(s++);
            }

        } else {
            // Non-contiguous copy: each time, copy itemsize bytes then skip
            int limit = d + src.getShape()[0] * stride;
            for (; d != limit; d += stride) {
                Pointer srcItem = src.getPointer(s++);
                System.arraycopy(srcItem.storage, srcItem.offset, storage, d, itemsize);
            }
        }

    }

    @Override
    public synchronized PyBuffer getBuffer(int flags) {
        if (exports > 0) {
            // Always safe to re-export if the current count is not zero
            return getBufferAgain(flags);
        } else {
            // exports==0 so refuse
            throw bufferReleased("getBuffer");
        }
    }

    /**
     * Allow an exporter to re-use a BaseBytes even if it has been "finally" released. Many
     * sub-classes of <code>BaseBytes</code> can be re-used even after a final release by consumers,
     * simply by incrementing the <code>exports</code> count again: the navigation arrays and the
     * buffer view of the exporter's state all remain valid. We do not let consumers do this through
     * the {@link PyBuffer} interface: from their perspective, calling {@link PyBuffer#release()}
     * should mean the end of their access, although we can't stop them holding a reference to the
     * PyBuffer. Only the exporting object, which handles the implementation type is trusted to know
     * when re-use is safe.
     * <p>
     * An exporter will use this method as part of its implementation of
     * {@link BufferProtocol#getBuffer(int)}. On return from that, the buffer <i>and the exporting
     * object</i> must then be in effectively the same state as if the buffer had just been
     * constructed by that method. Exporters that destroy related resources on final release of
     * their buffer (by overriding {@link #releaseAction()}), or permit themselves structural change
     * invalidating the buffer, must either reconstruct the missing resources or avoid
     * <code>getBufferAgain</code>.
     */
    public synchronized BaseBuffer getBufferAgain(int flags) {
        // If only the request flags are correct for this type, we can re-use this buffer
        checkRequestFlags(flags);
        // Count another consumer of this
        exports += 1;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When the final matching release occurs (that is the number of <code>release</code> calls
     * equals the number of <code>getBuffer</code> calls), the implementation here calls
     * {@link #releaseAction()}, which the implementer of a specific buffer type should override if
     * it needs specific actions to take place.
     */
    @Override
    public void release() {
        if (--exports == 0) {
            // This is a final release.
            releaseAction();
        } else if (exports < 0) {
            // Buffer already had 0 exports. (Put this right, in passing.)
            exports = 0;
            throw bufferReleased("release");
        }
    }

    @Override
    public void close() {
        release();
    }

    @Override
    public boolean isReleased() {
        return exports <= 0;
    }

    @Override
    public PyBuffer getBufferSlice(int flags, int start, int length) {
        return getBufferSlice(flags, start, length, 1);
    }

    // Let the sub-class implement
    // @Override public PyBuffer getBufferSlice(int flags, int start, int length, int stride) {}

    @Override
    public ByteBuffer getNIOByteBuffer() {
        // Determine the limit of the buffer just beyond the last item.
        int length = calcGreatestIndex() + getItemsize() - index0;
        ByteBuffer b = ByteBuffer.wrap(storage, index0, length);
        // Return as read-only if it is.
        return isReadonly() ? b.asReadOnlyBuffer() : b;
    }

    @Override
    public Pointer getBuf() {
        return new Pointer(storage, index0);
    }

    @Override
    public Pointer getPointer(int index) throws IndexOutOfBoundsException {
        return new Pointer(storage, calcIndex(index));
    }

    @Override
    public Pointer getPointer(int... indices) throws IndexOutOfBoundsException {
        return new Pointer(storage, calcIndex(indices));
    }

    @Override
    public int[] getStrides() {
        return strides;
    }

    @Override
    public int[] getSuboffsets() {
        // No actual 'suboffsets' member until a sub-class needs it
        return null;
    }

    @Override
    public boolean isContiguous(char order) {
        // Correct for one-dimensional buffers
        return true;
    }

    @Override
    public String getFormat() {
        // Avoid having to have an actual 'format' member
        return "B";
    }

    @Override
    public int getItemsize() {
        // Avoid having to have an actual 'itemsize' member
        return 1;
    }

    /**
     * This method will be called when the number of calls to {@link #release()} on this buffer is
     * equal to the number of calls to {@link PyBuffer#getBuffer(int)} and to
     * {@link BufferProtocol#getBuffer(int)} that returned this buffer. The default implementation
     * does nothing. Override this method to add release behaviour specific to an exporter. A common
     * convention is to do this within the definition of {@link BufferProtocol#getBuffer(int)}
     * within the exporting class, where a nested class is ultimately defined.
     */
    protected void releaseAction() {}

    /**
     * Some <code>PyBuffer</code>s, those created by slicing a <code>PyBuffer</code> are related to
     * a root <code>PyBuffer</code>. During creation of such a slice, we need to supply a value for
     * this root. If the present object is not itself a slice, this is root is the object itself; if
     * the buffer is already a slice, it is the root it was given at creation time. Often this is
     * the only difference between a slice-view and a directly-exported buffer. Override this method
     * in slices to return the root buffer of the slice.
     *
     * @return this buffer (or the root buffer if this is a sliced view)
     */
    protected PyBuffer getRoot() {
        return this;
    }

    /**
     * Check the number of indices (but not their values), raising a Python BufferError if this does
     * not match the number of dimensions. This is a helper for N-dimensional arrays.
     *
     * @param indices into the buffer (to test)
     * @return number of dimensions
     * @throws PyException (BufferError) if wrong number of indices
     */
    int checkDimension(int[] indices) throws PyException {
        int n = indices.length;
        checkDimension(n);
        return n;
    }

    /**
     * Check that the number offered is in fact the number of dimensions in this buffer, raising a
     * Python BufferError if this does not match the number of dimensions. This is a helper for
     * N-dimensional arrays.
     *
     * @param n number of dimensions being assumed by caller
     * @throws PyException (BufferError) if wrong number of indices
     */
    void checkDimension(int n) throws PyException {
        int ndim = getNdim();
        if (n != ndim) {
            String fmt = "buffer with %d dimension%s accessed as having %d dimension%s";
            String msg = String.format(fmt, ndim, ndim == 1 ? "" : "s", n, n, n == 1 ? "" : "s");
            throw Py.BufferError(msg);
        }
    }

    /**
     * The toString() method of a buffer reproduces the values in the buffer (as unsigned integers)
     * as the character codes of a <code>String</code>.
     */
    @Override
    public String toString() {
        int n = getLen();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.appendCodePoint(intAt(i));
        }
        return sb.toString();
    }

    /**
     * General purpose method to construct an exception to throw according to the syndrome.
     *
     * @param syndrome of the mis-match between buffer and requested features
     * @return PyException (BufferError) specifying the mis-match
     */
    private static PyException bufferErrorFromSyndrome(int syndrome) {

        if ((syndrome & ND) != 0) {
            return bufferRequires("shape array");
        } else if ((syndrome & WRITABLE) != 0) {
            return bufferIsNot("writable");
        } else if ((syndrome & AS_ARRAY) != 0) {
            return bufferIsNot("accessible as a Java array");
        } else if ((syndrome & C_CONTIGUOUS) != 0) {
            return bufferIsNot("C-contiguous");
        } else if ((syndrome & F_CONTIGUOUS) != 0) {
            return bufferIsNot("Fortran-contiguous");
        } else if ((syndrome & ANY_CONTIGUOUS) != 0) {
            return bufferIsNot("contiguous");
        } else if ((syndrome & STRIDES) != 0) {
            return bufferRequires("strides array");
        } else if ((syndrome & INDIRECT) != 0) {
            return bufferRequires("suboffsets array");
        } else {
            // Catch-all error (never in practice if this method is complete)
            return bufferIsNot("capable of matching request");
        }
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>TypeError("cannot modify read-only memory")</code>.
     *
     * @return the error as a PyException
     */
    protected static PyException notWritable() {
        return Py.TypeError("cannot modify read-only memory");
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("underlying buffer is not {property}")</code>.
     *
     * @param property
     * @return the error as a PyException
     */
    protected static PyException bufferIsNot(String property) {
        return Py.BufferError("underlying buffer is not " + property);
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>ValueError("buffer ... different structures")</code>.
     *
     * @return the error as a PyException
     */
    protected static PyException differentStructure() {
        return Py.ValueError("buffer assignment: lvalue and rvalue have different structures");
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("buffer structure requires consumer to use {feature}")</code>.
     *
     * @param feature
     * @return the error as a PyException
     */
    protected static PyException bufferRequires(String feature) {
        return Py.BufferError("buffer structure requires consumer to use " + feature);
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("{operation} operation forbidden on released buffer object")</code>.
     *
     * @param operation name of operation or null
     * @return the error as a PyException
     */
    protected static PyException bufferReleased(String operation) {
        String op = (operation == null) ? "" : operation + " ";
        return Py.BufferError(op + "operation forbidden on released buffer object");
    }

}
