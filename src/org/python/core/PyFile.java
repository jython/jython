/*
 * Copyright (c) Corporation for National Research Initiatives
 * Copyright (c) Jython Developers
 */
package org.python.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import org.python.core.finalization.FinalizableBuiltin;
import org.python.core.finalization.FinalizeTrigger;
import org.python.core.io.BinaryIOWrapper;
import org.python.core.io.BufferedIOBase;
import org.python.core.io.BufferedRandom;
import org.python.core.io.BufferedReader;
import org.python.core.io.BufferedWriter;
import org.python.core.io.FileIO;
import org.python.core.io.IOBase;
import org.python.core.io.LineBufferedRandom;
import org.python.core.io.LineBufferedWriter;
import org.python.core.io.RawIOBase;
import org.python.core.io.StreamIO;
import org.python.core.io.TextIOBase;
import org.python.core.io.TextIOWrapper;
import org.python.core.io.UniversalIOWrapper;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * The Python file type. Wraps an {@link TextIOBase} object.
 */
@ExposedType(name = "file", doc = BuiltinDocs.file_doc)
public class PyFile extends PyObject implements FinalizableBuiltin, Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyFile.class);

    /** The filename */
    @ExposedGet(doc = BuiltinDocs.file_name_doc)
    public PyObject name;

    /** The mode string */
    @ExposedGet(doc = BuiltinDocs.file_mode_doc)
    public String mode;

    @ExposedGet(doc = BuiltinDocs.file_encoding_doc)
    public String encoding;

    @ExposedGet(doc = BuiltinDocs.file_errors_doc)
    public String errors;

    /** Indicator dictating whether a space should be written to this
     * file on the next print statement (not currently implemented in
     * print ) */
    public boolean softspace = false;

    /** Whether this file is opened for reading */
    private boolean reading = false;

    /** Whether this file is opened for writing */
    private boolean writing = false;

    /** Whether this file is opened in appending mode */
    private boolean appending = false;

    /** Whether this file is opened for updating */
    private boolean updating = false;

    /** Whether this file is opened in binary mode */
    private boolean binary = false;

    /** Whether this file is opened in universal newlines mode */
    private boolean universal = false;

    /** The underlying IO object */
    private TextIOBase file;

    /** The file's closer object; ensures the file is closed at
     * shutdown */
    private Closer closer;

    public PyFile() {FinalizeTrigger.ensureFinalizer(this);}

    public PyFile(PyType subType) {
        super(subType);
        FinalizeTrigger.ensureFinalizer(this);
    }

    public PyFile(RawIOBase raw, String name, String mode, int bufsize) {
        parseMode(mode);
        file___init__(raw, name, mode, bufsize);
        FinalizeTrigger.ensureFinalizer(this);
    }

    public PyFile(InputStream istream, String name, String mode, int bufsize, boolean closefd) {
        parseMode(mode);
        file___init__(new StreamIO(istream, closefd), name, mode, bufsize);
        FinalizeTrigger.ensureFinalizer(this);
    }

    /**
     * Creates a file object wrapping the given <code>InputStream</code>. The builtin
     * method <code>file</code> doesn't expose this functionality (<code>open</code> does
     * albeit deprecated) as it isn't available to regular Python code. To wrap an
     * InputStream in a file from Python, use
     * {@link org.python.core.util.FileUtil#wrap(InputStream, String, int)}
     * {@link org.python.core.util.FileUtil#wrap(InputStream, String)}
     * {@link org.python.core.util.FileUtil#wrap(InputStream, int)}
     * {@link org.python.core.util.FileUtil#wrap(InputStream)}
     */
    public PyFile(InputStream istream, String mode, int bufsize) {
        this(istream, "<Java InputStream '" + istream + "' as file>", mode, bufsize, true);
    }

    public PyFile(InputStream istream, String mode) {
        this(istream, mode, -1);
    }

    public PyFile(InputStream istream, int bufsize) {
        this(istream, "r", bufsize);
    }

    public PyFile(InputStream istream) {
        this(istream, -1);
    }

    public PyFile(OutputStream ostream, String name, String mode, int bufsize, boolean closefd) {
        parseMode(mode);
        file___init__(new StreamIO(ostream, closefd), name, mode, bufsize);
        FinalizeTrigger.ensureFinalizer(this);
    }

    /**
     * Creates a file object wrapping the given <code>OutputStream</code>. The builtin
     * method <code>file</code> doesn't expose this functionality (<code>open</code> does
     * albeit deprecated) as it isn't available to regular Python code. To wrap an
     * OutputStream in a file from Python, use
     * {@link org.python.core.util.FileUtil#wrap(OutputStream, String, int)}
     * {@link org.python.core.util.FileUtil#wrap(OutputStream, String)}
     * {@link org.python.core.util.FileUtil#wrap(OutputStream, int)}
     * {@link org.python.core.util.FileUtil#wrap(OutputStream)}
     */

    public PyFile(OutputStream ostream, String mode, int bufsize) {
         this(ostream, "<Java OutputStream '" + ostream + "' as file>", mode, bufsize, true);
    }

    public PyFile(OutputStream ostream, int bufsize) {
        this(ostream, "w", bufsize);
    }

    public PyFile(OutputStream ostream) {
        this(ostream, -1);
    }

    public PyFile(String name, String mode, int bufsize) {
        file___init__(new FileIO(name, parseMode(mode)), name, mode, bufsize);
        FinalizeTrigger.ensureFinalizer(this);
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.file___init___doc)
    final void file___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("file", args, kwds, new String[] {"name", "mode", "buffering"},
                                     1);
        PyObject name = ap.getPyObject(0);
        String mode = ap.getString(1, "r");
        int bufsize = ap.getInt(2, -1);
        file___init__(new FileIO((PyString) name, parseMode(mode)), name, mode, bufsize);
        closer = new Closer(file, Py.getSystemState());
    }

    private void file___init__(RawIOBase raw, String name, String mode, int bufsize) {
        file___init__(raw, Py.newStringOrUnicode(name), mode, bufsize);
    }

    private void file___init__(RawIOBase raw, PyObject name, String mode, int bufsize) {
        this.name = name;
        this.mode = mode;

        BufferedIOBase buffer = createBuffer(raw, bufsize);
        if (universal) {
            this.file = new UniversalIOWrapper(buffer);
        } else if (!binary) {
            this.file = new TextIOWrapper(buffer);
        } else {
            this.file = new BinaryIOWrapper(buffer);
        }
    }

    /**
     * Set the strings defining the encoding and error handling policy. Setting these strings
     * affects behaviour of the {@link #writelines(PyObject)} when passed a {@link PyUnicode} value.
     *
     * @param encoding the <code>encoding</code> property of <code>file</code>.
     * @param errors the <code>errors</code> property of <code>file</code> (or <code>null</code>).
     */
    void setEncoding(String encoding, String errors) {
        this.encoding = encoding;
        this.errors = errors;
    }

    /**
     * Wrap the given RawIOBase with a BufferedIOBase according to the
     * mode and given bufsize.
     *
     * @param raw a RawIOBase value
     * @param bufsize an int size of the buffer
     * @return a BufferedIOBase wrapper
     */
    private BufferedIOBase createBuffer(RawIOBase raw, int bufsize) {
        if (bufsize < 0) {
            bufsize = IOBase.DEFAULT_BUFFER_SIZE;
        }
        boolean lineBuffered = bufsize == 1;
        BufferedIOBase buffer;
        if (updating) {
            buffer = lineBuffered ? new LineBufferedRandom(raw) : new BufferedRandom(raw, bufsize);
        } else if (writing || appending) {
            buffer = lineBuffered ? new LineBufferedWriter(raw) : new BufferedWriter(raw, bufsize);
        } else if (reading) {
            // Line buffering is for output only
            buffer = new BufferedReader(raw, lineBuffered ? IOBase.DEFAULT_BUFFER_SIZE : bufsize);
        } else {
            // Should never happen
            throw Py.ValueError("unknown mode: '" + mode + "'");
        }
        return buffer;
    }

    /**
     * Parse and validate the python file mode, returning a cleaned file mode suitable for FileIO.
     *
     * @param mode a python file mode String
     * @return a RandomAccessFile mode String
     */
    private String parseMode(String mode) {

        String message = null;
        boolean duplicate = false, invalid = false, text_intent = false;
        int n = mode.length();

        // Convert the letters to booleans, noticing duplicates
        for (int i = 0; i < n; i++) {
            char c = mode.charAt(i);

            switch (c) {
                case 'r':
                    duplicate = reading;
                    reading = true;
                    break;
                case 'w':
                    duplicate = writing;
                    writing = true;
                    break;
                case 'a':
                    duplicate = appending;
                    appending = true;
                    break;
                case '+':
                    duplicate = updating;
                    updating = true;
                    break;
                case 'b':
                    duplicate = binary;
                    binary = true;
                    break;
                case 't':
                    duplicate = text_intent;
                    text_intent = true;
                    binary = false;
                    break;
                case 'U':
                    duplicate = universal;
                    universal = true;
                    break;
                default:
                    invalid = true;
            }

            // duplicate is set iff c was encountered previously */
            if (duplicate) {
                invalid = true;
                break;
            }
        }

        // Implications
        reading |= universal;
        binary |= universal;

        // Standard tests and the mode for FileIO
        StringBuilder fileioMode = new StringBuilder();
        if (!invalid) {
            if (universal && (writing || appending)) {
                // Not quite true, consider 'Ub', but it's what CPython says:
                message = "universal newline mode can only be used with modes starting with 'r'";
            } else {
                // Build the FileIO mode string
                if (reading) {
                    fileioMode.append('r');
                }
                if (writing) {
                    fileioMode.append('w');
                }
                if (appending) {
                    fileioMode.append('a');
                }
                if (fileioMode.length() != 1) {
                    // We should only have added one of the above
                    message = "mode string must begin with one of 'r', 'w', 'a' or 'U', not '" //
                            + mode + "'";
                }
                if (updating) {
                    fileioMode.append('+');
                }
            }
            invalid |= (message != null);
        }

        // Finally, if invalid, report this as an error
        if (invalid) {
            if (message == null) {
                // Duplicates discovered or invalid symbols
                message = String.format("invalid mode: '%.20s'", mode);
            }
            throw Py.ValueError(message);
        }

        return fileioMode.toString();
    }

    @ExposedMethod(defaults = {"-1"}, doc = BuiltinDocs.file_read_doc)
    final synchronized PyString file_read(int size) {
        checkClosed();
        return new PyString(file.read(size));
    }

    public PyString read(int size) {
        return file_read(size);
    }

    public PyString read() {
        return file_read(-1);
    }

    @ExposedMethod(doc = BuiltinDocs.file_readinto_doc)
    final synchronized int file_readinto(PyObject buf) {
        checkClosed();
        return file.readinto(buf);
    }

    public int readinto(PyObject buf) {
        return file_readinto(buf);
    }

    @ExposedMethod(defaults = {"-1"}, doc = BuiltinDocs.file_readline_doc)
    final synchronized PyString file_readline(int max) {
        checkClosed();
        return new PyString(file.readline(max));
    }

    public PyString readline(int max) {
        return file_readline(max);
    }

    public PyString readline() {
        return file_readline(-1);
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.file_readlines_doc)
    final synchronized PyObject file_readlines(int sizehint) {
        checkClosed();
        PyList list = new PyList();
        int count = 0;
        do {
            String line = file.readline(-1);
            int len = line.length();
            if (len == 0) {
                // EOF
                break;
            }
            count += len;
            list.append(new PyString(line));
        } while (sizehint <= 0 || count < sizehint);
        return list;
    }

    public PyObject readlines(int sizehint) {
        return file_readlines(sizehint);
    }

    public PyObject readlines() {
        return file_readlines(0);
    }

    @Override
    public PyObject __iternext__() {
        return file___iternext__();
    }

    final synchronized PyObject file___iternext__() {
        checkClosed();
        String next = file.readline(-1);
        if (next.length() == 0) {
            return null;
        }
        return new PyString(next);
    }

    @ExposedMethod(doc = BuiltinDocs.file_next_doc)
    final PyObject file_next() {
        PyObject ret = file___iternext__();
        if (ret == null) {
            throw Py.StopIteration("");
        }
        return ret;
    }

    public PyObject next() {
        return file_next();
    }

    @ExposedMethod(names = {"__enter__", "__iter__", "xreadlines"},
                   doc = BuiltinDocs.file___iter___doc)
    final PyObject file_self() {
        checkClosed();
        return this;
    }

    public PyObject __enter__() {
        return file_self();
    }

    @Override
    public PyObject __iter__() {
        return file_self();
    }

    public PyObject xreadlines() {
        return file_self();
    }

    @ExposedMethod(doc = BuiltinDocs.file_write_doc)
    final void file_write(PyObject obj) {
        file_write(asWritable(obj, null));
    }

    final synchronized void file_write(String string) {
        checkClosed();
        softspace = false;
        file.write(string);
    }

    public void write(String string) {
        file_write(string);
    }

    @ExposedMethod(doc = BuiltinDocs.file_writelines_doc)
    final synchronized void file_writelines(PyObject lines) {
        checkClosed();
        PyObject iter = Py.iter(lines, "writelines() requires an iterable argument");
        for (PyObject item = null; (item = iter.__iternext__()) != null;) {
            checkClosed(); // ... in case a nasty iterable closed this file
            softspace = false;
            file.write(asWritable(item, "writelines() argument must be a sequence of strings"));
        }
    }

    public void writelines(PyObject lines) {
        file_writelines(lines);
    }

    /**
     * Return a String for writing to the underlying file from obj. This is a helper for {@link file_write}
     * and {@link file_writelines}.
     *
     * @param obj to write
     * @param message for TypeError if raised (or null for default message)
     * @return bytes representing the value (as a String in the Jython convention)
     */
    private String asWritable(PyObject obj, String message) {

        if (obj instanceof PyUnicode) {
            // Unicode must be encoded into bytes (null arguments here invoke the default values)
            return ((PyUnicode)obj).encode(encoding, errors);

        } else if (obj instanceof PyString) {
            // Take a short cut
            return ((PyString)obj).getString();

        } else if (obj instanceof PyArray && !binary) {
            // Fall through to TypeError. (If binary, BufferProtocol takes care of PyArray.)

        } else {
            // Try to get a simple byte-oriented buffer
            try (PyBuffer buf = ((BufferProtocol)obj).getBuffer(PyBUF.SIMPLE)) {
                // ... and treat those bytes as a String
                return buf.toString();
            } catch (ClassCastException e) {
                // Does not implement BufferProtocol (in reality). Fall through to message.
            }
        }

        if (message == null) {
            // Messages differ for text or binary streams (CPython) but we always add the type
            String fmt = "expected a string or%s buffer, not %.200s";
            message = String.format(fmt, (binary ? "" : " character"), obj.getType().fastGetName());
        }
        throw Py.TypeError(message);
    }

    @ExposedMethod(doc = BuiltinDocs.file_tell_doc)
    final synchronized long file_tell() {
        checkClosed();
        return file.tell();
    }

    public long tell() {
        return file_tell();
    }

    @ExposedMethod(defaults = {"0"}, doc = BuiltinDocs.file_seek_doc)
    final synchronized void file_seek(long pos, int how) {
        checkClosed();
        file.seek(pos, how);
    }

    public void seek(long pos, int how) {
        file_seek(pos, how);
    }

    public void seek(long pos) {
        file_seek(pos, 0);
    }

    @ExposedMethod(doc = BuiltinDocs.file_flush_doc)
    final synchronized void file_flush() {
        checkClosed();
        file.flush();
    }

    public void flush() {
        file_flush();
    }

    @ExposedMethod(doc = BuiltinDocs.file_close_doc)
    final synchronized void file_close() {
        if (closer != null) {
            closer.close();
            closer = null;
        } else {
            file.close();
        }
    }

    public void close() {
        file_close();
    }

    @ExposedMethod(doc = BuiltinDocs.file___exit___doc)
    final void file___exit__(PyObject type, PyObject value, PyObject traceback) {
        close();
    }

    public void __exit__(PyObject type, PyObject value, PyObject traceback) {
        file___exit__(type, value, traceback);
    }

    @ExposedMethod(defaults = {"null"}, doc = BuiltinDocs.file_truncate_doc)
    final void file_truncate(PyObject position) {
        if (position == null) {
            file_truncate();
            return;
        }
        file_truncate(position.asLong());
    }

    final synchronized void file_truncate(long position) {
        file.truncate(position);
    }

    public void truncate(long position) {
        file_truncate(position);
    }

    final synchronized void file_truncate() {
        file.truncate(file.tell());
    }

    public void truncate() {
        file_truncate();
    }

    public boolean isatty() {
        return file_isatty();
    }

    @ExposedMethod(doc = BuiltinDocs.file_isatty_doc)
    final boolean file_isatty() {
        return file.isatty();
    }

    public PyObject fileno() {
        return file_fileno();
    }

    @ExposedMethod(doc = BuiltinDocs.file_fileno_doc)
    final PyObject file_fileno() {
        return PyJavaType.wrapJavaObject(file.fileno());
    }

    @ExposedMethod(names = {"__str__", "__repr__"}, doc = BuiltinDocs.file___str___doc)
    final String file_toString() {
        String state = file.closed() ? "closed" : "open";
        String id = Py.idstr(this);
        String escapedName;
        if (name instanceof PyUnicode) {
            // unicode: always uses the format u'%s', and the escaped value thus:
            escapedName = "u'"+PyString.encode_UnicodeEscape(name.toString(), false)+"'";
        } else {
            // anything else: uses repr(), which for str (common case) is smartly quoted
            escapedName = name.__repr__().getString();
        }
        return String.format("<%s file %s, mode '%s' at %s>", state, escapedName, mode, id);
    }

    @Override
    public String toString() {
        return file_toString();
    }

    private void checkClosed() {
        file.checkClosed();
    }

    @ExposedGet(name = "closed", doc = BuiltinDocs.file_closed_doc)
    public boolean getClosed() {
        return file.closed();
    }

    @ExposedGet(name = "newlines", doc = BuiltinDocs.file_newlines_doc)
    public PyObject getNewlines() {
        return file.getNewlines();
    }

    @ExposedGet(name = "softspace", doc = BuiltinDocs.file_softspace_doc)
    public PyObject getSoftspace() {
        // NOTE: not actual bools because CPython is this way
        return softspace ? Py.One : Py.Zero;
    }

    @ExposedSet(name = "softspace")
    public void setSoftspace(PyObject obj) {
        softspace = obj.__nonzero__();
    }

    @ExposedDelete(name = "softspace")
    public void delSoftspace() {
        throw Py.TypeError("can't delete numeric/char attribute");
    }

    @Override
    public Object __tojava__(Class<?> cls) {
        Object obj = null;
        if (InputStream.class.isAssignableFrom(cls)) {
            obj = file.asInputStream();
        } else if (OutputStream.class.isAssignableFrom(cls)) {
            obj = file.asOutputStream();
        }
        if (obj == null) {
            obj = super.__tojava__(cls);
        }
        return obj;
    }

    @Override
    public void __del_builtin__() {
        if (closer != null) {
            closer.close();
        }
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
        private final TextIOBase file;
        private PySystemState sys;

        public Closer(TextIOBase file, PySystemState sys) {
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
        @Override
        public Void call() {
            file.close();
            sys = null;
            return null;
        }

    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return name == null ? 0 : visit.visit(name, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == name;
    }
}
