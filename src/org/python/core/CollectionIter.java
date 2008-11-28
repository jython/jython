package org.python.core;

import java.util.Enumeration;
import java.util.Iterator;

class EnumerationIter extends PyIterator {

    private Enumeration<Object> proxy;

    public EnumerationIter(Enumeration<Object> proxy) {
        this.proxy = proxy;
    }

    public PyObject __iternext__() {
        return proxy.hasMoreElements() ? Py.java2py(proxy.nextElement()) : null;
    }
}

class IteratorIter extends PyIterator {

    private Iterator<Object> proxy;

    public IteratorIter(Iterator<Object> proxy) {
        this.proxy = proxy;
    }

    public PyObject __iternext__() {
        return proxy.hasNext() ? Py.java2py(proxy.next()) : null;
    }
}
