// Copyright (c) 2013 Jython Developers
package org.python.core;

import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleStringBuffer;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Implementation of the Python <code>buffer</code> type. <code>buffer</code> is being superseded in
 * Python 2.7 by <code>memoryview</code>, and is provided here to support legacy Python code. Use
 * <code>memoryview</code> if you can.
 * <p>
 * <code>buffer</code> and <code>memoryview</code> both wrap the <em>same</em> Jython buffer API:
 * that designed for <code>memoryview</code>. In CPython, a new C API (which Jython's resembles) was
 * introduced with <code>memoryview</code>. Because of this, <code>buffer</code> and
 * <code>memoryview</code> may be supplied as arguments in the same places, and will accept as
 * arguments the same (one-dimensional byte-array) types. Their behaviour differs as detailed in the
 * documentation.
 */
@Untraversable
@ExposedType(name = "buffer", doc = BuiltinDocs.buffer_doc, base = PyObject.class,
        isBaseType = false)
public class Py2kBuffer extends PySequence implements BufferProtocol {

    public static final PyType TYPE = PyType.fromClass(Py2kBuffer.class);

    /** The underlying object on which the buffer was created. */
    private final BufferProtocol object;
    /** The offset (in bytes) into the offered object at which the buffer starts. */
    private final int offset;
    /** Number of bytes to include in the buffer (or -1 for all available). */
    private final int size;

    /**
     * Construct a Py2kBuffer from an object supporting the {@link BufferProtocol}. The
     * <code>buffer</code> takes no lease on the <code>PyBuffer</code> at present, but for each
     * action performed obtains a new one and releases it. (Major difference from
     * <code>memoryview</code>.) Note that when <code>size=-1</code> is given, the buffer reflects
     * the changing size of the underlying object.
     * 
     * @param object the object on which this is to be a buffer.
     * @param offset into the array exposed by the object (0 for start).
     * @param size of the slice or -1 for all of the object.
     */
    public Py2kBuffer(BufferProtocol object, int offset, int size) {
        super(TYPE);

        if (object instanceof Py2kBuffer) {
            // Special behaviour when the source object is another of our kind.
            Py2kBuffer source = (Py2kBuffer)object;
            offset = source.offset + offset;
            if (source.size >= 0) {
                // The source imposes a size limit, or rather it imposes an end
                int end = source.offset + source.size;
                if (size < 0 || offset + size > end) {
                    // We are asked for unlimited/excessive length, but must impose source end.
                    size = end - offset;
                }
            }
            // This will be a Py2kBuffer with the derived offset and size on the same object.
            object = source.object;
        }
        this.object = object;
        this.offset = offset;
        this.size = size;
    }

    /**
     * Every action on the <code>buffer</code> must obtain a new {@link PyBuffer} reflecting (this
     * buffer's slice of) the contents of the backing object.
     * 
     * @return a <code>PyBuffer</code> onto the specified slice.
     */
    private PyBuffer getBuffer() {
        /*
         * Ask for a simple one-dimensional byte view (not requiring strides, indirect, etc.) from
         * the object, as we cannot deal with other navigation. Ask for read access. If the object
         * is writable, the PyBuffer will be writable, but we won't write to it.
         */
        final int flags = PyBUF.SIMPLE;
        PyBuffer buf = object.getBuffer(flags);

        // This may already be what we need, or this buffer may be a sub-range of the object
        if (offset > 0 || size >= 0) {
            /*
             * It's a sub-range so we have to construct a slice buffer on the first buffer. Take
             * care that the bounds of the slice are within the object, which may have changed size
             * since the buffer was created.
             */
            PyBuffer first = buf;
            int start = offset;
            int length = first.getLen() - start;

            if (length <= 0) {
                // Range now lies outside object: zero length slice
                start = length = 0;
            } else if (size >= 0 && size < length) {
                // A size less than the available bytes was specified (size==-1 => all of them)
                length = size;
            }

            // Now offset and length specify a feasible slice
            buf = first.getBufferSlice(flags, offset, length);

            // We should release our first lease because the slice buf holds one.
            // That lease will be released when buf is released.
            first.release();
        }
        return buf;
    }

    /**
     * Return a {@link PyObject} bearing the interface {@link BufferProtocol} and equivalent to the
     * argument, or return <code>null</code>. This is a helper function to those methods that accept
     * a range of types supporting the buffer API. Normally the return is exactly the argument,
     * except in the case of a {@link PyUnicode}, which will be converted to a {@link PyString}
     * according to Py2k semantics, equivalent to a UTF16BE encoding to bytes (for Py2k
     * compatibility).
     * 
     * @param obj the object to access.
     * @return <code>PyObject</code> supporting {@link BufferProtocol}, if not <code>null</code>.
     */
    private static BufferProtocol asBufferableOrNull(PyObject obj) {

        if (obj instanceof PyUnicode) {
            /*
             * Jython unicode does not support the buffer protocol (so that you can't take a
             * memoryview of one). But to be compatible with CPython we allow buffer(unicode) to
             * export two-byte UTF-16. Fortunately, a buffer is read-only, so we can use a copy.
             */
            String bytes = codecs.encode((PyString)obj, "UTF-16BE", "replace");
            return new PyString(bytes);

        } else if (obj instanceof BufferProtocol) {
            // That will do directly
            return (BufferProtocol)obj;

        } else {
            // We don't know how to give this value the buffer API.
            return null;
        }
    }

    /** Names of arguments in the constructor (for ArgParser). */
    private static String[] paramNames = {"object", "offset", "size"};

    @ExposedNew
    static PyObject buffer_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
            String[] keywords) {

        // Use the ArgParser to access the arguments
        ArgParser ap = new ArgParser("buffer", args, keywords, paramNames, 1);
        PyObject obj = ap.getPyObject(0);
        int offset = ap.getIndex(1, 0);
        int size = ap.getInt(2, -1);

        // Get the object as a BufferProtocol if possible
        BufferProtocol object = asBufferableOrNull(obj);

        // Checks
        if (object == null) {
            throw Py.TypeError("buffer object expected (or unicode)");
        } else if (offset < 0) {
            throw Py.ValueError("offset must be zero or positive");
        } else if (size < -1) {
            throw Py.ValueError("size must be zero or positive");
        } else {
            // Checks ok
            return new Py2kBuffer(object, offset, size);
        }
    }

    @Override
    public int __len__() {
        PyBuffer buf = getBuffer();
        try {
            return buf.getLen();
        } finally {
            buf.release();
        }
    }

    @Override
    public PyString __repr__() {
        String fmt = "<read-only buffer for %s, size %d, offset %d at 0x%s>";
        String ret = String.format(fmt, Py.idstr((PyObject)object), size, offset, Py.idstr(this));
        return new PyString(ret);
    }

    @Override
    public PyString __str__() {
        PyBuffer buf = getBuffer();
        try {
            if (buf instanceof BaseBuffer) {
                // In practice, it always is
                return new PyString(buf.toString());
            } else {
                // But just in case ...
                String s = StringUtil.fromBytes(buf);
                return new PyString(s);
            }
        } finally {
            buf.release();
        }
    }

    /**
     * {@inheritDoc} A <code>buffer</code> implements this as concatenation and returns a
     * <code>str</code> ({@link PyString}) result.
     */
    @Override
    public PyObject __add__(PyObject other) {
        return buffer___add__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.buffer___add___doc)
    final PyObject buffer___add__(PyObject other) {

        // The other operand must offer us the buffer interface
        BufferProtocol bp = asBufferableOrNull(other);

        if (bp == null) {
            // Allow PyObject._basic_add to pick up the pieces or raise informative error
            return null;
        } else {
            // PyBuffer on the underlying object of this buffer
            PyBuffer buf = getBuffer();
            try {
                // And on the other operand (ask for simple 1D-bytes).
                PyBuffer otherBuf = bp.getBuffer(PyBUF.SIMPLE);
                try {
                    // Concatenate the buffers as strings
                    return new PyString(buf.toString().concat(otherBuf.toString()));
                } finally {
                    // Must always let go of the buffer
                    otherBuf.release();
                }
            } finally {
                // Must always let go of the buffer
                buf.release();
            }
        }
    }

    /**
     * {@inheritDoc} On a <code>buffer</code> it returns a <code>str</code> containing the buffer
     * contents <code>n</code> times.
     */
    @Override
    public PyObject __mul__(PyObject o) {
        return buffer___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.buffer___mul___doc)
    final PyObject buffer___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    /**
     * {@inheritDoc} On a <code>buffer</code> it returns a <code>str</code> containing the buffer
     * contents <code>n</code> times.
     */
    @Override
    public PyObject __rmul__(PyObject o) {
        return buffer___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.buffer___rmul___doc)
    final PyObject buffer___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    /*
     * ============================================================================================
     * Python API comparison operations
     * ============================================================================================
     */

    /**
     * Comparison function between two <code>buffer</code>s of bytes, returning 1, 0 or -1 as a>b,
     * a==b, or a&lt;b respectively. The comparison is by value, using Python unsigned byte
     * conventions, left-to-right (low to high index). Zero bytes are significant, even at the end
     * of the array: <code>[65,66,67]&lt;"ABC\u0000"</code>, for example and <code>[]</code> is less
     * than every non-empty b, while <code>[]==""</code>.
     * 
     * @param a left-hand wrapped array in the comparison
     * @param b right-hand wrapped object in the comparison
     * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
     */
    private static int compare(PyBuffer a, PyBuffer b) {

        // Compare elements one by one in these ranges:
        int ap = 0;
        int aEnd = ap + a.getLen();
        int bp = 0;
        int bEnd = b.getLen();

        while (ap < aEnd) {
            if (bp >= bEnd) {
                // a is longer than b
                return 1;
            } else {
                // Compare the corresponding bytes
                int aVal = a.intAt(ap++);
                int bVal = b.intAt(bp++);
                int diff = aVal - bVal;
                if (diff != 0) {
                    return (diff < 0) ? -1 : 1;
                }
            }
        }

        // All the bytes matched and we reached the end of a
        if (bp < bEnd) {
            // But we didn't reach the end of b
            return -1;
        } else {
            // And the end of b at the same time, so they're equal
            return 0;
        }
    }

    /**
     * Comparison function between this <code>buffer</code> and any other object. The inequality
     * comparison operators are based on this.
     * 
     * @param b
     * @return 1, 0 or -1 as this>b, this==b, or this&lt;b respectively, or -2 if the comparison is
     *         not implemented
     */
    private int buffer_cmp(PyObject b) {

        // Check the memeoryview is still alive: works here for all the inequalities
        PyBuffer buf = getBuffer();
        try {

            // Try to get a byte-oriented view
            PyBuffer bv = BaseBytes.getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject._cmp_unsafe() and related code.
                return -2;

            } else {

                try {
                    if (bv == buf) {
                        // Same buffer: quick result
                        return 0;
                    } else {
                        // Actually compare the contents
                        return compare(buf, bv);
                    }

                } finally {
                    // Must always let go of the buffer
                    bv.release();
                }
            }
        } finally {
            buf.release();
        }

    }

    /**
     * Fail-fast comparison function between byte array types and any other object, for when the
     * test is only for equality. The inequality comparison operators <code>__eq__</code> and
     * <code>__ne__</code> are based on this.
     * 
     * @param b
     * @return 0 if this==b, or +1 or -1 if this!=b, or -2 if the comparison is not implemented
     */
    private int buffer_cmpeq(PyObject b) {

        // Get a view on the underlying object
        PyBuffer buf = getBuffer();
        try {

            // Try to get a byte-oriented view
            PyBuffer bv = BaseBytes.getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject._cmp_unsafe() and related code.
                return -2;

            } else {

                try {
                    if (bv == buf) {
                        // Same buffer: quick result
                        return 0;
                    } else if (bv.getLen() != buf.getLen()) {
                        // Different size: can't be equal, and we don't care which is bigger
                        return 1;
                    } else {
                        // Actually compare the contents
                        return compare(buf, bv);
                    }

                } finally {
                    // Must always let go of the buffer
                    bv.release();
                }
            }
        } finally {
            buf.release();
        }

    }

    /*
     * These strings are adapted from the patch in CPython issue 15855 and the on-line documentation
     * most attributes do not come with any docstrings in CPython 2.7, so the make_pydocs trick
     * won't work. This is a complete set, although not all are needed in Python 2.7.
     */
    private final static String tobytes_doc = "M.tobytes() -> bytes\n\n"
            + "Return the data in the buffer as a bytestring (an object of class str).";

    private final static String tolist_doc = "M.tolist() -> list\n\n"
            + "Return the data in the buffer as a list of elements.";

    /*
     * ============================================================================================
     * Support for the Buffer API
     * ============================================================================================
     */

    /**
     * {@inheritDoc}
     * <p>
     * The {@link PyBuffer} returned from this method is provided directly by the underlying object
     * on which this buffer was constructed, taking account of the slicing arguments (offset and
     * size), if these were given when the buffer was constructed.
     */
    @Override
    public PyBuffer getBuffer(int flags) {

        // Get a simple buffer meeting the specification of tha caller
        PyBuffer buf = object.getBuffer(flags);

        // This may already be what we need, or this buffer may be a sub-range of the object
        if (offset > 0 || size >= 0) {
            /*
             * It's a sub-range so we have to construct a slice buffer on the first buffer. Take
             * care that the bounds of the slice are within the object, which may have changed size
             * since the buffer was created.
             */
            PyBuffer first = buf;
            int start = offset;
            int length = first.getLen() - start;

            if (length <= 0) {
                // Range now lies outside object: zero length slice
                start = length = 0;
            } else if (size >= 0 && size < length) {
                // A size less than the available bytes was specified (size==-1 => all of them)
                length = size;
            }

            // Now offset and length specify a feasible slice
            buf = first.getBufferSlice(flags, offset, length);

            // We should release our first lease because the slice buf holds one.
            // That lease will be released when buf is released.
            first.release();
        }
        return buf;
    }

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */
    /**
     * Gets the indexed element of the <code>buffer</code> as a one byte string. This is an
     * extension point called by PySequence in its implementation of {@link #__getitem__}. It is
     * guaranteed by PySequence that the index is within the bounds of the <code>buffer</code>.
     * 
     * @param index index of the element to get.
     * @return one-character string formed from the byte at the index
     */
    @Override
    protected PyString pyget(int index) {
        // Our chance to check the buffer is still alive
        PyBuffer buf = getBuffer();
        try {
            // Treat the byte at the index as a character code
            return new PyString(String.valueOf((char)buf.intAt(index)));
        } finally {
            buf.release();
        }
    }

    /**
     * Returns a slice of elements from this sequence as a PyString.
     * 
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a PyString corresponding the the given range of elements.
     */
    @Override
    protected synchronized PyString getslice(int start, int stop, int step) {
        // Our chance to check the buffer is still alive
        PyBuffer buf = getBuffer();
        try {
            int n = sliceLength(start, stop, step);
            PyBuffer first = buf;
            buf = first.getBufferSlice(PyBUF.FULL_RO, start, n, step);
            first.release(); // We've finished (buf holds a lease)
            PyString ret = Py.newString(buf.toString());
            return ret;
        } finally {
            buf.release();
        }
    }

    /**
     * <code>buffer*int</code> represents repetition in Python, and returns a <code>str</code> (
     * <code>bytes</code>) object.
     * 
     * @param count the number of times to repeat this.
     * @return a PyString repeating this buffer (as a <code>str</code>) that many times
     */
    @Override
    protected synchronized PyString repeat(int count) {
        PyBuffer buf = getBuffer();
        try {
            PyString ret = Py.newString(buf.toString());
            return (PyString)ret.repeat(count);
        } finally {
            buf.release();
        }
    }

    /**
     * Sets the indexed element of the <code>buffer</code> to the given value, treating the
     * operation as assignment to a slice of length one. This is different from the same operation
     * on a byte array, where the assigned value must be an int: here it must have the buffer API
     * and length one. This is an extension point called by PySequence in its implementation of
     * {@link #__setitem__} It is guaranteed by PySequence that the index is within the bounds of
     * the <code>buffer</code>. Any other clients calling <tt>pyset(int, PyObject)</tt> must make
     * the same guarantee.
     * 
     * @param index index of the element to set.
     * @param value to set this element to, regarded as a buffer of length one unit.
     * @throws PyException(AttributeError) if value cannot be converted to an integer
     * @throws PyException(ValueError) if value<0 or value>255
     */
    @Override
    public synchronized void pyset(int index, PyObject value) throws PyException {
        // Our chance to check the buffer is still alive
        PyBuffer buf = getBuffer();
        try {

            // Get a buffer API on the value being assigned
            PyBuffer valueBuf = BaseBytes.getViewOrError(value);
            try {
                if (valueBuf.getLen() != 1) {
                    // CPython 2.7 message
                    throw Py.ValueError("cannot modify size of buffer object");
                }
                buf.storeAt(valueBuf.byteAt(0), index);
            } finally {
                valueBuf.release();
            }
        } finally {
            buf.release();
        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics. If the step
     * size is one, it is a simple slice and the operation is equivalent to replacing that slice,
     * with the value, accessing the value via the buffer protocol.
     * 
     * <pre>
     * a = bytearray(b'abcdefghijklmnopqrst')
     * m = buffer(a)
     * m[2:7] = "ABCDE"
     * </pre>
     * 
     * Results in <code>a=bytearray(b'abABCDEhijklmnopqrst')</code>.
     * <p>
     * If the step size is one, but stop-start does not match the length of the right-hand-side a
     * ValueError is thrown.
     * <p>
     * If the step size is not one, and start!=stop, the slice defines a certain number of elements
     * to be replaced. This function is not available in Python 2.7 (but it is in Python 3.3).
     * <p>
     * 
     * <pre>
     * a = bytearray(b'abcdefghijklmnopqrst')
     * a[2:12:2] = iter( [65, 66, 67, long(68), "E"] )
     * </pre>
     * 
     * Results in <code>a=bytearray(b'abAdBfChDjElmnopqrst')</code> in Python 3.3.
     * 
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value an object consistent with the slice assignment
     */
    @Override
    protected synchronized void setslice(int start, int stop, int step, PyObject value) {
        // Our chance to check the buffer is still alive
        PyBuffer buf = getBuffer();
        try {

            if (step == 1 && stop < start) {
                // Because "b[5:2] = v" means insert v just before 5 not 2.
                // ... although "b[5:2:-1] = v means b[5]=v[0], b[4]=v[1], b[3]=v[2]
                stop = start;
            }

            // Get a buffer API on the value being assigned
            PyBuffer valueBuf = BaseBytes.getViewOrError(value);

            // We'll also regard the assigned slice as a buffer.
            PyBuffer bufSlice = null;

            try {
                // How many destination items? Has to match size of value.
                int n = sliceLength(start, stop, step);
                if (n != valueBuf.getLen()) {
                    // CPython 2.7 message
                    throw Py.ValueError("cannot modify size of buffer object");
                }

                /*
                 * In the next section, we get a sliced view of the buf and write the value to it.
                 * The approach to errors is unusual for compatibility with CPython. We pretend we
                 * will not need a WRITABLE buffer in order to avoid throwing a BufferError. This
                 * does not stop the returned object being writable, simply avoids the check. If in
                 * fact it is read-only, then trying to write raises TypeError.
                 */

                bufSlice = buf.getBufferSlice(PyBUF.FULL_RO, start, n, step);
                bufSlice.copyFrom(valueBuf);

            } finally {

                // Release the buffers we obtained (if we did)
                if (bufSlice != null) {
                    bufSlice.release();
                }
                valueBuf.release();
            }
        } finally {
            buf.release();
        }
    }

}
