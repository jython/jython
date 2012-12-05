/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.core.imp;

/**
 * The Python _io module.
 */
public class _io implements ClassDictInit {

    public static final PyString __doc__ = new PyString("Java implementation of _io.");

    /**
     * This method is called when the module is loaded, to populate the namespace (dictionary) of
     * the module. The dictionary has been initialised at this point reflectively from the methods
     * of this class and this method nulls those entries that ought not to be exposed.
     *
     * @param dict namespace of the module
     */
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_io"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("FileIO", PyFileIO.TYPE);

        // Define UnsupportedOperation exception by constructing the type

        PyObject exceptions = imp.load("exceptions");
        PyObject ValueError = exceptions.__getattr__("ValueError");
        PyObject IOError = exceptions.__getattr__("IOError");
        // Equivalent to class UnsupportedOperation(ValueError, IOError) : pass
        // UnsupportedOperation = makeException(dict, "UnsupportedOperation", ValueError, IOError);
        // XXX Work-around: slots not properly initialised unless IOError comes first
        UnsupportedOperation = makeException(dict, "UnsupportedOperation", IOError, ValueError);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("makeException", null);
    }

    /** A Python class for the <code>UnsupportedOperation</code> exception. */
    public static PyType UnsupportedOperation;

    /**
     * A function that returns a {@link PyException}, which is a Java exception suitable for
     * throwing, and that will be raised as an <code>UnsupportedOperation</code> Python exception.
     *
     * @param message text message parameter to the Python exception
     * @return nascent <code>UnsupportedOperation</code> Python exception
     */
    public static PyException UnsupportedOperation(String message) {
        return new PyException(UnsupportedOperation, message);
    }

    /**
     * Convenience method for constructing a type object of a Python exception, named as given, and
     * added to the namespace of the "_io" module.
     *
     * @param dict module dictionary
     * @param excname name of the exception
     * @param bases one or more bases (superclasses)
     * @return the constructed exception type
     */
    private static PyType makeException(PyObject dict, String excname, PyObject... bases) {
        PyStringMap classDict = new PyStringMap();
        classDict.__setitem__("__module__", Py.newString("_io"));
        PyType type = (PyType)Py.makeClass(excname, bases, classDict);
        dict.__setitem__(excname, type);
        return type;
    }

}
