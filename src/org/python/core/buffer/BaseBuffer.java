package org.python.core.buffer;

import java.nio.ByteBuffer;

import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Base implementation of the Buffer API providing variables and accessors for the navigation
 * arrays, methods for expressing and checking the buffer request flags, methods and mechanism for
 * get-release counting, boilerplate error checks and their associated exceptions, and default
 * implementations of some methods for access to the buffer content. The design aim is to ensure
 * unglamorous common code need only be implemented once.
 * <p>
 * This class leaves undefined the storage mechanism for the bytes (typically <code>byte[]</code> or
 * <code>java.nio.ByteBuffer</code>), while remaining definite that it is an indexable sequence of
 * bytes. A concrete class that extends this one must provide elementary accessors
 * {@link #byteAtImpl(int)}, {@link #storeAtImpl(byte, int)} that abstract this storage, a factory
 * {@link #getNIOByteBufferImpl()} for <code>ByteBuffer</code>s that wrap the storage, and a factory
 * for slices {@link #getBufferSlice(int, int, int, int)}.
 * <p>
 * The sub-class constructor must specify the feature flags (see {@link #BaseBuffer(int)}), set
 * {@link #index0}, {@link #shape} and {@link #strides}, and finally check the client capabilities
 * with {@link #checkRequestFlags(int)}. Sub-classes intended to represent slices of exporters that
 * must count their exports as part of a locking protocol, as does <code>bytearray</code>, must
 * override {@link #getRoot()} so that a buffer view {@link #release()} on a slice, propagates to
 * the buffer view that provided it.
 * <p>
 * Access methods provided here necessarily work with the abstracted {@link #byteAtImpl(int)},
 * {@link #storeAtImpl(byte, int)} interface, but subclasses are able to override them with more
 * efficient versions that employ knowledge of the particular storage type used.
 * <p>
 * This base implementation is writable only if {@link PyBUF#WRITABLE} is in the feature flags
 * passed to the constructor. Otherwise, all methods for write access raise a <code>TypeError</code>
 * and {@link #isReadonly()} returns <code>true</code>. However, a client intending to write should
 * have presented {@link PyBUF#WRITABLE} in its client request flags when getting the buffer, and
 * been prevented by a <code>BufferError</code> exception at that point.
 * <p>
 * At the time of writing, only one-dimensional buffers of item size one are used in the Jython
 * core.
 */
public abstract class BaseBuffer implements PyBuffer {

    /**
     * The object that exported this buffer (or <code>null</code> if the subclass or exporter
     * chooses not to supply a reference).
     */
    protected BufferProtocol obj;

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
     * Absolute byte-index in the storage of <code>item[0]</code>. In one dimension, for a positive
     * <code>stride</code> this is equal to the offset of the first byte used in whatever
     * byte-storage is provided, and for a negative <code>stride</code> it is the first byte of the
     * last item. In an N-dimensional buffer with strides of mixed sign, it could be anywhere in the
     * data.
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
     * mutilated version of the apparent <code>featureFlags</code> in which the non-navigation flags
     * are inverted. The syndrome <code>S</code> of the error is computed as follows. Let
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
     * Construct an instance of <code>BaseBuffer</code> in support of a sub-class, specifying the
     * 'feature flags', or at least a starting set to be adjusted later. Also specify the navigation
     * ( {@link #index0}, {@link #shape}, and {@link #strides}). These 'feature flags' are the
     * features of the buffer exported, not the flags that form the consumer's request. The buffer
     * will be read-only unless {@link PyBUF#WRITABLE} is set. {@link PyBUF#FORMAT} is implicitly
     * added to the feature flags.
     * <p>
     * To complete initialisation, the sub-class normally must create its own wrapped byte-storage,
     * and call {@link #checkRequestFlags(int)} passing the consumer's request flags.
     *
     * @param featureFlags bit pattern that specifies the features allowed
     * @param index0 index into storage of <code>item[0,...,0]</code>
     * @param shape elements in each dimension
     * @param strides between successive elements in each dimension
     */
    protected BaseBuffer(int featureFlags, int index0, int[] shape, int[] strides) {
        setFeatureFlags(featureFlags | FORMAT);
        this.index0 = index0;
        this.shape = shape;
        this.strides = strides;
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
     * Remove features from this buffer expressed using the constants defined in {@link PyBUF},
     * clearing individual flags specified while leaving others already set. Equivalent to
     * <code>setFeatureFlags(~flags & getFeatureFlags())</code>.
     *
     * @param flags to clear within the feature flags
     */
    protected final void removeFeatureFlags(int flags) {
        setFeatureFlags(~flags & getFeatureFlags());
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
     * In a subset of the flags, the consumer specifies the set of navigation arrays (
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
        // WRITABLE is a non-navigation flag, so is inverted in gFeatureFlags
        return (gFeatureFlags & WRITABLE) != 0; // i.e. featureFlags & WRITABLE is false
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

    // XXX Consider making this part of the PyBUF interface
    protected int getSize() {
        final int N = shape.length;
        int size = shape[0];
        for (int k = 1; k < N; k++) {
            size *= shape[k];
        }
        return size;
    }

    @Override
    public int getLen() {
        final int N = shape.length;
        int len = getItemsize();
        for (int k = 0; k < N; k++) {
            len *= shape[k];
        }
        return len;
    }

    @Override
    public final BufferProtocol getObj() {
        return obj;
    }

    /**
     * Retrieve the byte at the given index in the underlying storage treated as a flat sequence of
     * bytes. This byte-index will have been computed from the item index (which may have been
     * multi-dimensional), taking into account {@link #index0}, {@link #shape}, {@link #strides},
     * and the item size. The caller is responsible for validating the original item-index and
     * raising (typically) an <code>IndexOutOfBoundsException</code>. Misuse of this method may
     * still result in unchecked exceptions characteristic of the storage implementation.
     *
     * @param byteIndex byte-index of location to retrieve
     * @return the byte at byteIndex
     */
    abstract protected byte byteAtImpl(int byteIndex) throws IndexOutOfBoundsException;

    /**
     * Store the byte at the given index in the underlying storage treated as a flat sequence of
     * bytes. This byte-index will have been computed from the item index (which may have been
     * multi-dimensional), taking into account {@link #index0}, {@link #shape}, {@link #strides},
     * and the item size. The caller is responsible for validating the original item-index and
     * raising (typically) an <code>IndexOutOfBoundsException</code>. Misuse of this method may
     * still result in unchecked exceptions characteristic of the storage implementation. This
     * method must implement the check for read-only character, raising a <code>BufferError</code>
     * in the case of a violation.
     *
     * @param value to store
     * @param byteIndex byte-index of location to retrieve
     * @throws PyException(BufferError) if this object is read-only.
     */
    abstract protected void storeAtImpl(byte value, int byteIndex)
            throws IndexOutOfBoundsException, PyException;

    /**
     * {@inheritDoc}
     * <p>
     * The <code>BaseBuffer</code> implementation delegates to {@link #byteAtImpl(int)} via
     * <code>byteAtImpl(byteIndex(index))</code>.
     */
    @Override
    public byte byteAt(int index) throws IndexOutOfBoundsException {
        return byteAtImpl(byteIndex(index));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>BaseBuffer</code> implementation delegates to {@link #byteAtImpl(int)} via
     * <code>byteAtImpl(byteIndex(index))</code>, cast unsigned to an <code>int</code>.
     */
    @Override
    public int intAt(int index) throws IndexOutOfBoundsException {
        return 0xff & byteAtImpl(byteIndex(index));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>BaseBuffer</code> implementation delegates to {@link #storeAtImpl(byte, int)} via
     * <code>storeAtImpl(value, byteIndex(index))</code>.
     */
    @Override
    public void storeAt(byte value, int index) throws IndexOutOfBoundsException, PyException {
        storeAtImpl(value, byteIndex(index));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>BaseBuffer</code> implementation delegates to {@link #byteAtImpl(int)} via
     * <code>byteAtImpl(byteIndex(indices))</code>.
     */
    @Override
    public byte byteAt(int... indices) throws IndexOutOfBoundsException {
        return byteAtImpl(byteIndex(indices));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>BaseBuffer</code> implementation delegates to {@link #byteAtImpl(int)} via
     * <code>byteAtImpl(byteIndex(indices))</code>, cast unsigned to an <code>int</code>.
     */
    @Override
    public int intAt(int... indices) throws IndexOutOfBoundsException {
        return 0xff & byteAt(indices);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <code>BaseBuffer</code> implementation delegates to {@link #storeAtImpl(byte, int)} via
     * <code>storeAtImpl(value, byteIndex(indices))</code>.
     */
    @Override
    public void storeAt(byte value, int... indices) throws IndexOutOfBoundsException, PyException {
        storeAtImpl(value, byteIndex(indices));
    }

    /*
     * In this implementation, we throw IndexOutOfBoundsException if index < 0 or > shape[0], but we
     * could rely on the array or ByteBuffer checks when indexing, especially the latter since
     * position is checked against limit.
     */
    @Override
    public int byteIndex(int index) throws IndexOutOfBoundsException {
        // Treat as one-dimensional
        if (index < 0 || index >= shape[0]) {
            throw new IndexOutOfBoundsException();
        }
        return index0 + index * strides[0];
    }

    /*
     * In this implementation, we throw IndexOutOfBoundsException if any index[i] < 0 or > shape[i].
     */
    @Override
    public int byteIndex(int... indices) throws IndexOutOfBoundsException {
        final int N = checkDimension(indices);
        // In general: index0 + sum(k=0,N-1) indices[k]*strides[k]
        int index = index0;
        for (int k = 0; k < N; k++) {
            int ik = indices[k];
            if (ik < 0 || ik >= shape[k]) {
                throw new IndexOutOfBoundsException();
            }
            index += ik * strides[k];
        }
        return index;
    }

    /**
     * Calculate the absolute byte index in the storage array of the last item of the exported data
     * (if we are not using indirection). This is the greatest value attained by
     * {@link #byteIndex(int...)}. The first byte not used will be one <code>itemsize</code> more
     * than the returned value.
     *
     * @return greatest absolute index in storage
     */
    protected int calcGreatestIndex() {
        final int N = shape.length;
        // If all the strides are positive, the maximal value is found from:
        // index = index0 + sum(k=0,N-1) (shape[k]-1)*strides[k]
        // but in general, for any k where strides[k]<=0, the term should be zero.
        int index = index0;
        int[] strides = getStrides();
        for (int k = 0; k < N; k++) {
            int stride = strides[k];
            if (stride > 0) {
                index += (shape[k] - 1) * stride;
            }
        }
        return index;
    }

    /**
     * Calculate the absolute byte index in the storage array of the first item of the exported data
     * (if we are not using indirection). This is the least value attained by
     * {@link #byteIndex(int...)}.
     *
     * @return least absolute index in storage
     */
    protected int calcLeastIndex() {
        final int N = shape.length;
        // If all the strides are positive, the maximal value is just index0,
        // but in general, we must allow strides[k]<=0 for some k:
        // index = index0 + sum(k=0,N-1) (strides[k]<0) ? (shape[k]-1)*strides[k] : 0
        int index = index0;
        int[] strides = getStrides();
        for (int k = 0; k < N; k++) {
            int stride = strides[k];
            if (stride < 0) {
                index += (shape[k] - 1) * stride;
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
        copyTo(0, dest, destPos, getSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride, but is unable to optimise access to sequential bytes.
     */
    @Override
    public void copyTo(int srcIndex, byte[] dest, int destPos, int count)
            throws IndexOutOfBoundsException, PyException {

        checkDimension(1);

        int itemsize = getItemsize();
        int s = srcIndex, d = destPos;

        if (itemsize == 1) {
            // Single byte items
            for (int i = 0; i < count; i++) {
                dest[d++] = byteAt(s++);
            }
        } else {
            // Multi-byte items
            for (int i = 0; i < count; i++) {
                int p = byteIndex(s++);
                for (int j = 0; j < itemsize; j++) {
                    dest[d++] = byteAtImpl(p + j);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride, but is unable to optimise access to sequential bytes.
     */
    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int count)
            throws IndexOutOfBoundsException, PyException {

        checkDimension(1);
        checkWritable();

        int itemsize = getItemsize();
        int d = destIndex, s = srcPos;

        if (itemsize == 1) {
            // Single byte items
            for (int i = 0; i < count; i++) {
                storeAt(src[s++], d++);
            }
        } else {
            // Multi-byte items
            for (int i = 0; i < count; i++) {
                int p = byteIndex(d++);
                for (int j = 0; j < itemsize; j++) {
                    storeAtImpl(src[s++], p++);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation in <code>BaseBuffer</code> deals with the general one-dimensional
     * case of arbitrary item size and stride, but is unable to optimise access to sequential bytes.
     */
    @Override
    public void copyFrom(PyBuffer src) throws IndexOutOfBoundsException, PyException {

        checkDimension(1);
        checkWritable();

        int itemsize = getItemsize();
        int count = getSize();
        int byteLen = src.getLen();

        // Block operation if different item or overall size (permit reshape)
        if (src.getItemsize() != itemsize || byteLen != count * itemsize) {
            throw differentStructure();
        }

        /*
         * It is not possible in general to know that this and src do not share storage. There is
         * always a risk of incorrect results if we do not go via an intermediate byte array.
         * Sub-classes may be able to avoid this.
         */
        byte[] t = new byte[byteLen];
        src.copyTo(t, 0);
        this.copyFrom(t, 0, 0, count);
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
     * Allow an exporter to re-use this object again even if it has been "finally" released. Many
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
     * <p>
     * Note that, when this is a sliced view obtained from another <code>PyBuffer</code> the
     * implementation in <code>BaseBuffer</code> automatically sends one <code>release()</code>
     * Sub-classes should not propagate the release themselves when overriding
     * {@link #releaseAction()}.
     */
    @Override
    public void release() {
        if (--exports == 0) {
            // This is a final release.
            releaseAction();
            // We have to release the root too if we are not a root.
            PyBuffer root = getRoot();
            if (root != this) {
                root.release();
            }
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
    public PyBuffer getBufferSlice(int flags, int start, int count) {
        return getBufferSlice(flags, start, count, 1);
    }

    // Let the sub-class implement
    // @Override public PyBuffer getBufferSlice(int flags, int start, int count, int stride) {}

    /**
     * Create a new <code>java.nio.ByteBuffer</code> on the underlying storage, such that
     * positioning this buffer to a particular byte using {@link #byteIndex(int)} or
     * {@link #byteIndex(int[])} positions it at the first byte of the item so indexed.
     */
    abstract protected ByteBuffer getNIOByteBufferImpl();

    @Override
    public ByteBuffer getNIOByteBuffer() {
        // The buffer spans the whole storage
        ByteBuffer b = getNIOByteBufferImpl();
        // For the one-dimensional contiguous case it makes sense to set the limit:
        if (shape.length == 1 && isContiguous('A')) {
            int stride = strides[0];
            if (getItemsize() == stride) {
                b.limit(index0 + shape[0] * stride);
            }
        }
        // The buffer is positioned at item[0]
        b.position(index0);
        return b;
    }

    @Override
    public boolean hasArray() {
        // AS_ARRAY is a non-navigation flag, so is inverted in gFeatureFlags
        return (gFeatureFlags & AS_ARRAY) == 0; // i.e. featureFlags & AS_ARRAY is true
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getBuf() {
        checkHasArray();
        return new Pointer(getNIOByteBuffer().array(), index0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int index) throws IndexOutOfBoundsException {
        Pointer p = getBuf();
        p.offset = byteIndex(index);
        return p;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pointer getPointer(int... indices) throws IndexOutOfBoundsException {
        Pointer p = getBuf();
        p.offset = byteIndex(indices);
        return p;
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

    private boolean isCContiguous() {
        /*
         * If we were to compute the strides array for a C-contiguous array, the last stride would
         * equal the item size, and generally stride[k-1] = shape[k]*stride[k]. This is the basis of
         * the test. However, note that for any k where shape[k]==1 there is no "next sub-array" and
         * no discontiguity.
         */
        final int N = shape.length;
        /*
         * size is the stride in bytes-index from item[i0,i1,...,ik,0,...,0] to
         * item[i0,i1,...,ik+1,0,...,0]. Start the iteration at the largest k. An increment of one
         * in the last index makes a stride of the item size.
         */
        int size = getItemsize();
        for (int k = N - 1; k >= 0; k--) {
            int nk = shape[k];
            if (nk > 1) {
                if (strides[k] != size) {
                    return false;
                }
                size *= nk;
            }
        }
        return true;
    }

    private boolean isFortranContiguous() {
        /*
         * If we were to compute the strides array for a Fortran-contiguous array, the first stride
         * would equal the item size, and generally stride[k+1] = shape[k]*stride[k]. This is the
         * basis of the test. However, note that for any k where shape[k]==1 there is no
         * "next sub-array" and no discontiguity.
         */
        final int N = shape.length;
        /*
         * size is the stride in bytes-index from item[0,...,0,ik,0,...,0] to
         * item[0,...,0,ik+1,0,...,0]. Start the iteration at k=0. An increment of one in the first
         * index makes a stride of the item size.
         */
        int size = getItemsize();
        for (int k = 0; k < N; k++) {
            int nk = shape[k];
            if (nk > 1) {
                if (strides[k] != size) {
                    return false;
                }
                size *= nk;
            }
        }
        return true;
    }

    @Override
    public boolean isContiguous(char order) {
        if (getSuboffsets() != null) {
            return false;
        }
        switch (order) {
            case 'C':
                return isCContiguous();
            case 'F':
                return isFortranContiguous();
            case 'A':
                return isCContiguous() || isFortranContiguous();
            default:
                return false;
        }
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
     * Some <code>PyBuffer</code>s, those created by slicing a <code>PyBuffer</code>, are related to
     * a root <code>PyBuffer</code>. During creation of such a slice, we need to supply a value for
     * this root. If the present object is not itself a slice, this root is the object itself; if
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
     * Check that the buffer is writable.
     *
     * @throws PyException (TypeError) if not
     */
    protected void checkWritable() throws PyException {
        if (isReadonly()) {
            throw notWritable();
        }
    }

    /**
     * Check that the buffer is backed by an array the client can access as byte[].
     *
     * @throws PyException (BufferError) if not
     */
    protected void checkHasArray() throws PyException {
        if (!hasArray()) {
            throw bufferIsNot("accessible as a Java array");
        }
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
