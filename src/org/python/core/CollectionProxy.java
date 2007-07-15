// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

class CollectionProxy {
    public static final CollectionProxy NoProxy = new EnumerationProxy(null);

    public static CollectionProxy findCollection(Object object) {
        if (object == null)
            return NoProxy;
        
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


        if (object instanceof Vector) {
            return new VectorProxy(((Vector) object));
        }
        if (object instanceof Enumeration) {
            return new EnumerationProxy(((Enumeration) object));
        }
        if (object instanceof Dictionary) {
            return new DictionaryProxy(((Dictionary) object));
        }

        return NoProxy;
    }

    /** The basic functions to implement a mapping* */
    public int __len__() {
        throw Py.AttributeError("__len__");
    }

    public PyObject __finditem__(int key) {
        return __finditem__(new PyInteger(key));
    }

    public PyObject __finditem__(PyObject key) {
        throw Py.AttributeError("__getitem__");
    }

    public PyObject __getitem__(int key) {
        PyObject ret = __finditem__(key);
        if (ret == null)
            throw Py.KeyError("" + key);
        return ret;
    }

    public PyObject __getitem__(PyObject key) {
        PyObject ret = __finditem__(key);
        if (ret == null)
            throw Py.KeyError(key.toString());
        return ret;
    }

    public void __setitem__(PyObject key, PyObject value) {
        throw Py.AttributeError("__setitem__");
    }

    public void __delitem__(PyObject key) {
        throw Py.AttributeError("__delitem__");
    }
}

class EnumerationProxy extends CollectionProxy {
    Enumeration proxy;

    int counter;

    public EnumerationProxy(Enumeration proxy) {
        this.proxy = proxy;
        this.counter = 0;
    }

    public PyObject __finditem__(int key) {
        if (key != this.counter) {
            throw Py
                    .ValueError("enumeration indices must be consecutive ints starting at 0");
        }
        this.counter++;
        if (this.proxy.hasMoreElements()) {
            return Py.java2py(this.proxy.nextElement());
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

class VectorProxy extends CollectionProxy {
    Vector proxy;

    public VectorProxy(Vector proxy) {
        this.proxy = proxy;
    }

    public int __len__() {
        return this.proxy.size();
    }

    public PyObject __finditem__(int key) {
        try {
            return Py.java2py(this.proxy.elementAt(key));
        } catch (ArrayIndexOutOfBoundsException exc) {
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

    public void __setitem__(PyObject key, PyObject value) {
        if (key instanceof PyInteger) {
            this.proxy.setElementAt(Py.tojava(value, Object.class),
                    ((PyInteger) key).getValue());
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }

    public void __delitem__(PyObject key) {
        if (key instanceof PyInteger) {
            this.proxy.removeElementAt(((PyInteger) key).getValue());
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }
}

class DictionaryProxy extends CollectionProxy {
    Dictionary proxy;

    public DictionaryProxy(Dictionary proxy) {
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
