package org.python.core;

import org.python.expose.ExposedType;

/**
 * base class for jython strings.
 */
@ExposedType(name = "basestring", base = PyObject.class, doc = BuiltinDocs.basestring_doc)
public abstract class PyBaseString extends PySequence {
    
    public static final PyType TYPE = PyType.fromClass(PyBaseString.class);

    protected PyBaseString(PyType type) {
        super(type);
    }
}
