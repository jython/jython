package org.python.core;

import org.python.core.buffer.BaseBuffer;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * Class implementing the Python <code>memoryview</code> type, at present highly incomplete. It
 * provides a wrapper around the Jython buffer API, but slice operations and most others are
 * missing.
 */
@ExposedType(name = "memoryview", doc = BuiltinDocs.memoryview_doc, base = PyObject.class,
        isBaseType = false)
public class PyMemoryView extends PySequence implements BufferProtocol {

    public static final PyType TYPE = PyType.fromClass(PyMemoryView.class);

    /** The buffer exported by the object of which this is a view. */
    private PyBuffer backing;
    /**
     * A memoryview in the released state forbids most Python API actions. If the underlying
     * PyBuffer is shared, the memoryview may be released while the underlying PyBuffer is not
     * "finally" released.
     */
    private boolean released;
    /** Cache the result of getting shape here. */
    private PyObject shape;
    /** Cache the result of getting strides here. */
    private PyObject strides;
    /** Cache the result of getting suboffsets here. */
    private PyObject suboffsets;

    /**
     * Construct a PyMemoryView from a PyBuffer interface. The buffer so obtained will be writable
     * if the underlying object permits it. The <code>memoryview</code> takes a new lease on the
     * <code>PyBuffer</code>.
     *
     * @param pybuf buffer exported by some underlying object
     */
    public PyMemoryView(PyBuffer pybuf) {
        super(TYPE);
        backing = pybuf.getBuffer(PyBUF.FULL_RO);
    }

