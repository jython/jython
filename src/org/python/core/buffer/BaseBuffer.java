package org.python.core.buffer;

import org.python.core.BufferPointer;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyException;

/**
 * Base implementation of the Buffer API for implementations to extend. The default implementation
 * provides some mechanisms for checking the consumer's capabilities against those stated as
 * necessary by the exporter. Default implementations of methods are provided for the standard array
 * organisations. The implementors of simple buffers will find it more efficient to override methods
 * to which performance might be sensitive with a calculation specific to their actual type.
 * <p>
 * The default implementation raises a read-only exception for those methods that store data in the
 * buffer, and {@link #isReadonly()} returns <code>true</code>. Writable types must override this
 * implementation. Default implementations of other methods are generally oriented towards
 * contiguous N-dimensional arrays.
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
     * filled (difference from CPython).
     */
    protected int[] shape;
    /**
     * Step sizes in the underlying buffer essential to correct translation of an index (or indices)
     * into an index into the storage. This reference will be <code>null</code> if not needed for
     * the storage organisation, and not requested by the consumer in <code>flags</code>. If it is
     * either necessary for the buffer navigation, or requested by the consumer in flags, the
     * <code>strides</code> array must be correctly filled to at least the length of the
     * <code>shape</code> array.
     */
    protected int[] strides;
    /**
     * Reference to a structure that wraps the underlying storage that the exporter is sharing with
     * the consumer.
     */
    protected BufferPointer buf;
    /**
     * Bit pattern using the constants defined in {@link PyBUF} that records the actual capabilities
     * this buffer offers. See {@link #assignCapabilityFlags(int, int, int, int)}.
     */
    protected int capabilityFlags;

    /**
     * The result of the operation is to set the {@link #capabilityFlags} according to the
     * capabilities this instance should support. This method is normally called in the constructor
     * of each particular sub-class of <code>BaseBuffer</code>, passing in a <code>flags</code>
     * argument that originated in the consumer's call to {@link BufferProtocol#getBuffer(int)}.
     * <p>
     * The consumer supplies as a set of <code>flags</code>, using constants from {@link PyBUF}, the
     * capabilities that it expects from the buffer. These include a statement of which navigational
     * arrays it will use ( <code>shape</code>, <code>strides</code>, and <code>suboffsets</code>),
     * whether it wants the <code>format</code> string set so it describes the item type or left
     * null, and whether it expects the buffer to be writable. The consumer flags are taken by this
     * method both as a statement of needs to be met by the buffer, and as a statement of
     * capabilities in the consumer to navigate different buffers.
     * <p>
     * In its call to this method, the exporter specifies the capabilities it requires the consumer
     * to have (and indicate by asking for them in <code>flags</code>) in order to navigate the
     * buffer successfully. For example, if the buffer is a strided array, the consumer must specify
     * that it expects the <code>strides</code> array. Otherwise the method concludes the consumer
     * is not capable of the navigation required. Capabilities specified in the
     * <code>requiredFlags</code> must appear in the consumer's <code>flags</code> request. If any
     * don't, a Python <code>BufferError</code> will be raised. If there is no error these flags
     * will be set in <code>capabilityFlags</code> as required of the buffer.
     * <p>
     * The exporter specifies some capabilities it <i>allows</i> the consumer to request, such as
     * the <code>format</code> string. Depending on the type of exporter, the navigational arrays (
     * <code>shape</code>, <code>strides</code>, and <code>suboffsets</code>) may also be allowed
     * rather than required. Capabilities specified in the <code>allowedFlags</code>, if they also
     * appear in the consumer's <code>flags</code>, will be set in <code>capabilityFlags</code>.
     * <p>
     * The exporter specifies some capabilities that will be supplied whether requested or not. For
     * example (and it might be the only one) this is used only to express that an unstrided,
     * one-dimensional array is <code>C_CONTIGUOUS</code>, <code>F_CONTIGUOUS</code>, and
     * <code>ANY_CONTIGUOUS</code>, all at once. Capabilities specified in the
     * <code>impliedFlags</code>, will be set in <code>capabilityFlags</code> whether in the
     * consumer's <code>flags</code> or not.
     * <p>
     * Capabilities specified in the consumer's <code>flags</code> request, if they do not appear in
     * the exporter's <code>requiredFlags</code> <code>allowedFlags</code> or
     * <code>impliedFlags</code>, will cause a Python <code>BufferError</code>.
     * <p>
     * Note that this method cannot actually set the <code>shape</code>, <code>strides</code> and
     * <code>suboffsets</code> properties: the implementation of the specific buffer type must do
     * that based on the <code>capabilityFlags</code>. This forms a partial counterpart to CPython
     * <code>PyBuffer_FillInfo()</code> but it is not specific to the simple type of buffer, and
     * covers the flag processing of all buffer types. This is complex (in CPython) and the Jython
     * approach attempts to be compatible yet comprehensible.
     */
    protected void assignCapabilityFlags(int flags, int requiredFlags, int allowedFlags,
            int impliedFlags) {

        // Ensure what may be requested includes what must be and what comes unasked
        allowedFlags = allowedFlags | requiredFlags | impliedFlags;

        // Look for request flags (other than buffer organisation) outside what is allowed
        int syndrome = flags & ~(allowedFlags | ORGANISATION);

        if (syndrome != 0) {
            // Some flag was set that is neither required nor allowed
            if ((syndrome & WRITABLE) != 0) {
                throw notWritable();
            } else if ((syndrome & C_CONTIGUOUS) != 0) {
                throw bufferIsNot("C-contiguous");
            } else if ((syndrome & F_CONTIGUOUS) != 0) {
                throw bufferIsNot("Fortran-contiguous");
            } else if ((syndrome & ANY_CONTIGUOUS) != 0) {
                throw bufferIsNot("contiguous");
            } else {
                // Catch-all error (never in practice?)
                throw bufferIsNot("capable of matching request");
            }

        } else if ((flags & requiredFlags) != requiredFlags) {
            // This buffer needs more capability to navigate than the consumer has requested
            if ((flags & ND) != ND) {
                throw bufferRequires("shape");
            } else if ((flags & STRIDES) != STRIDES) {
                throw bufferRequires("strides");
            } else if ((flags & INDIRECT) != INDIRECT) {
                throw bufferRequires("suboffsets");
            } else {
                // Catch-all error
                throw bufferRequires("feature consumer lacks");
            }

        } else {
            // These flags control returns from (default) getShape etc..
            capabilityFlags = (flags & allowedFlags) | impliedFlags;
            // Note that shape and strides are still to be initialised
        }

        /*
         * Caller must responds to the requested/required capabilities with shape and strides arrays
         * suited to the actual type of buffer.
         */
    }

    /**
     * Provide an instance of BaseBuffer or a sub-class meeting the consumer's expectations as
     * expressed in the flags argument. Compare CPython:
     *
     * <pre>
     * int PyBuffer_FillInfo(Py_buffer *view, PyObject *exporter,
     *                       void *buf, Py_ssize_t len,
     *                       int readonly, int flags)
     * </pre>
     *
     * @param exporter the exporting object
     * @param buf descriptor for the exported buffer itself
     */
    protected BaseBuffer(BufferProtocol exporter, BufferPointer buf) {
        // Exporting object (is allowed to be null)
        this.obj = exporter;
        // Exported data (not normally allowed to be null)
        this.buf = buf;
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
     * The implementation here calls {@link #releaseAction()}, which the implementer of a specific
     * buffer type should override with the necessary actions to release the buffer from the
     * exporter. It is not an error to call this method more than once (difference from CPython), or
     * on a temporary buffer that needs no release action. If not released explicitly, it will be
     * called during object finalisation (before garbage collection) of the buffer object.
     */
    @Override
    public final void release() {
        if (obj != null) {
            releaseAction();
        }
        obj = null;
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
        return true;
    }

    @Override
    public String getFormat() {
        // Avoid having to have an actual 'format' member
        return ((capabilityFlags & FORMAT) == 0) ? null : "B";
    }

    @Override
    public int getItemsize() {
        // Avoid having to have an actual 'itemsize' member
        return 1;
    }

    /**
     * Ensure buffer, if not released sooner, is released from the exporter during object
     * finalisation (before garbage collection) of the buffer object.
     */
    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    /**
     * This method will be called when the consumer calls {@link #release()} (to be precise, only on
     * the first call). The default implementation does nothing. Override this method to add release
     * behaviour specific to exporter. A common convention is to do this within the definition of
     * {@link BufferProtocol#getBuffer(int)} within the exporting class, where a nested class is
     * finally defined.
     */
    protected void releaseAction() {}

    /**
     * Check the number of indices (but not their values), raising a Python BufferError if this does
     * not match the number of dimensions.
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
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("underlying buffer is not writable")</code>.
     *
     * @return the error as a PyException
     */
    protected PyException notWritable() {
        return bufferIsNot("writable");
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("underlying buffer is not {property}")</code>.
     *
     * @param property
     * @return the error as a PyException
     */
    protected PyException bufferIsNot(String property) {
        return Py.BufferError("underlying buffer is not " + property);
    }

    /**
     * Convenience method to create (for the caller to throw) a
     * <code>BufferError("underlying buffer requires {feature}")</code>.
     *
     * @param feature
     * @return the error as a PyException
     */
    protected PyException bufferRequires(String feature) {
        return Py.BufferError("underlying buffer requires " + feature);
    }

}
