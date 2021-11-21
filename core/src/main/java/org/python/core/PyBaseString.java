package org.python.core;

import org.python.expose.ExposedType;

/**
 * base class for jython strings.
 */
@ExposedType(name = "basestring", base = PyObject.class, doc = BuiltinDocs.basestring_doc)
public abstract class PyBaseString extends PySequence implements CharSequence {
    
    public static final PyType TYPE = PyType.fromClass(PyBaseString.class);

    protected PyBaseString(PyType type) {
        super(type);
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }
}
