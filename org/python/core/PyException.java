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
    private boolean instantiated=false;

    public void instantiate() {
        if (!instantiated) {
            if (type instanceof PyClass &&
                (!(value instanceof PyInstance &&
                   __builtin__.isinstance(value, (PyClass)type))))
            {
                //System.out.println("value: "+value);
                if (value instanceof PyTuple) {
                    value = ((PyClass)type).__call__(((PyTuple)value).list);
                } else {
                    if (value == Py.None) {
                        value = ((PyClass)type).__call__(Py.EmptyObjects);
                    } else {
                        value = ((PyClass)type).__call__(
                            new PyObject[] {value});
                    }
                }
            }
            instantiated = true;
        }
    }

    public PyException() {
        //System.out.println("PyException");
        //super.printStackTrace();
        this(Py.None, Py.None);
    }

    public PyException(PyObject type) {
        this(type, Py.None);
    }

    public PyException(PyObject type, PyObject value) {
        this.type = type;
        this.value = value;

        PyFrame frame = Py.getFrame();
        traceback = new PyTraceback(frame);
        if (frame != null && frame.tracefunc != null) {
            frame.tracefunc = frame.tracefunc.traceException(frame, this);
        }
    }

    public PyException(PyObject type, String value) {
        this(type, new PyString(value));
    }

    public PyException(PyObject type, PyObject value, PyTraceback traceback) {
        this.type = type;
        this.value = value;
        this.traceback = traceback;
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
}
