// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * A class representing the singleton None object,
 */
@ExposedType(name="NoneType")
public class PyNone extends PyObject implements Serializable
{

    public static final PyType TYPE = PyType.fromClass(PyNone.class);

    PyNone() {
        super(TYPE);
    }

    private Object writeReplace() {
        return new Py.SingletonResolver("None");
    }    
    
    public boolean __nonzero__() {
        return NoneType___nonzero__();
    }

    @ExposedMethod
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

    @ExposedMethod(names="__repr__")
    final String NoneType_toString() {
        return "None";
    }

    public boolean isMappingType() {
        return false;
    }

    public boolean isSequenceType() {
        return false;
    }

    public boolean isNumberType() {
        return false;
    }

    public String asStringOrNull(int index) {
        return null;
    }

    public String asStringOrNull() {
        return null;
    }
}
