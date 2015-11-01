/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyByteArray;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * An implementation of Python <code>_io._RawIOBase</code> mirroring the arrangement of methods in
 * the CPython version.
 */
@ExposedType(name = "_io._RawIOBase", doc = PyRawIOBase.doc, base = PyIOBase.class)
public class PyRawIOBase extends PyIOBase {

    public static final PyType TYPE = PyType.fromClass(PyRawIOBase.class);

    public PyRawIOBase() {
        this(TYPE);
    }

    public PyRawIOBase(PyType subtype) {
        super(subtype);
    }

    @ExposedNew
    static PyObject _RawIOBase__new__(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        if (new_.for_type == subtype) {
            // We only want an _io._RawIOBase, so the constructor does it all
            return new PyRawIOBase();
        } else {
            // We want some sub-class of it (in which __init__ will be called by the caller)
            return new PyRawIOBaseDerived(subtype);
        }
    }

    /**
     * The read() method is implemented by calling readinto(); derived classes that want to support
     * read() only need to implement readinto() as a primitive operation. In general, readinto() can
     * be more efficient than read().
     *
     * @param n number of bytes to read (if possible)
     * @return a PyString holding the bytes read or <code>Py.None</code> (when a non-blocking source
     *         is not ready with further data)
     */
    public PyObject read(int n) {
        return _read(n);
    }

    /*
     * CPython comment: (It would be tempting to also provide an implementation of readinto() in
     * terms of read(), in case the latter is a more suitable primitive operation, but that would
     * lead to nasty recursion in case a subclass doesn't implement either.)
     */
    @ExposedMethod(defaults = "null", doc = read_doc)
    final PyObject _RawIOBase_read(PyObject n) {
        if (n == null || n == Py.None) {
            return _read(-1);
        } else if (n.isIndex()) {
            return _read(n.asInt());
        } else {
            throw tailoredTypeError("integer", n);
        }
    }

    /**
     * Implementation of the read() method once the argument has been reduced to an int.
     * @param n number of bytes to read (if possible)
     * @return a PyString holding the bytes read or <code>Py.None</code> (when a non-blocking source
     *         is not ready with further data)
     */
    private PyObject _read(int n) {

        if (n < 0) {
            // This is really a request to read the whole stream
            return invoke("readall");

        } else {
            // Allocate a buffer big enough to satisfy the request
            PyByteArray b = new PyByteArray(n);

            // Read up to that much using the (possibly overridden) readinto() method
            PyObject m = invoke("readinto", b);

            if (m.isIndex()) {
                // It returned the actual count of bytes read
                int count = m.asIndex();
                PyBuffer view = b.getBuffer(PyBUF.FULL_RO);
                // We can forget view.release() as the bytearray b is garbage outside this method.

                // Did we get all the bytes we expected?
                if (count < n) {
                    // No, so swap the view for a slice of just the data we actually read.
                    view = view.getBufferSlice(PyBUF.FULL_RO, 0, count);
                }

                // Make a str from that view
                return new PyString(view.toString());

            } else {
                // It must have returned None (signalling a vacuous read of non-blocking stream)
                return m;
            }

        }
    }

    /**
     * Read until end of file, using multiple <code>read()</code> operations on the underlying
     * stream. If the first <code>read()</code> returns <code>None</code> (only possible in the case
     * of a non-blocking stream), this method returns <code>None</code>.
     *
     * @return a PyString holding the bytes read or <code>Py.None</code> (when a non-blocking source
     *         is not ready with further data)
     */
    public PyObject readall() {
        return _RawIOBase_readall();
    }

    @ExposedMethod(doc = readall_doc)
    final synchronized PyObject _RawIOBase_readall() {

        // Get reference to the (possibly overridden) read() method
        PyObject readMethod = __getattr__("read");

        // Quite often, a single read operation will do the trick
        PyObject prev = readMethod.__call__(_jyio.DEFAULT_BUFFER_SIZE);

        if (!prev.__nonzero__()) {
            // Nothing on the first read: that means we're done
            return prev;

        } else {
            // Try a second read
            PyObject curr = readMethod.__call__(_jyio.DEFAULT_BUFFER_SIZE);
            if (!curr.__nonzero__()) {
                // Nothing on the second read: the result is just the first one
                return prev;

            } else {
                // Remembering more than one thing is hard: we're going to need a list
                PyList list = new PyList();
                list.add(prev);

                // Accumulate the current read result and get another, until we run out of bytes.
                do {
                    list.add(curr);
                    curr = readMethod.__call__(_jyio.DEFAULT_BUFFER_SIZE);
                } while (curr.__nonzero__());

                // Stitch it all together
                return Py.EmptyString.join(list);
            }
        }
    }

    /**
     * Read up to <code>len(b)</code> bytes into <code>bytearray b</code> and return the number of
     * bytes read. If the object is in non-blocking mode and no bytes are available,
     * <code>None</code> is returned.";
     *
     * @param b byte array to try to fill
     * @return number of bytes actually read or <code>Py.None</code> (when a non-blocking source is
     *         not ready with further data)
     */
    public PyObject readinto(PyObject b) {
        return _RawIOBase_readinto(b);
    }

    @ExposedMethod(doc = readinto_doc)
    final synchronized PyLong _RawIOBase_readinto(PyObject b) {
        throw unsupported("readinto");
    }

    /**
     * Write the given bytes or bytearray object to the underlying raw stream and return the number
     * of bytes written.
     *
     * @param b buffer of bytes to be written
     * @return the number of bytes written
     */
    public PyObject write(PyObject b) {
        return _RawIOBase_write(b);
    }

    @ExposedMethod(doc = write_doc)
    final PyLong _RawIOBase_write(PyObject b) {
        throw unsupported("write");
    }

    /*
     * Documentation strings: public where they might be useful to a subclass.
     */
    public static final String read_doc = "Read up to n bytes from the object and return them.\n"
            + "As a convenience, if n is unspecified or -1, readall() is called.";

    public static final String readall_doc =
            "Read and return all the bytes from the stream until EOF, using multiple\n"
                    + "calls to the stream if necessary.";

    public static final String readinto_doc =
            "Read up to len(b) bytes into bytearray b and return the number of bytes read.\n"
                    + "If the object is in non-blocking mode and no bytes are available,\n"
                    + "None is returned.";

    public static final String write_doc =
            "Write the given bytes or bytearray object, b, to the underlying raw\n"
                    + "stream and return the number of bytes written.";

    static final String doc = "Base class for raw binary I/O.";

}
