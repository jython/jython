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
public class PyMemoryView extends PyObject {

    // XXX This should probably extend PySequence to get the slice behaviour

    public static final PyType TYPE = PyType.fromClass(PyMemoryView.class);

    /**
     * The buffer exported by the object. We do not a present implement the buffer sharing strategy
     * used by CPython <code>memoryview</code>.
     */
    private PyBuffer backing;
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
        return backing.getShape().length;
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

}
