// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.lang.reflect.*;

public class PyBeanProperty extends PyReflectedField {
    public Method getMethod, setMethod;
    public Class myType;
    String __name__;

    public PyBeanProperty(String name, Class myType,
                          Method getMethod, Method setMethod)
    {
        __name__ = name;
        this.getMethod = getMethod;
        this.setMethod = setMethod;
        this.myType = myType;
    }

    public PyObject _doget(PyObject self) {
        if (self == null) {
            if (field != null) {
                return super._doget(null);
            }
            throw Py.AttributeError("instance attr: "+__name__);
        }

        if (getMethod == null) {
            throw Py.AttributeError("write-only attr: "+__name__);
        }

        Object iself = Py.tojava(self, getMethod.getDeclaringClass());

        try {
            Object value = getMethod.invoke(iself, Py.EmptyObjects);
            return Py.java2py(value);
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
    }

    public boolean _doset(PyObject self, PyObject value) {
        if (self == null) {
            if (field != null) {
                return super._doset(null, value);
            }
            throw Py.AttributeError("instance attr: "+__name__);
        }

        if (setMethod == null) {
            throw Py.AttributeError("read-only attr: "+__name__);
        }

        Object iself = Py.tojava(self, setMethod.getDeclaringClass());

        // Special handling of tuples - try to call a class constructor
        if (value instanceof PyTuple) {
            try {
                PyTuple vtup = (PyTuple)value;
                value = PyJavaClass.lookup(myType).__call__(vtup.getArray()); // xxx PyObject subclasses
            } catch (Throwable t) {
                // If something goes wrong ignore it?
            }
        }
        Object jvalue = Py.tojava(value, myType);

        try {
            setMethod.invoke(iself, new Object[] {jvalue});
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        return true;
    }

    public PyBeanProperty copy() {
        return new PyBeanProperty(__name__, myType, getMethod, setMethod);
    }

    public String toString() {
        String typeName = "unknown";
        if (myType != null) {
            typeName = myType.getName();
        }
        return "<beanProperty "+__name__+" type: "+typeName+" "+
            Py.idstr(this)+">";
    }
}

