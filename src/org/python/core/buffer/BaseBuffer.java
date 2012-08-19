package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyByteArray;
import org.python.core.PyException;

/**
 * Base implementation of the Buffer API providing default method implementations appropriate to
 * read-only buffers of bytes in one dimension (mainly), and access to the navigational arrays.
 * There are methods for expressing the valid buffer request flags and one for checking an actual
 * request against them. All methods for write access raise a Buffer error, so readonly buffers can
 * simply omit to implement them.
 * <p>
 * This base implementation raises a read-only exception for those methods specified to store data
 * in the buffer, and {@link #isReadonly()} returns <code>true</code>. Writable types must override
 * this implementation. The implementors of simple buffers will find it more efficient to override
 * methods to which performance might be sensitive with a calculation specific to their actual type.
 * <p>
 * At the time of writing, only the SIMPLE organisation (one-dimensional, of item size one) is used
 * in the Jython core.
 */
public abstract class BaseBuffer implements PyBuffer {

    /**
     * The object from which this buffer export must be released (see {@link PyBuffer#release()}).
     * This is normally the original exporter of this buffer and the owner of the underlying
     * storage. Exceptions to this occur when some other object is managing release (this is the
     * case when a <code>memoryview</code> has provided the buffer), and when disposal can safely be
     * left to the Java garbage collector (local temporaries and perhaps exports from immutable
     * objects).
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
     * Reference to a structure that wraps the underlying storage that the exporter is sharing with
     * the consumer.
     */
    protected BufferPointer buf;
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
     * downside is that we have to provide methods for setting and getting the actual flags in terms
     * a client might expect them to be expressed. We can recover the original <code>F</code> since:
     *
     * <pre>
     * N G + N'G' = F
     * </pre>
     */
    private int gFeatureFlags = ~NAVIGATION; // featureFlags = 0

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

    /**
     * Provide an instance of BaseBuffer or a sub-class meeting the consumer's expectations as
     * expressed in the flags argument.
     *
     * @param exporter the exporting object
     */
    protected BaseBuffer(BufferProtocol exporter) {
        // Exporting object (is allowed to be null?)
        this.obj = exporter;
    }

