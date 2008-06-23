package org.python.core;

import java.io.Serializable;

public class PyNotImplemented extends PySingleton implements Serializable
{
    PyNotImplemented() {
        super("NotImplemented");
    }

    public Object __tojava__(Class c) {
        // Danger here. java.lang.Object gets null not None
        if (c == PyObject.class) {
            return this;
        }
        if (c.isPrimitive()) {
            return Py.NoConversion;
        }
        return null;
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }


    private Object writeReplace() {
        return new Py.SingletonResolver("NotImplemented");
    }
    
}

