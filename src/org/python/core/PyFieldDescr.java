package org.python.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PyFieldDescr extends PyDataDescr {

    public PyFieldDescr(String name, Class c, String field_name) {
        this(name, c, field_name, false);
    }

    public PyFieldDescr(String name, Class c, String field_name, boolean readonly) {
        super(PyType.fromClass(c), name, get(c, field_name).getType());
        field = get(c, field_name);
        this.readonly = readonly || Modifier.isFinal(field.getModifiers());
    }

    private static Field get(Class onClass, String name) {
        Field f;
        try {
            f = onClass.getField(name);
        } catch(NoSuchFieldException e) {
            throw Py.SystemError("bogus attribute spec");
        }
        if(Modifier.isStatic(f.getModifiers())) {
            throw Py.SystemError("static attributes not supported");
        }
        return f;
    }

    @Override
    public Object invokeGet(PyObject obj) {
        try {
            return field.get(obj);
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e);
        }
    }

    @Override
    public void invokeSet(PyObject obj, Object converted) {
        try {
            field.set(obj, converted);
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e);
        }
    }

    public boolean implementsDescrSet() {
        return !readonly;
    }

    private Field field;

    private boolean readonly;
}
