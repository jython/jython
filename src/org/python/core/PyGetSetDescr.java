package org.python.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class PyGetSetDescr extends PyDescriptor {

    private Method get_meth;

    private Method set_meth;

    private Class getset_type;

    public PyGetSetDescr(PyType dtype,
                         String name,
                         Class c,
                         String get,
                         String set) {
        this.name = name;
        this.dtype = dtype;
        try {
            get_meth = c.getMethod(get, new Class[] {});
        } catch(NoSuchMethodException e) {
            throw Py.SystemError("bogus getset spec");
        }
        if(Modifier.isStatic(get_meth.getModifiers()))
            throw Py.SystemError("static getset not supported");
        getset_type = get_meth.getReturnType();
        if(set != null) {
            try {
                set_meth = c.getMethod(set, new Class[] {getset_type});
            } catch(NoSuchMethodException e) {
                throw Py.SystemError("bogus getset spec");
            }
            if(Modifier.isStatic(set_meth.getModifiers()))
                throw Py.SystemError("static getset not supported");
        }
    }

    public PyGetSetDescr(String name, Class c, String get, String set) {
        this(PyType.fromClass(c), name, c, get, set);
    }

    public String toString() {
        return "<attribute '" + name + "' of '" + dtype.fastGetName()
                + "' objects>";
    }

    /**
     * @see org.python.core.PyObject#__get__(org.python.core.PyObject,
     *      org.python.core.PyObject)
     */
    public PyObject __get__(PyObject obj, PyObject type) {
        try {
            if(obj != null) {
                PyType objtype = obj.getType();
                if(objtype != dtype && !objtype.isSubType(dtype))
                    throw get_wrongtype(objtype);
                Object v = get_meth.invoke(obj, new Object[0]);
                if(v == null) {
                    obj.noAttributeError(name);
                }
                return Py.java2py(v);
            }
            return this;
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e); // unexpected
        } catch(InvocationTargetException e) {
            throw Py.JavaError(e);
        }
    }

    /**
     * @see org.python.core.PyObject#__set__(org.python.core.PyObject,
     *      org.python.core.PyObject)
     */
    public void __set__(PyObject obj, PyObject value) {
        try {
            // obj != null
            PyType objtype = obj.getType();
            if(objtype != dtype && !objtype.isSubType(dtype))
                throw get_wrongtype(objtype);
            Object converted = value.__tojava__(getset_type);
            if(converted == Py.NoConversion) {
                throw Py.TypeError(""); // xxx
            }
            set_meth.invoke(obj, new Object[] {converted});
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e); // unexpected
        } catch(InvocationTargetException e) {
            throw Py.JavaError(e);
        }
    }

    /**
     * @see org.python.core.PyObject#implementsDescrSet()
     */
    public boolean implementsDescrSet() {
        return set_meth != null;
    }

    /**
     * @see org.python.core.PyObject#isDataDescr()
     */
    public boolean isDataDescr() {
        return true;
    }
}
