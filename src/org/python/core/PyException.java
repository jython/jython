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
        if (isExceptionInstance(value)) {
            inClass = value.fastGetClass();
        }

        if (isExceptionClass(type)) {
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

    /**
     * Logic for the raise statement
     *
     * @param type the first arg to raise, a type or an instance
     * @param value the second arg, the instance of the class or
     * arguments to its constructor
     * @param tb a traceback object
     * @return a PyException wrapper
     */
    public static PyException doRaise(PyObject type, PyObject value, PyObject traceback) {
        if (type == null) {
            ThreadState state = Py.getThreadState();
            type = state.exception.type;
            value = state.exception.value;
            traceback = state.exception.traceback;
        }

        if (traceback == Py.None) {
            traceback = null;
        } else if (traceback != null && !(traceback instanceof PyTraceback)) {
            throw Py.TypeError("raise: arg 3 must be a traceback or None");
        }

        if (value == null) {
            value = Py.None;
        }

        // Repeatedly, replace a tuple exception with its first item
        while (type instanceof PyTuple && ((PyTuple)type).size() > 0) {
            type = type.__getitem__(0);
        }

        if (type.getType() == PyString.TYPE) {
            Py.warning(Py.DeprecationWarning, "raising a string exception is deprecated");
        } else if (isExceptionClass(type)) {
            PyException pye = new PyException(type, value, (PyTraceback)traceback);
            pye.normalize();
            return pye;
        } else if (isExceptionInstance(type)) {
            // Raising an instance.  The value should be a dummy.
            if (value != Py.None) {
                throw Py.TypeError("instance exception may not have a separate value");
            } else {
                // Normalize to raise <class>, <instance>
                value = type;
                type = type.fastGetClass();
            }
        } else {
            // Not something you can raise.  You get an exception
            // anyway, just not what you specified :-)
            throw Py.TypeError("exceptions must be classes, instances, or strings (deprecated), "
                               + "not " + type.getType().fastGetName());
        }
        return new PyException(type, value, (PyTraceback)traceback);
    }

    /**
     * Determine whether obj is a Python exception class
     *
     * @param obj a PyObject
     * @return true if an exception
     */
    public static boolean isExceptionClass(PyObject obj) {
        return obj instanceof PyClass
                || (obj instanceof PyType && ((PyType)obj).isSubType((PyType)Py.BaseException));
    }

    /**
     * Determine whether obj is an Python exception instance
     *
     * @param obj a PyObject
     * @return true if an exception instance
     */
    public static boolean isExceptionInstance(PyObject obj) {
        return obj instanceof PyInstance || obj.getType().isSubType((PyType)Py.BaseException);
    }

    /**
     * Get the name of the exception's class
     *
     * @param obj a PyObject exception
     * @return String exception name
     */
    public static String exceptionClassName(PyObject obj) {
        return obj instanceof PyClass ? ((PyClass)obj).__name__ : ((PyType)obj).fastGetName();
    }
}
