package org.python.expose;

import org.python.core.PyObject;

/**
 * Contains the basic information needed to construct a builtin Python type.
 */
public interface TypeBuilder {

    public String getName();

    public PyObject getDict();

    public Class getTypeClass();
}
