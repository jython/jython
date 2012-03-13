/* Copyright (c) Jython Developers */
package org.python.modules._fileio;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import org.python.core.ArgParser;
import org.python.core.BuiltinDocs;
import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyInteger;
import org.python.core.PyJavaType;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.io.FileIO;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "_fileio._FileIO")
public class PyFileIO extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyFileIO.class);

    private FileIO file;
	private PyObject name;
	private Boolean seekable;

    @ExposedGet
	public boolean closefd;

    /** The mode string */
    @ExposedGet(doc = BuiltinDocs.file_mode_doc)
    public String mode;

    /** The file's closer object; ensures the file is closed at shutdown */
    private Closer closer;

    public PyFileIO() {
        super(TYPE);
    }

    public PyFileIO(PyType subType) {
        super(subType);
    }

    public PyFileIO(String name, String mode, boolean closefd) {
    	this();
        _FileIO___init__(Py.newString(name), mode, closefd);
    }

    public PyFileIO(String name, String mode) {
    	this(name, mode, true);
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.file___init___doc)
    final void _FileIO___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("file", args, kwds, new String[] {"name", "mode", "closefd"}, 1);
        PyObject name = ap.getPyObject(0);
        if (!(name instanceof PyString)) {
            throw Py.TypeError("coercing to Unicode: need string, '" + name.getType().fastGetName()
                               + "' type found");
        }
        String mode = ap.getString(1, "r");
        boolean closefd = Py.py2boolean(ap.getPyObject(2, Py.True));
        // TODO: make this work with file channels so closefd=False can be used
        if (!closefd)
        	throw Py.ValueError("Cannot use closefd=False with file name");
        
        _FileIO___init__((PyString)name, mode, closefd);
        closer = new Closer(file, Py.getSystemState());
    }

    private void _FileIO___init__(PyString name, String mode, boolean closefd) {
    	mode = parseMode(mode);
        this.name = name;
        this.mode = mode;
        this.closefd = closefd;
        this.file = new FileIO((PyString) name, mode.replaceAll("b", ""));
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

    @ExposedMethod(doc = BuiltinDocs.file_close_doc)
    final synchronized void _FileIO_close() {
        if (closer != null) {
            closer.close();
            closer = null;
        } else {
            file.close();
        }
    }

    public void close() {
        _FileIO_close();
    }

    public boolean readable() {
        return _FileIO_readable();
    }

    @ExposedMethod(doc = "True if file was opened in a read mode.")
    final boolean _FileIO_readable() {
        return file.readable();
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.file_seek_doc)
    final synchronized PyObject _FileIO_seek(long pos, int how) {
        checkClosed();
        return Py.java2py(file.seek(pos, how));
    }

    public boolean seekable() {
        return _FileIO_seekable();
    }

    @ExposedMethod(doc = "True if file supports random-access.")
    final boolean _FileIO_seekable() {
    	if (seekable == null)
    		seekable = file.seek(0, 0) >= 0;
    	return seekable;
    }

    @ExposedMethod(doc = BuiltinDocs.file_tell_doc)
    final synchronized long _FileIO_tell() {
        checkClosed();
        return file.tell();
    }

    public long tell() {
        return _FileIO_tell();
    }

    @ExposedMethod(defaults = {"null"}, doc = BuiltinDocs.file_truncate_doc)
    final PyObject _FileIO_truncate(PyObject position) {
        if (position == null)
            return Py.java2py(_FileIO_truncate());
    	return Py.java2py(_FileIO_truncate(position.asLong()));
    }

    final synchronized long _FileIO_truncate(long position) {
        return file.truncate(position);
    }

    public long truncate(long position) {
        return _FileIO_truncate(position);
    }

    final synchronized long _FileIO_truncate() {
        return file.truncate(file.tell());
    }

    public void truncate() {
        _FileIO_truncate();
    }

    public boolean isatty() {
        return _FileIO_isatty();
    }

    @ExposedMethod(doc = BuiltinDocs.file_isatty_doc)
    final boolean _FileIO_isatty() {
        return file.isatty();
    }

    public boolean writable() {
        return _FileIO_writable();
    }

    @ExposedMethod(doc = "True if file was opened in a write mode.")
    final boolean _FileIO_writable() {
        return file.writable();
    }

    public PyObject fileno() {
        return _FileIO_fileno();
    }

    @ExposedMethod(doc = BuiltinDocs.file_fileno_doc)
    final PyObject _FileIO_fileno() {
        return PyJavaType.wrapJavaObject(file.fileno());
    }

    @ExposedMethod(doc = BuiltinDocs.file_read_doc)
    final synchronized PyString _FileIO_read(int size) {
        checkClosed();
        ByteBuffer buf = file.read(size);
        return new PyString(StringUtil.fromBytes(buf));
    }

    public PyString read(int size) {
        return _FileIO_read(size);
    }

    @ExposedMethod(doc = BuiltinDocs.file_read_doc)
    final synchronized PyString _FileIO_readall() {
    	return _FileIO_read(-1);
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
        }
        if (message == null) {
            message = String.format("argument 1 must be string or buffer, not %.200s",
                                    obj.getType().fastGetName());
        }
        throw Py.TypeError(message);
    }

    @ExposedMethod(doc = BuiltinDocs.file_write_doc)
    final PyObject _FileIO_write(PyObject obj) {
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
    final String _FileIO_toString() {
        if (name instanceof PyUnicode) {
            String escapedName = PyString.encode_UnicodeEscape(name.toString(), false);
            return String.format("<_fileio.FileIO name='%s', mode='%s'>", escapedName, mode);
        }
        return String.format("<_fileio.FileIO name='%s', mode='%s'>", name, mode);
    }

    @Override
    public String toString() {
        return _FileIO_toString();
    }

    private void checkClosed() {
        file.checkClosed();
    }

    @ExposedGet(name = "closed", doc = BuiltinDocs.file_closed_doc)
    public boolean getClosed() {
        return file.closed();
    }

    /**
     * XXX update docs - A mechanism to make sure PyFiles are closed on exit. On creation Closer adds itself
     * to a list of Closers that will be run by PyFileCloser on JVM shutdown. When a
     * PyFile's close or finalize methods are called, PyFile calls its Closer.close which
     * clears Closer out of the shutdown queue.
     *
     * We use a regular object here rather than WeakReferences and their ilk as they may
     * be collected before the shutdown hook runs. There's no guarantee that finalize will
     * be called during shutdown, so we can't use it. It's vital that this Closer has no
     * reference to the PyFile it's closing so the PyFile remains garbage collectable.
     */
    private static class Closer implements Callable<Void> {

        /**
         * The underlying file
         */
        private final FileIO file;
        private PySystemState sys;

        public Closer(FileIO file, PySystemState sys) {
            this.file = file;
            this.sys = sys;
            sys.registerCloser(this);
        }

        /** For closing directly */
        public void close() {
            sys.unregisterCloser(this);
            file.close();
            sys = null;
        }

        /** For closing as part of a shutdown process */
        public Void call() {
            file.close();
            sys = null;
            return null;
        }

    }
}
