// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


public class PyReflectedField extends PyObject {
    public Field field;
    public static PyClass __class__;

    public PyReflectedField(PyClass c) {
        super(c);
    }

    public PyReflectedField(Field field) {
        super(__class__);
        this.field = field;
    }

    public PyObject _doget(PyObject self) {
        Object iself = null;
        if (!Modifier.isStatic(field.getModifiers())) {
            if (self == null)
                return this;
            iself = Py.tojava(self, field.getDeclaringClass());
        }
        Object value;

        try {
            value = field.get(iself);
        } catch (IllegalAccessException exc) {
            throw Py.JavaError(exc);
        }

        return Py.java2py(value);
    }

    public boolean _doset(PyObject self, PyObject value) {
        Object iself = null;
        if (!Modifier.isStatic(field.getModifiers())) {
            if (self == null) {
                throw Py.AttributeError("set instance variable as static: "+
                                        field.toString());
            }
            iself = Py.tojava(self, field.getDeclaringClass());
        }
        Object fvalue = Py.tojava(value, field.getType());

        try {
            field.set(iself, fvalue);
        } catch (IllegalAccessException exc) {
            throw Py.JavaError(exc);
        }
        return true;
    }

    public String toString() {
        return "<reflected field "+field.toString()+" at "+Py.id(this)+">";
    }
}
