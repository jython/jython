// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyJavaInnerClass extends PyJavaClass
{
    public PyJavaClass parent=null;
    public PyJavaInnerClass(Class c, PyJavaClass parent) {
        super(__class__);
        init(c);
        this.parent = parent;
        int dollar = __name__.indexOf('$');
        if (dollar != -1) {
            __name__ = __name__.substring(0, dollar)+
                "." + __name__.substring(dollar+1, __name__.length());
        }

    }

    PyObject lookup(String name, boolean stop_at_java) {
        PyObject result = super.lookup(name, stop_at_java);
        if (result != null)
            return result;
        return parent.lookup(name, stop_at_java);
    }
}
