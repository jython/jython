// Copyright © Corporation for National Research Initiatives
package org.python.core;
import java.lang.reflect.*;

public class PyBeanEvent extends PyObject {
    public Method addMethod;
    public Class eventClass;
    public String __name__;

    public static PyClass __class__;
    public PyBeanEvent(String name, Class eventClass, Method addMethod) {
        super(__class__);
        __name__ = name.intern();
        this.addMethod = addMethod;
        this.eventClass = eventClass;
    }

    public PyObject _doget(PyObject container) {
        throw Py.TypeError("write only attribute");
    }

    public boolean _dodel(PyObject container) {
        throw Py.TypeError("can't delete this attribute");
    }

    public boolean _doset(PyObject self, PyObject value) {
        Object jself = Py.tojava(self, addMethod.getDeclaringClass());
        Object jvalue = Py.tojava(value, eventClass);

        try {
            addMethod.invoke(jself, new Object[] {jvalue});
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
        return true;
    }

    public String toString() {
        return "<beanEvent "+__name__+" for event "+
            eventClass.toString()+" at "+hashCode()+">";
    }
}
