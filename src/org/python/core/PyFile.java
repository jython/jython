// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.LinkedList;

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
import org.python.expose.MethodType;

/**
 * A python file wrapper around a java stream, reader/writer or file.
 */
@ExposedType(name = "file")
public class PyFile extends PyObject {
    /** The filename */
    @ExposedGet
    public PyObject name;

    /** The mode string */
    @ExposedGet
    public String mode;

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

    /** All PyFiles' closers */
    private static LinkedList closers = new LinkedList();

    static {
        initCloser();
    }

    public PyFile() {
    }

    public PyFile(PyType subType) {
        super(subType);
    }

    public PyFile(RawIOBase raw, String name, String mode, int bufsize) {
        parseMode(mode);
        file___init__(raw, name, mode, bufsize);
    }

    public PyFile(InputStream istream, String name, String mode, int bufsize,
                  boolean closefd) {
        parseMode(mode);
        file___init__(new StreamIO(istream, closefd), name, mode, bufsize);
    }

    public PyFile(InputStream istream, String name, String mode, int bufsize) {
        this(istream, name, mode, -1, true);
    }

    public PyFile(InputStream istream, String name, String mode) {
        this(istream, name, mode, -1);
    }

    public PyFile(InputStream istream, String name) {
        this(istream, name, "r");
    }

    public PyFile(InputStream istream) {
        this(istream, "<???>", "r");
    }

    public PyFile(OutputStream ostream, String name, String mode, int bufsize, boolean closefd) {
        parseMode(mode);
        file___init__(new StreamIO(ostream, closefd), name, mode, bufsize);
    }

    public PyFile(OutputStream ostream, String name, String mode, int bufsize) {
        this(ostream, name, mode, -1, true);
    }

    public PyFile(OutputStream ostream, String name, String mode) {
        this(ostream, name, mode, -1);
    }

    public PyFile(OutputStream ostream, String name) {
        this(ostream, name, "w");
    }

    public PyFile(OutputStream ostream) {
        this(ostream, "<???>", "w");
    }

    public PyFile(RandomAccessFile file, String name, String mode, int bufsize) {
        file___init__(new FileIO(file.getChannel(), parseMode(mode)), name, mode, bufsize);
    }

    public PyFile(RandomAccessFile file, String name, String mode) {
        this(file, name, mode, -1);
    }

    public PyFile(RandomAccessFile file, String name) {
        this(file, name, "r+");
    }

    public PyFile(RandomAccessFile file) {
        this(file, "<???>", "r+");
    }

    public PyFile(String name, String mode, int bufsize) {
        file___init__(new FileIO(name, parseMode(mode)), name, mode, bufsize);
    }

