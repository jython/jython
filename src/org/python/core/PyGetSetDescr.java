package org.python.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class PyGetSetDescr extends PyDataDescr {

    public PyGetSetDescr(String name, Class c, String get, String set) {
        this(PyType.fromClass(c), name, c, get, set, null);
    }

    public PyGetSetDescr(String name, Class c, String get, String set, String del) {
        this(PyType.fromClass(c), name, c, get, set, del);
    }

    public PyGetSetDescr(PyType dtype, String name, Class c, String get, String set) {
        this(dtype, name, c, get, set, null);
    }

    public PyGetSetDescr(PyType dtype, String name, Class c, String get, String set, String del) {
        super(dtype, name, getMethod(c, get).getReturnType());
        this.name = name;
        this.dtype = dtype;
        getMeth = getMethod(c, get);
        if(set != null) {
            setMeth = getMethod(c, set, ofType);
        }
        if(del != null) {
            delMeth = getMethod(c, del);
        }
    }

    private static Method getMethod(Class onClass, String name, Class... params) {
        Method meth;
        try {
            meth = onClass.getMethod(name, params);
        } catch(NoSuchMethodException e) {
            throw Py.SystemError("method '" + name + "' doesn't exist on '" + onClass.getName()
                    + "'");
        }
        if(Modifier.isStatic(meth.getModifiers())) {
            throw Py.SystemError("static '" + name + "' not supported on '" + onClass.getName()
                    + "'");
        }
        return meth;
    }

    @Override
    public Object invokeGet(PyObject obj) {
        try {
            return getMeth.invoke(obj);
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e);
        } catch(InvocationTargetException e) {
            throw Py.JavaError(e);
        }
    }

    @Override
    public void invokeSet(PyObject obj, Object converted) {
        try {
            setMeth.invoke(obj, converted);
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e);
        } catch(InvocationTargetException e) {
            throw Py.JavaError(e);
        }
    }

    @Override
    public void invokeDel(PyObject obj) {
        try {
            delMeth.invoke(obj, new Object[0]);
        } catch(IllegalArgumentException e) {
            throw Py.JavaError(e);
        } catch(IllegalAccessException e) {
            throw Py.JavaError(e);
        } catch(InvocationTargetException e) {
            throw Py.JavaError(e);
        }
    }

    @Override
    public boolean implementsDescrSet() {
        return setMeth != null;
    }

    @Override
    public boolean implementsDescrDelete() {
        return delMeth != null;
    }

    private Method getMeth;

    private Method setMeth;

    private Method delMeth;
}
