package org.python.expose;

import org.python.core.PyObject;
import org.python.core.PyType;

/**
 * Contains the basic information needed to construct a builtin Python type.
 */
public interface TypeBuilder {

    public String getName();

    public PyObject getDict(PyType type);

    public Class getTypeClass();

    public Class getBase();
}
