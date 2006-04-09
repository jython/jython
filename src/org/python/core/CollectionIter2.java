// Copyright (c) Finn Bock

package org.python.core;

import java.util.*;

class CollectionIter2 extends CollectionIter {
    CollectionIter2() throws Exception {
        Class.forName("java.util.Collection");
    }

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

        return null;
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
