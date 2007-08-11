// Copyright (c) Finn Bock

package org.python.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

class CollectionIter extends PyObject {
    PyObject findCollection(Object object) {
        if (object instanceof Map) {
            return new IteratorIter(((Map) object).keySet().iterator());
        }
        if (object instanceof Collection) {
            return new IteratorIter(((Collection) object).iterator());
        }
        if (object instanceof Iterator) {
            return new IteratorIter(((Iterator) object));
        }
        try {
            // TODO - Once we depend on Java 5 we can replace this with a check
            // for the Iterable interface
            Method m = object.getClass().getMethod("iterator", new Class[0]);
            if (Iterator.class.isAssignableFrom(m.getReturnType())) {
                return new IteratorIter((Iterator) m.invoke(object,
                        new Object[0]));
            }
        } catch (Exception e) {
            // Looks like one of the many reflection based exceptions ocurred so
            // we won't get an Iterator this way
        }
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

class IteratorIter extends CollectionIter {
    private Iterator proxy;

    public IteratorIter(Iterator proxy) {
        this.proxy = proxy;
    }

    public PyObject __iternext__() {
        if (!this.proxy.hasNext())
            return null;
        return Py.java2py(this.proxy.next());
    }
}
