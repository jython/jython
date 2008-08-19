/* Copyright (c) 2007 Jython Developers */
package org.python.modules.zipimport;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.core.exceptions;

/**
 * This module adds the ability to import Python modules (*.py,
 * *$py.class) and packages from ZIP-format archives.
 *
 * @author Philip Jenvey
 */
public class zipimport implements ClassDictInit {

    public static final PyString __doc__ = new PyString(
        "zipimport provides support for importing Python modules from Zip archives.\n" +
        "\n" +
        "This module exports three objects:\n" +
        "- zipimporter: a class; its constructor takes a path to a Zip archive.\n" +
        "- ZipImportError: exception raised by zipimporter objects. It's a\n" +
        "subclass of ImportError, so it can be caught as ImportError, too.\n" +
        "- _zip_directory_cache: a dict, mapping archive paths to zip directory\n" +
        "info dicts, as used in zipimporter._files.\n" +
        "\n" +
        "It is usually not needed to use the zipimport module explicitly; it is\n" +
        "used by the builtin import mechanism for sys.path items that are paths\n" +
        "to Zip archives.");

    public static PyObject ZipImportError;
    public static PyException ZipImportError(String message) {
        return new PyException(ZipImportError, message);
    }

    // XXX: Ideally this cache would be per PySystemState
    public static PyDictionary _zip_directory_cache = new PyDictionary();

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("zipimport"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("zipimporter", zipimporter.TYPE);
        dict.__setitem__("_zip_directory_cache", _zip_directory_cache);
        dict.__setitem__("ZipImportError", ZipImportError);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("initClassExceptions", null);
    }

    /**
     * Initialize the ZipImportError exception during startup
     *
     */
    public static void initClassExceptions(PyObject exceptions) {
        PyObject ImportError = exceptions.__finditem__("ImportError");
        ZipImportError = Py.makeClass("zipimport.ZipImportError", ImportError,
                                      new PyStringMap() {{
                                          __setitem__("__module__", Py.newString("zipimport"));
                                      }});
    }
}
