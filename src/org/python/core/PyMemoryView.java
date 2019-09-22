// Copyright (c) 2013 Jython Developers
package org.python.core;

import org.python.core.buffer.BaseBuffer;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Class implementing the Python <code>memoryview</code> type. It provides a wrapper around the
 * Jython buffer API.
 */
@ExposedType(name = "memoryview", doc = BuiltinDocs.memoryview_doc, base = PyObject.class,
        isBaseType = false)
public class PyMemoryView extends PySequence implements BufferProtocol, Traverseproc {

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
    /** Hash value cached (so we still know it after {@link #release()} is called. */
    private int hashCache;
    private boolean hashCacheValid = false;

    /**
     * Construct a <code>PyMemoryView</code> from an object bearing the {@link BufferProtocol}
     * interface. If this object is already an exported buffer, the <code>memoryview</code> takes a
     * new lease on it. The buffer so obtained will be writable if the underlying object permits it.
     *
     * @param pybuf buffer exported by some underlying object
     * @throws ClassCastException in cases where {@code pybuf.getBuffer} does so.
     */
    public PyMemoryView(BufferProtocol pybuf) throws ClassCastException {
        super(TYPE);
        /*
         * Ask for the full set of facilities (strides, indirect, etc.) from the object in case they
         * are necessary for navigation, but only ask for read access. If the object is writable,
         * the PyBuffer will be writable.
         */
        backing = pybuf.getBuffer(PyBUF.FULL_RO);
    }

    @ExposedNew
    static PyObject memoryview_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        // One 'object' argument required
        if (args.length != 1) {
            throw Py.TypeError("memoryview() takes exactly one argument");
        }

        // Use the ArgParser to access it
        ArgParser ap = new ArgParser("memoryview", args, keywords, "object");
        PyObject obj = ap.getPyObject(0);

