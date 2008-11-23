// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import org.python.util.Generic;

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

    private synchronized static Class<?> getAdapterClass(Class<?> c) {
        Class<?> pc = Py.findClass("org.python.proxies." + c.getName() + "$Adapter");
        if (pc == null) {
            pc = MakeProxies.makeAdapter(c);
        }
        return pc;
    }

    protected Map<Object, Map<String, WeakReference<Object>>> adapters =
        new WeakHashMap<Object, Map<String, WeakReference<Object>>>();

    protected Object getAdapter(Object o, String evc) {
        Map<String, WeakReference<Object>> ads = adapters.get(o);
        if (ads == null) {
            return null;
        }
        WeakReference<Object> adw = ads.get(evc);
        if (adw == null) {
            return null;
        }
        return adw.get();
    }

    protected void putAdapter(Object o, String evc, Object ad) {
        Map<String, WeakReference<Object>> ads = adapters.get(o);
        if(ads == null) {
            ads = Generic.map();
            adapters.put(o, ads);
        }
        ads.put(evc, new WeakReference<Object>(ad));
    }

    private synchronized Object getAdapter(Object self) {
        String eventClassName = eventClass.getName();
        Object adapter = getAdapter(self, eventClassName);
        if (adapter != null)
            return adapter;
        try {
            adapter = adapterClass.newInstance();
            addMethod.invoke(self, new Object[] {adapter});
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        putAdapter(self, eventClassName, adapter);
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
                throw Py.AttributeError("Internal bean event error: " + __name__);
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
