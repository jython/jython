// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

import org.python.expose.ExposedType;

/**
 * A class representing the singleton Ellipsis <code>...</code> object.
 */
@ExposedType(name = "ellipsis", base = PyObject.class, isBaseType = false)
public class PyEllipsis extends PySingleton implements Serializable {

    public static final PyType TYPE = PyType.fromClass(PyEllipsis.class);

    PyEllipsis() {
        super("Ellipsis");
    }

    private Object writeReplace() {
        return new Py.SingletonResolver("Ellipsis");
    }
}