    @ExposedNew
    static final PyObject file_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[]args,
                                   String[]keywords) {
        PyFile newFile;
        if (new_.for_type == subtype) {
            if (init) {
                if (args.length == 0) {
                    newFile = new PyFile();
                    newFile.file___init__(args, keywords);
                } else if (args[0] instanceof PyString ||
                           (args[0] instanceof PyJavaInstance &&
                            ((PyJavaInstance)args[0]).javaProxy == String.class)) {
                    // If first arg is a PyString or String, assume
                    // its being called as a builtin.
                    newFile = new PyFile();
                    newFile.file___init__(args, keywords);
                    newFile.closer = new Closer(newFile.file);
                } else {
                    // assume it's being called as a java class
                    PyJavaClass pjc = new PyJavaClass(PyFile.class);
                    newFile = (PyFile)pjc.__call__(args, keywords);
                }
            } else {
                newFile = new PyFile();
            }
        } else {
            newFile = new PyFileDerived(subtype);
        }
        return newFile;
    }

    @ExposedMethod
    final void file___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("file", args, kwds, new String[] {"name", "mode", "bufsize"},
                                     1);
        PyObject name = ap.getPyObject(0);
        if (!(name instanceof PyString)) {
            throw Py.TypeError("coercing to Unicode: need string, '" + name.getType().getFullName()
                               + "' type found");
        }
        String mode = ap.getString(1, "r");
        int bufsize = ap.getInt(2, -1);
        file___init__(new FileIO(name.toString(), parseMode(mode)), name, mode, bufsize);
    }

    private void file___init__(RawIOBase raw, String name, String mode, int bufsize) {
        file___init__(raw, new PyString(name), mode, bufsize);
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
            buffer = new BufferedReader(raw, lineBuffered ? 0 : bufsize);
        } else {
            // Should never happen
            throw Py.ValueError("unknown mode: '" + mode + "'");
        }
        return buffer;
    }

    /**
     * Parse and validate the python file mode, returning a cleaned
     * file mode suitable for FileIO.
     *
     * @param mode a python file mode String
     * @return a RandomAccessFile mode String
     */
    private String parseMode(String mode) {
        if (mode.length() == 0) {
            throw Py.ValueError("empty mode string");
        }

        String origMode = mode;
        if (mode.contains("U")) {
            universal = true;
            mode = mode.replace("U", "");
            if (mode.length() == 0) {
                mode = "r";
            } else if ("wa+".indexOf(mode.charAt(0)) > -1) {
                throw Py.ValueError("universal newline mode can only be used with modes starting "
                                    + "with 'r'");
            }
        }
        if ("rwa".indexOf(mode.charAt(0)) == -1) {
            throw Py.ValueError("mode string must begin with one of 'r', 'w', 'a' or 'U', not '"
                                + origMode + "'");
        }

        binary = mode.contains("b");
        reading = mode.contains("r");
        writing = mode.contains("w");
        appending = mode.contains("a");
        updating = mode.contains("+");

        return (reading ? "r" : "") + (writing ? "w" : "") + (appending ? "a" : "")
                + (updating ? "+" : "");
    }

    @ExposedMethod(defaults = {"-1"})
    final synchronized String file_read(int n) {
        checkClosed();
        return file.read(n);
    }

    public String read(int n) {
        return file_read(n);
    }

    public String read() {
        return file_read(-1);
    }

    @ExposedMethod
    final synchronized int file_readinto(PyObject buf) {
        checkClosed();
        return file.readinto(buf);
    }

    public int readinto(PyObject buf) {
        return file_readinto(buf);
    }

    @ExposedMethod(defaults = {"-1"})
    final synchronized String file_readline(int max) {
        checkClosed();
        return file.readline(max);
    }

    public String readline(int max) {
        return file_readline(max);
    }

    public String readline() {
        return file_readline(-1);
    }

    @ExposedMethod(defaults = {"0"})
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

    public PyObject __iter__() {
        return file___iter__();
    }

    @ExposedMethod
    final PyObject file___iter__() {
        checkClosed();
        return this;
    }

    public PyObject __iternext__() {
        return file___iternext__();
    }

    @ExposedMethod
    final synchronized PyObject file___iternext__() {
        checkClosed();
        String next = file.readline(-1);
        if (next.length() == 0) {
            return null;
        }
        return new PyString(next);
    }

    @ExposedMethod
    final PyObject file_next() {
        PyObject ret = __iternext__();
        if (ret == null) {
            throw Py.StopIteration("");
        }
        return ret;
    }

    public PyObject next() {
        return file_next();
    }

    @ExposedMethod
    final PyObject file_xreadlines() {
        checkClosed();
        return this;
    }

    public PyObject xreadlines() {
        return file_xreadlines();
    }

    @ExposedMethod
    final void file_write(PyObject o) {
        if (o instanceof PyUnicode) {
            // Call __str__ on unicode objects to encode them before writing
            file_write(o.__str__().string);
        } else if (o instanceof PyString) {
            file_write(((PyString)o).string);
        } else {
            throw Py.TypeError("write requires a string as its argument");
        }
    }

    final synchronized void file_write(String s) {
        checkClosed();
        softspace = false;
        file.write(s);
    }

    public void write(String s) {
        file_write(s);
    }

    @ExposedMethod
    final synchronized void file_writelines(PyObject a) {
        checkClosed();
        PyObject iter = Py.iter(a, "writelines() requires an iterable argument");

        PyObject item = null;
        while ((item = iter.__iternext__()) != null) {
            if (!(item instanceof PyString)) {
                throw Py.TypeError("writelines() argument must be a sequence of strings");
            }
            file.write(item.toString());
        }
    }

    public void writelines(PyObject a) {
        file_writelines(a);
    }

    @ExposedMethod
    final synchronized long file_tell() {
        checkClosed();
        return file.tell();
    }

    public long tell() {
        return file_tell();
    }

    @ExposedMethod(defaults = {"0"})
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

    @ExposedMethod
    final synchronized void file_flush() {
        checkClosed();
        file.flush();
    }

    public void flush() {
        file_flush();
    }

    @ExposedMethod
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

    @ExposedMethod(defaults = {"null"})
    final void file_truncate(PyObject position) {
        if (position == null) {
            file_truncate();
            return;
        }
        try {
            file_truncate(position.asLong(0));
        } catch (PyObject.ConversionException ce) {
            throw Py.TypeError("an integer is required");
        }
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

    @ExposedMethod
    final boolean file_isatty() {
        return file.isatty();
    }

    public PyObject fileno() {
        return file_fileno();
    }

    @ExposedMethod
    final PyObject file_fileno() {
        return new PyJavaInstance(file.fileno());
    }

    @ExposedMethod(names = {"__str__", "__repr__"})
    final String file_toString() {
        StringBuffer s = new StringBuffer("<");
        if (file.closed()) {
            s.append("closed ");
        } else {
            s.append("open ");
        }
        s.append("file ");
        s.append(name.__repr__());
        s.append(", mode '");
        s.append(mode);
        s.append("' ");
        s.append(Py.idstr(this));
        s.append(">");
        return s.toString();
    }

    public String toString() {
        return file_toString();
    }

    @ExposedMethod(type = MethodType.CMP)
    final int file___cmp__(PyObject o) {
        return super.__cmp__(o);
    }

    @ExposedMethod
    final boolean file___nonzero__() {
        return super.__nonzero__();
    }

    private void checkClosed() {
        file.checkClosed();
    }

    @ExposedGet(name = "closed")
    public boolean getClosed() {
        return file.closed();
    }

    @ExposedGet(name = "newlines")
    public PyObject getNewlines() {
        return file.getNewlines();
    }

    @ExposedGet(name = "softspace")
    public PyObject getSoftspace() {
        return softspace ? new PyInteger(1) : new PyInteger(0);
    }

    @ExposedSet(name = "softspace")
    public void setSoftspace(PyObject obj) {
        softspace = obj.__nonzero__();
    }

    @ExposedDelete(name = "softspace")
    public void delSoftspace() {
        throw Py.TypeError("can't delete numeric/char attribute");
    }

    public Object __tojava__(Class cls) {
        Object o = file.__tojava__(cls);
        if (o == null) {
            o = super.__tojava__(cls);
        }
        return o;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (closer != null) {
            closer.close();
        }
    }

    private static void initCloser() {
        try {
            Runtime.getRuntime().addShutdownHook(new PyFileCloser());
        } catch (SecurityException se) {
            Py.writeDebug("PyFile", "Can't register file closer hook");
        }
    }

    /**
     * A mechanism to make sure PyFiles are closed on exit. On
     * creation Closer adds itself to a list of Closers that will be
     * run by PyFileCloser on JVM shutdown. When a PyFile's close or
     * finalize methods are called, PyFile calls its Closer.close
     * which clears Closer out of the shutdown queue.
     *
     * We use a regular object here rather than WeakReferences and
     * their ilk as they may be collected before the shutdown hook
     * runs. There's no guarantee that finalize will be called during
     * shutdown, so we can't use it. It's vital that this Closer has
     * no reference to the PyFile it's closing so the PyFile remains
     * garbage collectable.
     */
    private static class Closer {

        /** The underlying file */
        private TextIOBase file;

        public Closer(TextIOBase file) {
            this.file = file;
            // Add ourselves to the queue of Closers to be run on shutdown
            synchronized (closers) {
                closers.add(this);
            }
        }

        public void close() {
            synchronized (closers) {
                if (!closers.remove(this)) {
                    return;
                }
            }
            doClose();
        }

        public void doClose() {
            file.close();
        }
    }

    private static class PyFileCloser extends Thread {

        public PyFileCloser() {
            super("Jython Shutdown File Closer");
        }

        public void run() {
            if (closers == null) {
                // closers can be null in some strange cases
                return;
            }
            synchronized (closers) {
                while (closers.size() > 0) {
                    try {
                        ((Closer)closers.removeFirst()).doClose();
                    } catch (PyException e) {
                        // continue
                    }
                }
            }
        }
    }

}
