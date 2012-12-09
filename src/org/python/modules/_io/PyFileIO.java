/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import java.nio.ByteBuffer;

import org.python.core.ArgParser;
import org.python.core.BaseBytes;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyBuffer;
import org.python.core.PyInteger;
import org.python.core.PyJavaType;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.io.FileIO;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "_io.FileIO", base = PyRawIOBase.class)
public class PyFileIO extends PyRawIOBase {

    public static final PyType TYPE = PyType.fromClass(PyFileIO.class);

    private FileIO ioDelegate;  // XXX final?
    private PyObject name;
    private Boolean seekable;

    @ExposedGet
    public boolean closefd;

    /** The mode string */
    @ExposedGet(doc = BuiltinDocs.file_mode_doc)
    public String mode;

    public PyFileIO() {
        super(TYPE);
    }

    public PyFileIO(PyType subType) {
        super(subType);
    }

    public PyFileIO(String name, String mode, boolean closefd) {
        this();
        FileIO___init__(Py.newString(name), mode, closefd);
    }

    public PyFileIO(String name, String mode) {
        this(name, mode, true);
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.file___init___doc)
    final void FileIO___init__(PyObject[] args, String[] kwds) {
        ArgParser ap =
                new ArgParser("file", args, kwds, new String[] {"name", "mode", "closefd"}, 1);
        PyObject name = ap.getPyObject(0);
        if (!(name instanceof PyString)) {
            throw Py.TypeError("coercing to Unicode: need string, '" + name.getType().fastGetName()
                    + "' type found");
        }
        String mode = ap.getString(1, "r");
        boolean closefd = Py.py2boolean(ap.getPyObject(2, Py.True));
        // TODO: make this work with file channels so closefd=False can be used
        if (!closefd) {
            throw Py.ValueError("Cannot use closefd=False with file name");
        }

        FileIO___init__((PyString)name, mode, closefd);
    }

    private void FileIO___init__(PyString name, String mode, boolean closefd) {
        mode = parseMode(mode);
        this.name = name;
        this.mode = mode;
        this.closefd = closefd;
        this.ioDelegate = new FileIO(name, mode.replaceAll("b", ""));
    }

    private String parseMode(String mode) {
        if (mode.length() == 0) {
            throw Py.ValueError("empty mode string");
        }

        String origMode = mode;
        if ("rwa".indexOf(mode.charAt(0)) == -1) {
            throw Py.ValueError("mode string must begin with one of 'r', 'w', 'a' or 'U', not '"
                    + origMode + "'");
        }

        boolean reading = mode.contains("r");
        boolean writing = mode.contains("w");
        boolean appending = mode.contains("a");
        boolean updating = mode.contains("+");

        return (reading ? "r" : "") + (writing ? "w" : "") + (appending ? "a" : "") + "b"
                + (updating ? "+" : "");
    }

    /*
     * ===========================================================================================
     * Exposed methods in the order they appear in CPython's fileio.c method table
     * ===========================================================================================
     */

    // _RawIOBase.read is correct for us
    // _RawIOBase.readall is correct for us

    @Override
    public PyObject readinto(PyObject buf) {
        return FileIO_readinto(buf);
    }

    @ExposedMethod(doc = readinto_doc)
    final PyLong FileIO_readinto(PyObject buf) {
        // Check we can do this
        _checkClosed();
        _checkReadable();
        // Perform the operation through a buffer view on the object
        PyBuffer pybuf = writablePyBuffer(buf);
        try {
            PyBuffer.Pointer bp = pybuf.getBuf();
            ByteBuffer byteBuffer = ByteBuffer.wrap(bp.storage, bp.offset, pybuf.getLen());
            int count;
            synchronized (ioDelegate) {
                count = ioDelegate.readinto(byteBuffer);
            }
            return new PyLong(count);
        } finally {
            // Must unlock the PyBuffer view from client's object
            pybuf.release();
        }
    }

    @Override
    public PyObject write(PyObject buf) {
        return FileIO_write(buf);
    }

