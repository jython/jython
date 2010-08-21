// A file-like object for writing to java.io.Writer objects;
// only to be used for stdout, stderr in PythonInterpreter#setOut(Writer), #setErr(Writer)
// (for backwards compatibility and JSR 223 support)
//
// no attempts to close etc at shutdown are done for this object (unlike PyFile),
// since again just for PythonInterpreter
// nor is this exposed as a type
package org.python.core;

import java.io.Writer;
import java.io.IOException;

public class PyFileWriter extends PyObject {

    private final Writer writer;
    private boolean closed;
    public boolean softspace = false;

    public PyFileWriter(Writer writer) {
        this.writer = writer;
        closed = false;
    }

    public boolean closed() {
        return closed;
    }

    public void checkClosed() {
        if (closed()) {
            throw Py.ValueError("I/O operation on closed file");
        }
    }

    public synchronized void flush() {
                checkClosed();
        try {
            writer.flush();
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    public void close() {
        try {
            writer.close();
            closed = true;
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    public void write(PyObject o) {
        if (o instanceof PyUnicode) {
            write(((PyUnicode) o).getString());
        } else if (o instanceof PyString) {
            write(((PyString) o).getString());
        } else {
            throw Py.TypeError("write requires a string as its argument");
        }
    }

    public synchronized void write(String s) {
        checkClosed();
        try {
            writer.write(s);
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    public synchronized void writelines(PyObject a) {
        checkClosed();
        PyObject iter = Py.iter(a, "writelines() requires an iterable argument");

        PyObject item = null;
        while ((item = iter.__iternext__()) != null) {
            if (!(item instanceof PyString)) {
                throw Py.TypeError("writelines() argument must be a sequence of strings");
            }
            write(item);
        }
    }
}
