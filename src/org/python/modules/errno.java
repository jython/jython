/* Copyright (c) Jython Developers */
package org.python.modules;

import com.kenai.constantine.Constant;
import com.kenai.constantine.ConstantSet;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.imp;

/**
 * The Python errno module.
 *
 * Errno constants can be accessed from Java code via
 * {@link com.kenai.constantine.platform.Errno}, e.g. Errno.ENOENT.
 */
public class errno implements ClassDictInit {

    public static final PyString __doc__ = Py.newString(
        "This module makes available standard errno system symbols.\n\n"
        + "The value of each symbol is the corresponding integer value,\n"
        + "e.g., on most systems, errno.ENOENT equals the integer 2.\n\n"
        + "The dictionary errno.errorcode maps numeric codes to symbol names,\n"
        + "e.g., errno.errorcode[2] could be the string 'ENOENT'.\n\n"
        + "Symbols that are not relevant to the underlying system are not defined.\n\n"
        + "To map error codes to error messages, use the function os.strerror(),\n"
        + "e.g. os.strerror(2) could return 'No such file or directory'.");

    /** Reverse mapping of codes to names. */
    public static final PyObject errorcode = new PyDictionary();

    public static void classDictInit(PyObject dict) {
        for (Constant constant : ConstantSet.getConstantSet("Errno")) {
            addCode(dict, constant.name(), constant.value(), constant.toString());
        }
        // XXX: necessary?
        addCode(dict, "ESOCKISBLOCKING", 20000, "Socket is in blocking mode");
        addCode(dict, "EGETADDRINFOFAILED", 20001, "getaddrinfo failed");

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

    private static void addCode(PyObject dict, String name, int code, String message) {
        PyObject nameObj = Py.newString(name);
        PyObject codeObj = Py.newInteger(code);
        dict.__setitem__(nameObj, codeObj);
        errorcode.__setitem__(codeObj, nameObj);
    }

    /**
     * @deprecated Use org.python.core.constantine.Errno.valueOf(code).toString() (or
     *             os.strerror from Python) instead.
     */
    @Deprecated
    public static PyObject strerror(PyObject code) {
        Py.warning(Py.DeprecationWarning,
                   "The errno.strerror function is deprecated, use os.strerror.");
        return imp.load("os").__getattr__("strerror").__call__(code);
    }
}