    @ExposedNew
    static PyObject memoryview_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        PyObject obj = args[0];
        if (obj instanceof BufferProtocol) {
            /*
             * Ask for the full set of facilities (strides, indirect, etc.) from the object in case
             * they are necessary for navigation, but only ask for read access. If the object is
             * writable, the PyBuffer will be writable.
             */
            return new PyMemoryView(((BufferProtocol)obj).getBuffer(PyBUF.FULL_RO));
        } else {
            throw Py.TypeError("cannot make memory view because object does not have "
                    + "the buffer interface");
        }
    }

    @ExposedGet(doc = format_doc)
    public String format() {
        return backing.getFormat();
    }

    @ExposedGet(doc = itemsize_doc)
    public int itemsize() {
        return backing.getItemsize();
    }

    @ExposedGet(doc = shape_doc)
    public PyObject shape() {
        if (shape == null) {
            shape = tupleOf(backing.getShape());
        }
        return shape;
    }

    @ExposedGet(doc = ndim_doc)
    public int ndim() {
        return backing.getNdim();
    }

    @ExposedGet(doc = strides_doc)
    public PyObject strides() {
        if (strides == null) {
            strides = tupleOf(backing.getStrides());
        }
        return strides;
    }

    @ExposedGet(doc = suboffsets_doc)
    public PyObject suboffsets() {
        if (suboffsets == null) {
            suboffsets = tupleOf(backing.getSuboffsets());
        }
        return suboffsets;
    }

    @ExposedGet(doc = readonly_doc)
    public boolean readonly() {
        return backing.isReadonly();
    }

    /**
     * Implementation of Python <code>tobytes()</code>. Return the data in the buffer as a byte
     * string (an object of class <code>str</code>).
     *
     * @return byte string of buffer contents.
     */
    /*
     * From Python 3, this is equivalent to calling the <code>bytes</code> constructor on the
     * <code>memoryview</code>.
     */
    public PyString tobytes() {
        return memoryview_tobytes();
    }

    @ExposedMethod(doc = tobytes_doc)
    final PyString memoryview_tobytes() {
        if (backing instanceof BaseBuffer) {
            // In practice, it always is
            return new PyString(backing.toString());
        } else {
            // But just in case ...
            String s = StringUtil.fromBytes(backing);
            return new PyString(s);
        }
    }

    /**
     * Implementation of Python <code>tolist()</code>. Return the data in the buffer as a
     * <code>list</code> where the elements are an appropriate type (<code>int</code> in the case of
     * a byte-oriented buffer, which is the only case presently supported).
     *
     * @return a list of buffer contents.
     */
    public PyList tolist() {
        return memoryview_tolist();
    }

    @ExposedMethod(doc = tolist_doc)
    final PyList memoryview_tolist() {
        int n = backing.getLen();
        PyList list = new PyList();
        for (int i = 0; i < n; i++) {
            list.add(new PyInteger(backing.intAt(i)));
        }
        return list;
    }

    /**
     * Make an integer array into a PyTuple of PyLong values or None if the argument is null.
     *
     * @param x the array (or null)
     * @return the PyTuple (or Py.None)
     */
    private PyObject tupleOf(int[] x) {
        if (x != null) {
            PyLong[] pyx = new PyLong[x.length];
            for (int k = 0; k < x.length; k++) {
                pyx[k] = new PyLong(x[k]);
            }
            return new PyTuple(pyx, false);
        } else {
            return Py.None;
        }
    }

    @Override
    public int __len__() {
        return backing.getLen();
    }

    /*
     * These strings are adapted from the patch in CPython issue 15855 and the on-line documentation
     * most attributes do not come with any docstrings in CPython 2.7, so the make_pydocs trick
     * won't work. This is a complete set, although not all are needed in Python 2.7.
     */
    private final static String cast_doc = "M.cast(format[, shape]) -> memoryview\n\n"
            + "Cast a memoryview to a new format or shape.";

    private final static String release_doc = "M.release() -> None\n\n"
            + "Release the underlying buffer exposed by the memoryview object.";

    private final static String tobytes_doc = "M.tobytes() -> bytes\n\n"
            + "Return the data in the buffer as a bytestring (an object of class str).";

    private final static String tolist_doc = "M.tolist() -> list\n\n"
            + "Return the data in the buffer as a list of elements.";

    private final static String c_contiguous_doc = "c_contiguous\n"
            + "A bool indicating whether the memory is C contiguous.";

    private final static String contiguous_doc = "contiguous\n"
            + "A bool indicating whether the memory is contiguous.";

    private final static String f_contiguous_doc = "c_contiguous\n"
            + "A bool indicating whether the memory is Fortran contiguous.";

    private final static String format_doc = "format\n"
            + "A string containing the format (in struct module style)\n"
            + " for each element in the view.";

    private final static String itemsize_doc = "itemsize\n"
            + "The size in bytes of each element of the memoryview.";

    private final static String nbytes_doc = "nbytes\n"
            + "The amount of space in bytes that the array would use in\n"
            + "a contiguous representation.";

    private final static String ndim_doc = "ndim\n"
            + "An integer indicating how many dimensions of a multi-dimensional\n"
            + "array the memory represents.";

    private final static String obj_doc = "obj\n" + "The underlying object of the memoryview.";

    private final static String readonly_doc = "readonly\n"
            + "A bool indicating whether the memory is read only.";

    private final static String shape_doc = "shape\n"
            + "A tuple of ndim integers giving the shape of the memory\n"
            + "as an N-dimensional array.";

    private final static String strides_doc = "strides\n"
            + "A tuple of ndim integers giving the size in bytes to access\n"
            + "each element for each dimension of the array.";

    private final static String suboffsets_doc = "suboffsets\n"
            + "A tuple of ndim integers used internally for PIL-style arrays\n" + "or None.";

    /*
     * ============================================================================================
     * Support for the Buffer API
     * ============================================================================================
     *
     * The buffer API allows other classes to access the storage directly.
     */

    /**
     * {@inheritDoc}
     * <p>
     * The {@link PyBuffer} returned from this method is just the one on which the
     * <code>memoryview</code> was first constructed. The Jython buffer API is such that sharing
     * directly is safe (as long as the get-release discipline is observed).
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {
        /*
         * The PyBuffer itself does all the export counting, and since the behaviour of memoryview
         * need not change, it really is a simple as:
         */
        return backing.getBuffer(flags);
    }

    /**
     * Request a release of the underlying buffer exposed by the <code>memoryview</code> object.
     * Many objects take special actions when a view is held on them (for example, a
     * <code>bytearray</code> would temporarily forbid resizing); therefore, calling
     * <code>release()</code> is handy to remove these restrictions (and free any dangling
     * resources) as soon as possible.
     * <p>
     * After this method has been called, any further operation on the view raises a
     * <code>ValueError</code> (except <code>release()</code> itself which can be called multiple
     * times with the same effect as just one call).
     * <p>
     * This becomes an exposed method from Python 3.2. The Jython implementation of
     * <code>memoryview</code> follows the Python 3.3 design internally, which is the version that
     * resolved some long-standing design issues.
     */
    public synchronized void release() {
        /*
         * It is not an error to call this release method while this <code>memoryview</code> has
         * buffer exports (e.g. another <code>memoryview</code> was created on it), but it will not
         * release the underlying object until the last consumer releases the buffer.
         */
        if (!released) {
            // Release the buffer (which is not necessarily final)
            backing.release();
            // Remember we've been released
            released = true;
        }
    }

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */
    /**
     * Gets the indexed element of the memoryview as an integer. This is an extension point called
     * by PySequence in its implementation of {@link #__getitem__}. It is guaranteed by PySequence
     * that the index is within the bounds of the memoryview.
     *
     * @param index index of the element to get.
     */
    @Override
    protected PyInteger pyget(int index) {
        return new PyInteger(backing.intAt(index));
    }

    /**
     * Returns a slice of elements from this sequence as a PyMemoryView.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a PyMemoryView corresponding the the given range of elements.
     */
    @Override
    protected synchronized PyMemoryView getslice(int start, int stop, int step) {
        int n = sliceLength(start, stop, step);
        PyBuffer view = backing.getBufferSlice(PyBUF.FULL_RO, start, n, step);
        PyMemoryView ret = new PyMemoryView(view);
        view.release(); // We've finished (new PyMemoryView holds a lease)
        return ret;
    }

    /**
     * memoryview*int is not implemented in Python, so this should never be called. We still have to
     * override it to satisfy PySequence.
     *
     * @param count the number of times to repeat this.
     * @return never
     * @throws PyException(NotImlemented) always
     */
    @Override
    protected synchronized PyMemoryView repeat(int count) throws PyException {
        throw Py.NotImplementedError("memoryview.repeat()");
    }

    /**
     * Sets the indexed element of the memoryview to the given value. This is an extension point
     * called by PySequence in its implementation of {@link #__setitem__} It is guaranteed by
     * PySequence that the index is within the bounds of the memoryview. Any other clients calling
     * <tt>pyset(int)</tt> must make the same guarantee.
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     * @throws PyException(AttributeError) if value cannot be converted to an integer
     * @throws PyException(ValueError) if value<0 or value>255
     */
    public synchronized void pyset(int index, PyObject value) throws PyException {
        backing.storeAt(BaseBytes.byteCheck(value), index);
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics. If the step
     * size is one, it is a simple slice and the operation is equivalent to replacing that slice,
     * with the value, accessing the value via the buffer protocol.
     *
     * <pre>
     * a = bytearray(b'abcdefghijklmnopqrst')
     * m = memoryview(a)
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

        if (step == 1 && stop < start) {
            // Because "b[5:2] = v" means insert v just before 5 not 2.
            // ... although "b[5:2:-1] = v means b[5]=v[0], b[4]=v[1], b[3]=v[2]
            stop = start;
        }

        if (!(value instanceof BufferProtocol)) {
            String fmt = "'%s' does not support the buffer interface";
            throw Py.TypeError(String.format(fmt, value.getType().getName()));
        }

        // We'll try to get two new buffers: and finally release them.
        PyBuffer valueBuf = null, backingSlice = null;

        try {
            // Get a buffer API on the value being assigned
            valueBuf = ((BufferProtocol)value).getBuffer(PyBUF.FULL_RO);

            // How many destination items? Has to match size of value.
            int n = sliceLength(start, stop, step);
            if (n != valueBuf.getLen()) {
                // CPython 2.7 message
                throw Py.ValueError("cannot modify size of memoryview object");
            }

            /*
             * In the next section, we get a sliced view of the backing and write the value to it.
             * The approach to errors is unusual for compatibility with CPython. We pretend we will
             * not need a WRITABLE buffer in order to avoid throwing a BufferError. This does not
             * stop the returned object being writable, simply avoids the check. If in fact it is
             * read-only, then trying to write raises TypeError.
             */

            backingSlice = backing.getBufferSlice(PyBUF.FULL_RO, start, n, step);
            backing.copyFrom(valueBuf);

        } finally {

            // Release the buffers we obtained (if we did)
            if (backingSlice != null) {
                backingSlice.release();
            }
            if (valueBuf != null) {
                valueBuf.release();
            }
        }
    }

}
