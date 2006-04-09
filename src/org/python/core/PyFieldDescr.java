package org.python.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PyFieldDescr extends PyDescriptor {

    private Field field;
    private Class field_type;
    private boolean readonly;

    public PyFieldDescr(String name, Class c, String field_name) {
        this(name, c, field_name, false);
    }

    public PyFieldDescr(
        String name,
        Class c,
        String field_name,
        boolean readonly) {
        this.name = name;
        this.dtype = PyType.fromClass(c);
        try {
            field = c.getField(field_name);
        } catch (NoSuchFieldException e) {
            throw Py.SystemError("bogus attribute spec");
        }
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw Py.SystemError("static attributes not supported");
        }
        this.readonly = readonly || Modifier.isFinal(modifiers);
        field_type = field.getType();

    }

    public String toString() {
        return "<member '" + name + "' of '"+dtype.fastGetName()+"' objects>";
    }

    /**
     * @see org.python.core.PyObject#__get__(org.python.core.PyObject, org.python.core.PyObject)
     */
    public PyObject __get__(PyObject obj, PyObject type) {
        try {
            if (obj != null) {
                PyType objtype = obj.getType();
                if (objtype != dtype && !objtype.isSubType(dtype))
                    throw get_wrongtype(objtype);
                return Py.java2py(field.get(obj));
            }
            return this;
        } catch (IllegalArgumentException e) {
            throw Py.JavaError(e);

        } catch (IllegalAccessException e) {
            throw Py.JavaError(e); // unexpected
        }
    }

    /**
     * @see org.python.core.PyObject#__set__(org.python.core.PyObject, org.python.core.PyObject)
     */
    public void __set__(PyObject obj, PyObject value) {
        try {
            // obj != null
            PyType objtype = obj.getType();
            if (objtype != dtype && !objtype.isSubType(dtype))
                throw get_wrongtype(objtype);
            Object converted = value.__tojava__(field_type);
            if (converted == Py.NoConversion) {
                throw Py.TypeError(""); // xxx
            }
            field.set(obj, converted);
        } catch (IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch (IllegalAccessException e) {
            throw Py.JavaError(e); // unexpected
        }
    }

    /**
     * @see org.python.core.PyObject#implementsDescrSet()
     */
    public boolean implementsDescrSet() {
        return !readonly;
    }

    /**
     * @see org.python.core.PyObject#isDataDescr()
     */
    public boolean isDataDescr() {
        return true;
    }

}