    @ExposedMethod(doc = write_doc)
    final PyLong FileIO_write(PyObject obj) {
        _checkWritable();
        // Get or synthesise a buffer API on the object to be written
        PyBuffer pybuf = readablePyBuffer(obj);
        try {
            // Access the data as a java.nio.ByteBuffer [pos:limit] within possibly larger array
            PyBuffer.Pointer bp = pybuf.getBuf();
            ByteBuffer byteBuffer = ByteBuffer.wrap(bp.storage, bp.offset, pybuf.getLen());
            int count;
            synchronized (ioDelegate) {
                count = ioDelegate.write(byteBuffer);
            }
            return new PyLong(count);
        } finally {
            // Even if that went badly, we should release the lock on the client buffer
            pybuf.release();
        }
    }

    @Override
    public long truncate() {
        return _truncate();
    }

    @Override
    public long truncate(long size) {
        return _truncate(size);
    }

    @ExposedMethod(defaults = "null", doc = truncate_doc)
    final long FileIO_truncate(PyObject size) {
        return (size != null) ? _truncate(size.asLong()) : _truncate();
    }

    /** Common to FileIO_truncate(null) and truncate(). */
    private final long _truncate() {
        synchronized (ioDelegate) {
            return ioDelegate.truncate(ioDelegate.tell());
        }
    }

    /** Common to FileIO_truncate(size) and truncate(size). */
    private final long _truncate(long size) {
        synchronized (ioDelegate) {
            return ioDelegate.truncate(size);
        }
    }

    /**
     * Close the underlying ioDelegate only if <code>closefd</code> was specified as (or defaulted
     * to) <code>True</code>.
     */
    @Override
    public void close() {
        FileIO_close();
    }

    @ExposedMethod
    final synchronized void FileIO_close() {
        // Close this object to further input (also calls flush)
        super.close();
        // Now close downstream (if required to)
        if (closefd) {
            ioDelegate.close();
        }
    }

    @Override
    public boolean readable() {
        return FileIO_readable();
    }

    @ExposedMethod(doc = "True if file was opened in a read mode.")
    final boolean FileIO_readable() {
        return ioDelegate.readable();
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.file_seek_doc)
    final synchronized PyObject FileIO_seek(long pos, int how) {
        _checkClosed();
        return Py.java2py(ioDelegate.seek(pos, how));
    }

    @Override
    public boolean seekable() {
        return FileIO_seekable();
    }

    @ExposedMethod(doc = "True if file supports random-access.")
    final boolean FileIO_seekable() {
        if (seekable == null) {
            seekable = ioDelegate.seek(0, 0) >= 0;
        }
        return seekable;
    }

    @ExposedMethod(doc = BuiltinDocs.file_tell_doc)
    final synchronized long FileIO_tell() {
        _checkClosed();
        return ioDelegate.tell();
    }

    @Override
    public long tell() {
        return FileIO_tell();
    }

    @Override
    public boolean isatty() {
        return FileIO_isatty();
    }

    @ExposedMethod(doc = BuiltinDocs.file_isatty_doc)
    final boolean FileIO_isatty() {
        return ioDelegate.isatty();
    }

    @Override
    public boolean writable() {
        return FileIO_writable();
    }

    @ExposedMethod(doc = "True if file was opened in a write mode.")
    final boolean FileIO_writable() {
        return ioDelegate.writable();
    }

    @Override
    public PyObject fileno() {
        return FileIO_fileno();
    }

    @ExposedMethod(doc = BuiltinDocs.file_fileno_doc)
    final PyObject FileIO_fileno() {
        return PyJavaType.wrapJavaObject(ioDelegate.fileno());
    }

    // fileio.c has no flush(), but why not, when there is fdflush()?
    // And it is a no-op for Jython io.FileIO, but why when there is FileChannel.force()?
    @Override
    public void flush() {
        FileIO_flush();
    }

    @ExposedMethod(doc = "Flush write buffers.")
    final void FileIO_flush() {
        if (writable()) {
            // Check for *downstream* close. (Locally, closed means "closed to client actions".)
            ioDelegate.checkClosed();
            ioDelegate.flush();
        }
    }

    @ExposedMethod(names = {"__str__", "__repr__"}, doc = BuiltinDocs.file___str___doc)
    final String FileIO_toString() {
        if (name instanceof PyUnicode) {
            String escapedName = PyString.encode_UnicodeEscape(name.toString(), false);
            return String.format("<_io.FileIO name='%s', mode='%s'>", escapedName, mode);
        }
        return String.format("<_io.FileIO name='%s', mode='%s'>", name, mode);
    }

    @Override
    public String toString() {
        return FileIO_toString();
    }

}
