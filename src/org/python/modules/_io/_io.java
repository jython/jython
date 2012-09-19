/* Copyright (c) Jython Developers */
package org.python.modules._io;

import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * The Python _fileio module.
 */
public class _io implements ClassDictInit {

    public static final PyString __doc__ = new PyString("Fast implementation of io.FileIO.");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_io"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("FileIO", PyFileIO.TYPE);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

}
