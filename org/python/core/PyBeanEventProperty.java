// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.*;
import java.util.Hashtable;
import java.io.*;

public class PyBeanEventProperty extends PyReflectedField
{
    public Method addMethod;
    public String eventName;
    public Class eventClass;
    public String __name__;

    public PyBeanEventProperty(String eventName, Class eventClass,
                               Method addMethod, Method eventMethod)
    {
        __name__ = eventMethod.getName().intern();
        this.addMethod = addMethod;
        this.eventName = eventName;
        this.eventClass = eventClass;
    }

    public PyObject _doget(PyObject self) {
        if (self == null)
            return this;

        initAdapter();

        Object jself = Py.tojava(self, addMethod.getDeclaringClass());

        Object field;
        try {
            field = adapterField.get(getAdapter(jself));
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        PyCompoundCallable func;
        if (field == null) {
            func = new PyCompoundCallable();
            setFunction(jself, func);
            return func;
        }
        if (field instanceof PyCompoundCallable)
            return (PyCompoundCallable)field;

        func = new PyCompoundCallable();
        setFunction(jself, func);
        func.append((PyObject)field);
        return func;
    }

    private synchronized static Class getAdapterClass(Class c) {
        // System.err.println("getting adapter for: "+c+", "+c.getName());
        InternalTables tbl=PyJavaClass.getInternalTables();
        Object o = tbl.getAdapterClass(c);
        if (o != null)
            return (Class)o;
        Class pc = Py.findClass("org.python.proxies."+c.getName()+"$Adapter");
        if (pc == null) {
            //System.err.println("adapter not found for: "+
            //                   "org.python.proxies."+
            //                   c.getName()+"$Adapter");
            pc = MakeProxies.makeAdapter(c);
        }
        tbl.putAdapterClass(c, pc);
        return pc;
    }

    private synchronized Object getAdapter(Object self) {
        InternalTables tbl=PyJavaClass.getInternalTables();
        String eventClassName = eventClass.getName();

        Object adapter = tbl.getAdapter(self, eventClassName);
        if (adapter != null)
            return adapter;

        try {
            adapter = adapterClass.newInstance();
            addMethod.invoke(self, new Object[] {adapter});
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        tbl.putAdapter(self, eventClassName, adapter);
        return adapter;
    }

    private Field adapterField;
    private Class adapterClass;

    private void initAdapter() {
        if (adapterClass == null) {
            adapterClass = getAdapterClass(eventClass);
        }
        if (adapterField == null) {
            try {
                adapterField = adapterClass.getField(__name__);
            } catch (NoSuchFieldException exc) {
                throw Py.AttributeError("Internal bean event error: "+
                                        __name__);
            }
        }
    }

    private void setFunction(Object self, PyObject callable) {
        initAdapter();
        try {
            adapterField.set(getAdapter(self), callable);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }

    public boolean _doset(PyObject self, PyObject value) {
        Object jself = Py.tojava(self, addMethod.getDeclaringClass());
        if (!(value instanceof PyCompoundCallable)) {
            PyCompoundCallable func = new PyCompoundCallable();
            setFunction(jself, func);
            func.append(value);
        } else {
            setFunction(jself, value);
        }
        return true;
    }

    public String toString() {
        return "<beanEventProperty "+__name__+" for event "+
            eventClass.toString()+" "+Py.idstr(this)+">";
    }
}