    @Override
    public boolean isReadonly() {
        // Default position is read only: mutable buffers must override
        return true;
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

    @Override
    public int getLen() {
        // Correct if contiguous. Override if strided or indirect with itemsize*product(shape).
        // Also override if buf==null !
        return buf.size;
    }

    // Let the sub-class implement:
    // @Override public byte byteAt(int index) throws IndexOutOfBoundsException {}

    @Override
    public int intAt(int index) throws IndexOutOfBoundsException {
        return 0xff & byteAt(index);
    }

    @Override
    public void storeAt(byte value, int index) throws IndexOutOfBoundsException, PyException {
        throw notWritable();
    }

    // Let the sub-class implement:
    // @Override public byte byteAt(int... indices) throws IndexOutOfBoundsException {}

    @Override
    public int intAt(int... indices) throws IndexOutOfBoundsException {
        return 0xff & byteAt(indices);
    }

    @Override
    public void storeAt(byte value, int... indices) throws IndexOutOfBoundsException, PyException {
        throw notWritable();
    }

    @Override
    public void copyTo(byte[] dest, int destPos) throws IndexOutOfBoundsException {
        // Correct for contiguous arrays (if destination expects same F or C contiguity)
        copyTo(0, dest, destPos, getLen());
    }

    // Let the sub-class implement:
    // @Override public void copyTo(int srcIndex, byte[] dest, int destPos, int length)
    // throws IndexOutOfBoundsException {}

    @Override
    public void copyFrom(byte[] src, int srcPos, int destIndex, int length)
            throws IndexOutOfBoundsException, PyException {
        throw notWritable();
    }

    /**
     * {@inheritDoc}
     * <p>
     * It is possible to call <code>getBuffer</code> on a buffer that has been "finally" released,
     * and it is allowable that the buffer implementation should still return itself as the result,
     * simply incrementing the getBuffer count, thus making it live. In fact, this is what the
     * <code>BaseBuffer</code> implementation does. On return, it <i>and the exporting object</i>
     * must then be in effectively the same state as if the buffer had just been constructed by the
     * exporter's <code>getBuffer</code> method. In many simple cases this is perfectly
     * satisfactory.
     * <p>
     * Exporters that destroy related resources on final release of their buffer (by overriding
     * {@link #releaseAction()}), or permit themeselves structural change invalidating the buffer,
     * must either reconstruct the missing resources or return a fresh buffer when
     * <code>PyBuffer.getBuffer</code> is called on their export. Resurrecting a buffer, when it
     * needs exporter action, may be implemented by specialising a library <code>PyBuffer</code>
     * implementation like this:
     *
     * <pre>
     * public synchronized PyBuffer getBuffer(int flags) {
     *     if (isReleased()) {
     *         // ... exporter actions necessary to make the buffer valid again
     *     }
     *     return super.getBuffer(flags);
     * }
     * </pre>
     *
     * Re-use can be prohibited by overriding <code>PyBuffer.getBuffer</code> so that a released
     * buffer gets a fresh buffer from the exporter. This is the approach taken in
     * {@link PyByteArray#getBuffer(int)}.
     *
     * <pre>
     * public synchronized PyBuffer getBuffer(int flags) {
     *     if (isReleased()) {
     *         // Force creation of a new buffer
     *         return obj.getBuffer(flags);
     *         // Or other exporter actions necessary and return this
     *     } else {
     *         return super.getBuffer(flags);
     *     }
     * }
     * </pre>
     *
     * Take care to avoid indefinite recursion if the exporter's <code>getBuffer</code> depends in
     * turn on <code>PyBuffer.getBuffer</code>.
     * <p>
     * Simply overriding {@link #releaseAction()} does not in itself make it necessary to override
     * <code>PyBuffer.getBuffer</code>, since <code>isReleased()</code> may do all that is needed.
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {
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
            throw Py.BufferError("attempt to release already-released buffer");
        }
    }

    @Override
    public boolean isReleased() {
        return exports <= 0;
    }

    @Override
    public BufferPointer getBuf() {
        return buf;
    }

    // Let the sub-class implement:
    // @Override public BufferPointer getPointer(int index) { return null; }
    // @Override public BufferPointer getPointer(int... indices) { return null; }

    @Override
    public int[] getStrides() {
        return strides;
    }

    @Override
    public int[] getSuboffsets() {
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
        // return ((featureFlags & FORMAT) == 0) ? null : "B";
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
     * Check the number of indices (but not their values), raising a Python BufferError if this does
     * not match the number of dimensions. This is a helper for N-dimensional arrays.
     *
     * @param indices into the buffer (to test)
     * @return number of dimensions
     * @throws PyException (BufferError) if wrong number of indices
     */
    final int checkDimension(int[] indices) throws PyException {
        int ndim = shape.length;
        if (indices.length != ndim) {
            if (indices.length < ndim) {
                throw Py.BufferError("too few indices supplied");
            } else {
                throw Py.BufferError("too many indices supplied");
            }
        }
        return ndim;
    }

    /**
     * General purpose method to construct an exception to throw according to the syndrome.
     *
     * @param syndrome of the mis-match between buffer and requested features
     * @return PyException (BufferError) specifying the mis-match
     */
    private static PyException bufferErrorFromSyndrome(int syndrome) {

        if ((syndrome & ND) != 0) {
            return bufferRequires("shape");
        } else if ((syndrome & STRIDES) != 0) {
            return bufferRequires("strides");
        } else if ((syndrome & INDIRECT) != 0) {
            return bufferRequires("suboffsets");
        } else if ((syndrome & WRITABLE) != 0) {
            return notWritable();
        } else if ((syndrome & C_CONTIGUOUS) != 0) {
            return bufferIsNot("C-contiguous");
        } else if ((syndrome & F_CONTIGUOUS) != 0) {
            return bufferIsNot("Fortran-contiguous");
        } else if ((syndrome & ANY_CONTIGUOUS) != 0) {
            return bufferIsNot("contiguous");
        } else {
            // Catch-all error (never in practice if this method is complete)
            return bufferIsNot("capable of matching request");
        }
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("underlying buffer is not writable")</code>.
     *
     * @return the error as a PyException
     */
    protected static PyException notWritable() {
        return bufferIsNot("writable");
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
     * <code>BufferError("underlying buffer requires {feature}")</code>.
     *
     * @param feature
     * @return the error as a PyException
     */
    protected static PyException bufferRequires(String feature) {
        return Py.BufferError("underlying buffer requires " + feature);
    }

}
