package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * Class implementing the Python <code>memoryview</code> type, at present highly incomplete. It
 * provides a wrapper around the Jython buffer API, but slice operations and most others are
 * missing.
 */
@ExposedType(name = "memoryview", base = PyObject.class, isBaseType = false)
public class PyMemoryView extends PyObject implements BufferProtocol {

    // XXX This should probably extend PySequence to get the slice behaviour

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
    private PyTuple shape;
    /** Cache the result of getting strides here. */
    private PyTuple strides;

    /**
     * Construct a PyMemoryView from an object that bears the necessary BufferProtocol interface.
     * The buffer so obtained will be writable if the underlying object permits it.
     *
     * @param obj object that will export the buffer
     */
    public PyMemoryView(BufferProtocol obj) {
        /*
         * Ask for the full set of facilities (strides, indirect, etc.) from the object in case they
         * are necessary for navigation, but only ask for read access. If the object is writable,
         * the PyBuffer will be writable.
         */
        backing = obj.getBuffer(PyBUF.FULL_RO);
    }

    @ExposedNew
    static PyObject memoryview_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        PyObject obj = args[0];
        if (obj instanceof BufferProtocol) {
            return new PyMemoryView((BufferProtocol)obj);
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
    public PyTuple shape() {
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
    public PyTuple strides() {
        if (strides == null) {
            strides = tupleOf(backing.getStrides());
        }
        return strides;
    }

    @ExposedGet(doc = readonly_doc)
    public boolean readonly() {
        return backing.isReadonly();
    }

    /**
     * Make an integer array into a PyTuple of PyInteger values.
     *
     * @param x the array
     * @return the PyTuple
     */
    private PyTuple tupleOf(int[] x) {
        PyInteger[] pyx = new PyInteger[x.length];
        for (int k = 0; k < x.length; k++) {
            pyx[k] = new PyInteger(x[k]);
        }
        return new PyTuple(pyx, false);
    }

    /*
     * These strings are adapted from the on-line documentation as the attributes do not come with
     * any docstrings.
     */
    private final static String memoryview_tobytes_doc = "tobytes()\n\n"
            + "Return the data in the buffer as a bytestring (an object of class str).\n\n"
            + ">>> m = memoryview(\"abc\")\n" + ">>> m.tobytes()\n" + "'abc'";

    private final static String memoryview_tolist_doc = "tolist()\n\n"
            + "Return the data in the buffer as a list of integers.\n\n"
            + ">>> memoryview(\"abc\").tolist()\n" + "[97, 98, 99]";

    private final static String format_doc = "format\n"
            + "A string containing the format (in struct module style) for each element in\n"
            + "the view. This defaults to 'B', a simple bytestring.\n";

    private final static String itemsize_doc = "itemsize\n"
            + "The size in bytes of each element of the memoryview.\n";

    private final static String shape_doc = "shape\n"
            + "A tuple of integers the length of ndim giving the shape of the memory as an\n"
            + "N-dimensional array.\n";

    private final static String ndim_doc = "ndim\n"
            + "An integer indicating how many dimensions of a multi-dimensional array the\n"
            + "memory represents.\n";

    private final static String strides_doc = "strides\n"
            + "A tuple of integers the length of ndim giving the size in bytes to access\n"
            + "each element for each dimension of the array.\n";

    private final static String readonly_doc = "readonly\n"
            + "A bool indicating whether the memory is read only.\n";

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
     * This becomes an exposed method only in Python 3.2, but the Jython implementation of
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

}
