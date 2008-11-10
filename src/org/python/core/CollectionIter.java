// Copyright (c) Finn Bock

package org.python.core;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

class CollectionIter{
    PyObject findCollection(Object object) {
        if (object instanceof Map) {
            return new IteratorIter(((Map) object).keySet().iterator());
        }
        if (object instanceof Iterable) {
            return new IteratorIter(((Iterable)object).iterator());
        }
        if (object instanceof Iterator) {
            return new IteratorIter(((Iterator) object));
        }
        if (object instanceof Enumeration) {
            return new EnumerationIter(((Enumeration) object));
        }
        if (object instanceof Dictionary) {
            return new EnumerationIter(((Dictionary) object).keys());
        }

        return null;
    }

}

class EnumerationIter extends PyIterator {
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

class IteratorIter extends PyIterator {
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
