// Copyright (c) Finn Bock

package org.python.core;

import java.util.*;

class CollectionIter extends PyObject {
    PyObject findCollection(Object object) {
        if (object instanceof Vector) {
            return new EnumerationIter(((Vector) object).elements());
        }
        if (object instanceof Enumeration) {
            return new EnumerationIter(((Enumeration) object));
        }
        if (object instanceof Dictionary) {
            return new EnumerationIter(((Dictionary) object).keys());
        }

        return null;
    }

    public PyObject next() {
        PyObject ret = __iternext__();
        if (ret == null) {
            throw Py.StopIteration(null);
        }
        return ret;
    }

}

class EnumerationIter extends CollectionIter {
    private Enumeration proxy;

    public EnumerationIter(Enumeration proxy) {
        this.proxy = proxy;
    }

    public PyObject __iternext__() {
        if (!this.proxy.hasMoreElements())
            return null;
        return Py.java2py(this.proxy.nextElement());
    }
}
