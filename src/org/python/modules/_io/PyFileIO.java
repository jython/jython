/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import java.nio.ByteBuffer;

import org.python.core.ArgParser;
import org.python.core.BaseBytes;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyInteger;
import org.python.core.PyJavaType;
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

    private FileIO file;
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
        ArgParser ap = new ArgParser("file", args, kwds, new String[] {"name", "mode", "closefd"}, 1);
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
        this.file = new FileIO(name, mode.replaceAll("b", ""));
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

        return (reading ? "r" : "") + (writing ? "w" : "") + (appending ? "a" : "")
                + "b" + (updating ? "+" : "");
    }

    /**
     * Close the underlying file only if <code>closefd</code> was specified as (or defaulted to)
     * <code>True</code>.
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
            file.close();
        }
    }

    @Override
    public boolean readable() {
        return FileIO_readable();
    }

    @ExposedMethod(doc = "True if file was opened in a read mode.")
    final boolean FileIO_readable() {
        return file.readable();
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.file_seek_doc)
    final synchronized PyObject FileIO_seek(long pos, int how) {
        checkClosed();
        return Py.java2py(file.seek(pos, how));
    }

    @Override
    public boolean seekable() {
        return FileIO_seekable();
    }

    @ExposedMethod(doc = "True if file supports random-access.")
    final boolean FileIO_seekable() {
    	if (seekable == null) {
            seekable = file.seek(0, 0) >= 0;
        }
    	return seekable;
    }

    @ExposedMethod(doc = BuiltinDocs.file_tell_doc)
    final synchronized long FileIO_tell() {
        checkClosed();
        return file.tell();
    }

    @Override
    public long tell() {
        return FileIO_tell();
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
        synchronized (file) {
            return file.truncate(file.tell());
        }
    }

    /** Common to FileIO_truncate(size) and truncate(size). */
    private final long _truncate(long size) {
        synchronized (file) {
            return file.truncate(size);
        }
    }

    @Override
    public boolean isatty() {
        return FileIO_isatty();
    }

    @ExposedMethod(doc = BuiltinDocs.file_isatty_doc)
    final boolean FileIO_isatty() {
        return file.isatty();
    }

    @Override
    public boolean writable() {
        return FileIO_writable();
    }

    @ExposedMethod(doc = "True if file was opened in a write mode.")
    final boolean FileIO_writable() {
        return file.writable();
    }

    @Override
    public PyObject fileno() {
        return FileIO_fileno();
    }

    @ExposedMethod(doc = BuiltinDocs.file_fileno_doc)
    final PyObject FileIO_fileno() {
        return PyJavaType.wrapJavaObject(file.fileno());
    }

    @ExposedMethod(defaults = {"-1"}, doc = BuiltinDocs.file_read_doc)
    final synchronized PyString FileIO_read(int size) {
        checkClosed();
        ByteBuffer buf = file.read(size);
        return new PyString(StringUtil.fromBytes(buf));
    }

    @Override
    public PyString read(int size) {
        return FileIO_read(size);
    }

    @ExposedMethod(doc = BuiltinDocs.file_read_doc)
    final synchronized PyString FileIO_readall() {
    	return FileIO_read(-1);
    }

    /**
     * Return a String for writing to the underlying file from obj.
     */
    private String asWritable(PyObject obj, String message) {
        if (obj instanceof PyUnicode) {
            return ((PyUnicode)obj).encode();
        } else if (obj instanceof PyString) {
            return ((PyString) obj).getString();
        } else if (obj instanceof PyArray) {
            return ((PyArray)obj).tostring();
        } else if (obj instanceof BaseBytes) {
            return StringUtil.fromBytes((BaseBytes)obj);
        }
        if (message == null) {
            message = String.format("argument 1 must be string or buffer, not %.200s",
                                    obj.getType().fastGetName());
        }
        throw Py.TypeError(message);
    }

    @ExposedMethod(doc = BuiltinDocs.file_write_doc)
    final PyObject FileIO_write(PyObject obj) {
    	String writable = asWritable(obj, null);
    	byte[] bytes = StringUtil.toBytes(writable);
        int written = write(ByteBuffer.wrap(bytes));
    	return new PyInteger(written);
    }

    final synchronized int write(ByteBuffer buf) {
        checkClosed();
        return file.write(buf);
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

    private void checkClosed() {
        file.checkClosed();
    }

    @ExposedGet(name = "closed", doc = BuiltinDocs.file_closed_doc)
    public boolean getClosed() {
        return file.closed();
    }

}
