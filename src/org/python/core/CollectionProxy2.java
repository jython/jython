// Copyright (c) Corporation for National Research Initiatives

/* Support for java-1.2 collections
 *  XXX: Is this used?  Or does the new collections integration
 *  (starting at 2.2a1) make it obsolete?
 */

package org.python.core;

import java.util.*;

class CollectionProxy2 extends CollectionProxy {
    public CollectionProxy instanceFindCollection(Object object) {
        if (object instanceof List) {
            return new ListProxy(((List) object));
        }
        if (object instanceof Map) {
            return new MapProxy(((Map) object));
        }
        if (object instanceof Collection) {
            return new IteratorProxy(((Collection) object).iterator());
        }
        if (object instanceof Iterator) {
            return new IteratorProxy(((Iterator) object));
        }

        return null;
    }
}

class ListProxy extends CollectionProxy {
    List proxy;

    public ListProxy(List proxy) {
        this.proxy = proxy;
    }

    public int __len__() {
        return this.proxy.size();
    }

    public PyObject __finditem__(int key) {
        try {
            return Py.java2py(this.proxy.get(key));
        } catch (IndexOutOfBoundsException exc) {
            return null;
        }
    }

    public PyObject __finditem__(PyObject key) {
        if (key instanceof PyInteger) {
            return __finditem__(((PyInteger) key).getValue());
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }

    public void __setitem__(int key, PyObject value) {
        this.proxy.set(key, Py.tojava(value, Object.class));
    }

    public void __setitem__(PyObject key, PyObject value) {
        if (key instanceof PyInteger) {
            __setitem__(((PyInteger) key).getValue(), value);
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }

    public void __delitem__(int key) {
        this.proxy.remove(key);
    }

    public void __delitem__(PyObject key) {
        if (key instanceof PyInteger) {
            __delitem__(((PyInteger) key).getValue());
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }
}

class MapProxy extends CollectionProxy {
    Map proxy;

    public MapProxy(Map proxy) {
        this.proxy = proxy;
    }

    public int __len__() {
        return this.proxy.size();
    }

    public PyObject __finditem__(int key) {
        throw Py.TypeError("loop over non-sequence");
    }

    public PyObject __finditem__(PyObject key) {
        return Py.java2py(this.proxy.get(Py.tojava(key, Object.class)));
    }

    public void __setitem__(PyObject key, PyObject value) {
        this.proxy.put(Py.tojava(key, Object.class), Py.tojava(value,
                Object.class));
    }

    public void __delitem__(PyObject key) {
        this.proxy.remove(Py.tojava(key, Object.class));
    }
}

class IteratorProxy extends CollectionProxy {
    Iterator proxy;

    int counter;

    public IteratorProxy(Iterator proxy) {
        this.proxy = proxy;
        this.counter = 0;
    }

    public PyObject __finditem__(int key) {
        if (key != this.counter) {
            throw Py
                    .ValueError("iterator indices must be consecutive ints starting at 0");
        }
        this.counter++;
        if (this.proxy.hasNext()) {
            return Py.java2py(this.proxy.next());
        } else {
            return null;
        }
    }

    public PyObject __finditem__(PyObject key) {
        if (key instanceof PyInteger) {
            return __finditem__(((PyInteger) key).getValue());
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }
}