        if (obj instanceof BufferProtocol) {
            // Certain types that implement BufferProtocol do not implement the buffer protocol
            try {
                return new PyMemoryView((BufferProtocol) obj);
            } catch (ClassCastException e) { /* fall through to message */ }
        }
        throw Py.TypeError(
                "cannot make memory view because object does not have the buffer interface");
    }

    // @ExposedGet(doc = obj_doc) // Not exposed in Python 2.7
    public PyObject obj() {
        checkNotReleased();
        BufferProtocol obj = backing.getObj();
        return (obj instanceof PyObject) ? (PyObject)obj : Py.None;
    }

    @ExposedGet(doc = format_doc)
    public String format() {
        checkNotReleased();
        return backing.getFormat();
    }

    @ExposedGet(doc = itemsize_doc)
    public int itemsize() {
        checkNotReleased();
        return backing.getItemsize();
    }

    @ExposedGet(doc = shape_doc)
    public PyObject shape() {
        checkNotReleased();
        if (shape == null) {
            shape = tupleOf(backing.getShape());
        }
        return shape;
    }

    @ExposedGet(doc = ndim_doc)
    public int ndim() {
        checkNotReleased();
        return backing.getNdim();
    }

    @ExposedGet(doc = strides_doc)
    public PyObject strides() {
        checkNotReleased();
        if (strides == null) {
            strides = tupleOf(backing.getStrides());
        }
        return strides;
    }

    @ExposedGet(doc = suboffsets_doc)
    public PyObject suboffsets() {
        checkNotReleased();
        if (suboffsets == null) {
            suboffsets = tupleOf(backing.getSuboffsets());
        }
        return suboffsets;
    }

    @ExposedGet(doc = readonly_doc)
    public boolean readonly() {
        checkNotReleased();
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
        checkNotReleased();
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
        checkNotReleased();
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
        checkNotReleased();
        return backing.getLen();
    }

    @Override
    public int hashCode() {
        return memoryview___hash__();
    }

    @ExposedMethod
    final int memoryview___hash__() {
        if (!hashCacheValid) {
            // We'll have to calculate it: only possible if not released
            checkNotReleased();
            // And if not mutable
            if (backing.isReadonly()) {
                hashCache = backing.toString().hashCode();
                hashCacheValid = true;
            } else {
                throw Py.ValueError("cannot hash writable memoryview object");
            }
        }
        return hashCache;
    }

    /*
     * ============================================================================================
     * Python API comparison operations
     * ============================================================================================
     */

    @Override
    public PyObject __eq__(PyObject other) {
        return memoryview___eq__(other);
    }

    @Override
    public PyObject __ne__(PyObject other) {
        return memoryview___ne__(other);
    }

    @Override
    public PyObject __lt__(PyObject other) {
        return memoryview___lt__(other);
    }

    @Override
    public PyObject __le__(PyObject other) {
        return memoryview___le__(other);
    }

    @Override
    public PyObject __ge__(PyObject other) {
        return memoryview___ge__(other);
    }

    @Override
    public PyObject __gt__(PyObject other) {
        return memoryview___gt__(other);
    }

    /**
     * Comparison function between two buffers of bytes, returning 1, 0 or -1 as a>b, a==b, or
     * a&lt;b respectively. The comparison is by value, using Python unsigned byte conventions,
     * left-to-right (low to high index). Zero bytes are significant, even at the end of the array:
     * <code>[65,66,67]&lt;"ABC\u0000"</code>, for example and <code>[]</code> is less than every
     * non-empty b, while <code>[]==""</code>.
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
     * Comparison function between this memoryview and any other object. The inequality comparison
     * operators are based on this.
     * <p>
     * In Python 2.7, <code>memoryview</code> objects are ordered by their equivalent byte sequence
     * values, and there is no concept of a released <code>memoryview</code>. In Python 3,
     * <code>memoryview</code> objects are not ordered but may be tested for equality: a
     * <code>memoryview</code> is always equal to itself, and distinct <code>memoryview</code>
     * objects are equal if they are not released, and view equal bytes. This method supports the
     * Python 2.7 model, and should probably not survive into Jython 3.
     *
     * @param b
     * @return 1, 0 or -1 as this>b, this==b, or this&lt;b respectively, or -2 if the comparison is
     *         not implemented
     */
    private int memoryview_cmp(PyObject b) {

        // Check the memeryview is still alive: works here for all the inequalities
        checkNotReleased();

        // Try to get a byte-oriented view
        PyBuffer bv = BaseBytes.getView(b);

        if (bv == null) {
            // Signifies a type mis-match. See PyObject._cmp_unsafe() and related code.
            return -2;

        } else {

            try {
                if (bv == backing) {
                    // Same buffer: quick result
                    return 0;
                } else {
                    // Actually compare the contents
                    return compare(backing, bv);
                }

            } finally {
                // Must always let go of the buffer
                bv.release();
            }
        }

    }

    /**
     * Fail-fast comparison function between byte array types and any other object, for when the
     * test is only for equality. The inequality comparison operators <code>__eq__</code> and
     * <code>__ne__</code> are based on this.
     * <p>
     * In Python 2.7, <code>memoryview</code> objects are ordered by their equivalent byte sequence
     * values, and there is no concept of a released <code>memoryview</code>. In Python 3,
     * <code>memoryview</code> objects are not ordered but may be tested for equality: a
     * <code>memoryview</code> is always equal to itself, and distinct <code>memoryview</code>
     * objects are equal if they are not released, and view equal bytes. This method supports a
     * compromise between of the two and should be rationalised in Jython 3.
     *
     * @param b
     * @return 0 if this==b, or +1 or -1 if this!=b, or -2 if the comparison is not implemented
     */
    private int memoryview_cmpeq(PyObject b) {

        if (this == b) {
            // Same object: quick success (even if released)
            return 0;

        } else if (released) {
            // Released memoryview is not equal to anything (but not an error to have asked)
            return -1;

        } else if ((b instanceof PyMemoryView) && ((PyMemoryView)b).released) {
            // Released memoryview is not equal to anything (but not an error to have asked)
            return 1;

        } else {

            // Try to get a byte-oriented view
            PyBuffer bv = BaseBytes.getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject._cmp_unsafe() and related code.
                return -2;

            } else {

                try {
                    if (bv == backing) {
                        // Same buffer: quick result
                        return 0;
                    } else if (bv.getLen() != backing.getLen()) {
                        // Different size: can't be equal, and we don't care which is bigger
                        return 1;
                    } else {
                        // Actually compare the contents
                        return compare(backing, bv);
                    }

                } finally {
                    // Must always let go of the buffer
                    bv.release();
                }
            }
        }
    }

    /**
     * Implementation of __eq__ (equality) operator. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
     */
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.memoryview___eq___doc)
    final PyObject memoryview___eq__(PyObject other) {
        int cmp = memoryview_cmpeq(other);
        if (cmp == 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __ne__ (not equals) operator. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
     */
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.memoryview___ne___doc)
    final PyObject memoryview___ne__(PyObject other) {
        int cmp = memoryview_cmpeq(other);
        if (cmp == 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __lt__ (less than) operator. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
     */
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.memoryview___lt___doc)
    final PyObject memoryview___lt__(PyObject other) {
        int cmp = memoryview_cmp(other);
        if (cmp >= 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __le__ (less than or equal to) operator. Comparison with an invalid type
     * returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
     */
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.memoryview___le___doc)
    final PyObject memoryview___le__(PyObject other) {
        int cmp = memoryview_cmp(other);
        if (cmp > 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __ge__ (greater than or equal to) operator. Comparison with an invalid type
     * returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
     */
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.memoryview___ge___doc)
    final PyObject memoryview___ge__(PyObject other) {
        int cmp = memoryview_cmp(other);
        if (cmp >= 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __gt__ (greater than) operator. Comparison with an invalid type returns
     * null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
     */
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.memoryview___gt___doc)
    final PyObject memoryview___gt__(PyObject other) {
        int cmp = memoryview_cmp(other);
        if (cmp > 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Called at the start of a context-managed suite (supporting the <code>with</code> clause).
     *
     * @return this object
     */
    public PyObject __enter__() {
        return memoryview___enter__();
    }

    @ExposedMethod(names = "__enter__")
    final PyObject memoryview___enter__() {
        checkNotReleased();
        return this;
    }

    /**
     * Called at the end of a context-managed suite (supporting the <code>with</code> clause), and
     * will release the <code>memoryview</code>.
     *
     * @return false
     */
    public boolean __exit__(PyObject type, PyObject value, PyObject traceback) {
        return memoryview___exit__(type, value, traceback);
    }

    @ExposedMethod
    final boolean memoryview___exit__(PyObject type, PyObject value, PyObject traceback) {
        memoryview_release();
        return false;
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
        checkNotReleased(); // Only for compatibility with CPython
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
     * This becomes an exposed method in CPython from 3.2. The Jython implementation of
     * <code>memoryview</code> follows the Python 3.3 design internally and therefore safely
     * anticipates Python 3 in exposing <code>memoryview.release</code> along with the related
     * context-management behaviour.
     */
    public synchronized void release() {
        memoryview_release();
    }

    @ExposedMethod(doc = release_doc)
    public synchronized final void memoryview_release() {
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

    /**
     * Check that the memoryview is not released and raise a ValueError if it is. Almost every
     * operation must call this before it starts work.
     */
    protected void checkNotReleased() {
        if (released) {
            throw Py.ValueError("operation forbidden on released memoryview object");
        }
        // The backing should not have been released if the memoryview has not been.
        assert (!backing.isReleased());
    }

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */
    /**
     * Gets the indexed element of the memoryview as a one byte string. This is an extension point
     * called by PySequence in its implementation of {@link #__getitem__}. It is guaranteed by
     * PySequence that the index is within the bounds of the memoryview.
     *
     * @param index index of the element to get.
     * @return one-character string formed from the byte at the index
     */
    @Override
    protected PyString pyget(int index) {
        // Our chance to check the memoryview is still alive
        checkNotReleased();
        // Treat the byte at the index as a character code
        return new PyString(String.valueOf((char)backing.intAt(index)));
        // Originally implemented Python 3 semantics, returning a PyInteger (for byte-oriented) !
        // return new PyInteger(backing.intAt(index));
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
        // Our chance to check the memoryview is still alive
        checkNotReleased();

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
     * @throws PyException {@code NotImplemented} always
     */
    @Override
    protected synchronized PyMemoryView repeat(int count) throws PyException {
        throw Py.NotImplementedError("memoryview.repeat()");
    }

    /**
     * Sets the indexed element of the memoryview to the given value, treating the operation as
     * assignment to a slice of length one. This is different from the same operation on a byte
     * array, where the assigned value must be an int: here it must have the buffer API and length
     * one. This is an extension point called by PySequence in its implementation of
     * {@link #__setitem__} It is guaranteed by PySequence that the index is within the bounds of
     * the memoryview. Any other clients calling <tt>pyset(int, PyObject)</tt> must make the same
     * guarantee.
     *
     * @param index index of the element to set.
     * @param value to set this element to, regarded as a buffer of length one unit.
     * @throws PyException {@code AttributeError} if value cannot be converted to an integer
     * @throws PyException {@code ValueError} if value&lt;0 or value&gt;255
     */
    @Override
    public synchronized void pyset(int index, PyObject value) throws PyException {
        // Our chance to check the memoryview is still alive
        checkNotReleased();

        // Get a buffer API on the value being assigned
        PyBuffer valueBuf = BaseBytes.getViewOrError(value);
        try {
            if (valueBuf.getLen() != 1) {
                // CPython 2.7 message
                throw Py.ValueError("cannot modify size of memoryview object");
            }
            backing.storeAt(valueBuf.byteAt(0), index);
        } finally {
            valueBuf.release();
        }
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
        // Our chance to check the memoryview is still alive
        checkNotReleased();

        if (step == 1 && stop < start) {
            // Because "b[5:2] = v" means insert v just before 5 not 2.
            // ... although "b[5:2:-1] = v means b[5]=v[0], b[4]=v[1], b[3]=v[2]
            stop = start;
        }

        // Get a buffer API on the value being assigned
        PyBuffer valueBuf = BaseBytes.getViewOrError(value);

        // We'll also regard the assigned slice as a buffer.
        PyBuffer backingSlice = null;

        try {
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
            backingSlice.copyFrom(valueBuf);

        } finally {

            // Release the buffers we obtained (if we did)
            if (backingSlice != null) {
                backingSlice.release();
            }
            valueBuf.release();
        }
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (backing != null) {
            if (backing instanceof PyObject) {
                retVal = visit.visit((PyObject)backing, arg);
                if (retVal != 0) {
                    return retVal;
                }
            } else if (backing instanceof Traverseproc) {
                retVal = ((Traverseproc)backing).traverse(visit, arg);
                if (retVal != 0) {
                    return retVal;
                }
            }
        }
        if (shape != null) {
            retVal = visit.visit(shape, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (strides != null) {
            retVal = visit.visit(strides, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return suboffsets == null ? 0 : visit.visit(suboffsets, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        if (ob != null && (ob == backing || ob == shape || ob == strides || ob == suboffsets)) {
            return true;
        } else if (suboffsets instanceof Traverseproc) {
            return ((Traverseproc)suboffsets).refersDirectlyTo(ob);
        } else {
            return false;
        }
    }
}
