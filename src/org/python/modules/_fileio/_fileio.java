/* Copyright (c) Jython Developers */
package org.python.modules._fileio;

import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * The Python _fileio module.
 */
public class _fileio implements ClassDictInit {

    public static final PyString __doc__ = new PyString("Fast implementation of io.FileIO.");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_fileio"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("_FileIO", PyFileIO.TYPE);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

}
