// Copyright (c) Corporation for National Research Initiatives
package org.python.core;


/**
 * A wrapper around a java inner class.
 */

public class PyJavaInnerClass extends PyJavaClass
{
    public PyJavaClass parent = null;

    public PyJavaInnerClass(Class c, PyJavaClass parent) {
        super(c);
        this.parent = parent;
        String pname = parent.__name__;
        __name__ = pname + "." + __name__.substring(pname.length() + 1);
    }

    PyObject lookup(String name, boolean stop_at_java) {
        PyObject result = super.lookup(name, stop_at_java);
        if (result != null)
            return result;
        return parent.lookup(name, stop_at_java);
    }
}
