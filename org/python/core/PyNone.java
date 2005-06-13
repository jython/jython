// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A class representing the singleton None object,
 */
public class PyNone extends PySingleton
{
    PyNone() {
        super("None");
    }

    public boolean __nonzero__() {
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

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'None' object";
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }
    public boolean isNumberType() { return false; }
    public String asStringOrNull(int index) { return null; }
}
