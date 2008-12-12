// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.Method;

public class PyBeanEvent<T> extends PyObject {

    public Method addMethod;

    public Class<T> eventClass;

    public String __name__;

    public PyBeanEvent(String name, Class<T> eventClass, Method addMethod) {
        __name__ = name.intern();
        this.addMethod = addMethod;
        this.eventClass = eventClass;
    }

    public PyObject _doget(PyObject container) {
        throw Py.TypeError("write only attribute");
    }

    boolean jdontdel() {
        throw Py.TypeError("can't delete this attribute");
    }

    public boolean _doset(PyObject self, PyObject value) {
        Object jself = Py.tojava(self, addMethod.getDeclaringClass());
        T jvalue = Py.tojava(value, eventClass);
        try {
            addMethod.invoke(jself, jvalue);
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        return true;
    }

    public String toString() {
        return "<beanEvent " + __name__ + " for event " + eventClass.toString() + " "
                + Py.idstr(this) + ">";
    }
}
