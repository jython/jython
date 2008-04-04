// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.io.*;

/**
 * A wrapper for all python exception. Note that the wellknown
 * python exception are <b>not</b> subclasses of PyException.
 * Instead the python exception class is stored in the
 * <code>type</code> field and value or class instance is stored
 * in the <code>value</code> field.
 */

public class PyException extends RuntimeException
{
    /**
     * The python exception class (for class exception) or
     * identifier (for string exception).
     */
    public PyObject type;

    /**
     * The exception instance (for class exception) or exception
     * value (for string exception).
     */
    public PyObject value = Py.None;

    public PyTraceback traceback;

    private boolean normalized = false;

    public PyException() {
        //System.out.println("PyException");
        //super.printStackTrace();
        this(Py.None, Py.None);
    }

    public PyException(PyObject type) {
        this(type, Py.None);
    }

    public PyException(PyObject type, PyObject value) {
        this(type, value, null);
    }

    public PyException(PyObject type, PyObject value, PyTraceback traceback) {
        this.type = type;
        this.value = value;

        if (traceback == null) {
            PyFrame frame = Py.getFrame();
            traceback = new PyTraceback(frame);
            if (frame != null && frame.tracefunc != null) {
                frame.tracefunc = frame.tracefunc.traceException(frame, this);
            }
        }
        this.traceback = traceback;
    }

    public PyException(PyObject type, String value) {
        this(type, new PyString(value));
    }

    private boolean printingStackTrace = false;
    public void printStackTrace() {
        Py.printException(this);
    }

    public synchronized void printStackTrace(PrintStream s) {
        //System.err.println("printStackTrace: "+s+", "+printingStackTrace);
        if (printingStackTrace) {
            super.printStackTrace(s);
        } else {
            try {
                printingStackTrace = true;
                Py.displayException(type, value, traceback, new PyFile(s));
            } finally {
                printingStackTrace = false;
            }
        }
    }

    public synchronized void super__printStackTrace(PrintWriter w) {
        try {
            printingStackTrace = true;
            super.printStackTrace(w);
        } finally {
            printingStackTrace = false;
        }
        //Py.printException(this, null, new PyFile(s));
    }

    public synchronized String toString() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (!printingStackTrace) {
            printStackTrace(new PrintStream(buf));
        }
        return buf.toString();
    }

    /**
     * Instantiates the exception value if it is not already an
     * instance.
     *
     */
    public void normalize() {
        if (normalized) {
            return;
        }
        PyObject inClass = null;
        if (Py.isExceptionInstance(value)) {
            inClass = value.fastGetClass();
        }

        if (Py.isExceptionClass(type)) {
            if (inClass == null || !__builtin__.issubclass(inClass, type)) {
                PyObject[] args;

                // Don't decouple a tuple into args when it's a
                // KeyError, pass it on through below
                if (value == Py.None) {
                    args = Py.EmptyObjects;
                } else if (value instanceof PyTuple && type != Py.KeyError) {
                    args = ((PyTuple)value).getArray();
                } else {
                    args = new PyObject[] {value};
                }

                value = type.__call__(args);
            } else if (inClass != type) {
                type = inClass;
            }
        }
        normalized = true;
    }
}
