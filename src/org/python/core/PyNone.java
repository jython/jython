// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

import org.python.expose.Exposed;

/**
 * A class representing the singleton None object,
 */
@Exposed(name="NoneType")
public class PyNone extends PyObject implements Serializable
{

    private static final PyType NONETYPE = PyType.fromClass(PyNone.class);

    PyNone() {
        super(NONETYPE);
    }

    private Object writeReplace() {
        return new Py.SingletonResolver("None");
    }    
    
    public boolean __nonzero__() {
        return NoneType___nonzero__();
    }

    @Exposed
    final boolean NoneType___nonzero__() {
        return false;
    }

    public Object __tojava__(Class c) {
        //Danger here.  java.lang.Object gets null not None
        if (c == PyObject.class)
            return this;
        if (c.isPrimitive())
            return Py.NoConversion;
        return null;
    }

    public String toString() throws PyIgnoreMethodTag {
        return NoneType_toString();
    }

    @Exposed(name="__repr__")
    final String NoneType_toString() {
        return "None";
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }
    public boolean isNumberType() { return false; }
    public String asStringOrNull(int index) { return null; }
}
