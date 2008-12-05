// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.util.Arrays;
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
        
        if (object instanceof Vector) {
            return new VectorProxy(((Vector) object));
        }
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




class ListProxy extends CollectionProxy {
    List proxy;

    public ListProxy(List proxy) {
        this.proxy = proxy;
    }

    public int __len__() {
        return this.proxy.size();
    }

    public PyObject __finditem__(int key) {
        int k = key < 0  ?  key + this.proxy.size()  :  key;
        try {
            return Py.java2py(this.proxy.get(k));
        } catch (IndexOutOfBoundsException exc) {
            throw Py.IndexError("index out of range: " + String.valueOf(key));
        }
    }

    protected PyObject __finditem__(int start, int stop, int step, int n) {
        if(step > 0 && stop < start) {
            stop = start;
        }
        PyObject[] newList = new PyObject[n];
        if(step == 1) {
            for (int i = start; i < stop; i++)
            {
                newList[i-start] = Py.java2py(this.proxy.get(i));
            }
            return new PyList(newList);
        }
        int j = 0;
        for(int i = start; j < n; i += step) {
            newList[j] = Py.java2py(this.proxy.get(i));
            j++;
        }
        return new PyList(newList);
    }

    public PyObject __finditem__(PyObject key) {
        if (key instanceof PyInteger) {
            return __finditem__(((PyInteger) key).getValue());
        } else if (key instanceof PySlice) {
            PySlice slice = (PySlice)key;
            int indices[] = slice.indicesEx(this.proxy.size());
            return __finditem__(indices[0], indices[1], indices[2], indices[3]);
        } else {
            throw Py.TypeError("only integer or slice keys accepted");
        }
    }

    public void __setitem__(int key, PyObject value) {
        int k = key < 0  ?  key + this.proxy.size()  :  key;
        try {
            this.proxy.set(k, Py.tojava(value, Object.class));
        } catch (IndexOutOfBoundsException exc) {
            throw Py.IndexError("assignment index out of range: " + String.valueOf(key));
        }
    }

    public void __setitem__(PyObject key, PyObject value) {
        if (key instanceof PyInteger) {
            __setitem__(((PyInteger) key).getValue(), value);
        } else if (key instanceof PySlice) {
            PySlice slice = (PySlice)key;
            int indices[] = slice.indicesEx(this.proxy.size());
            __setitem__(slice, indices[0], indices[1], indices[2], indices[3], value);
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }

    public void __delitem__(int key) {
        int k = key < 0  ?  key + this.proxy.size()  :  key;
        try {
            this.proxy.remove(k);
        } catch (IndexOutOfBoundsException exc) {
            throw Py.IndexError("assignment index out of range: " + String.valueOf(key));
        }
    }

    protected void __delitem__(int start, int stop, int step, int n) {
        if(step == 1) {
            for (int i = stop - 1; i >= start; i--) {
                this.proxy.remove(i);
            }
        } else if(step > 1) {
            for(int i = start; i < stop; i += step) {
                this.proxy.remove(i);
                i--;
                stop--;
            }
        } else if(step < 0) {
            for(int i = start; i >= 0 && i >= stop; i += step) {
                this.proxy.remove(i);
            }
        }
    }

    public void __delitem__(PyObject key) {
        if (key instanceof PyInteger) {
            __delitem__(((PyInteger) key).getValue());
        } else if (key instanceof PySlice) {
            PySlice slice = (PySlice)key;
            int indices[] = slice.indicesEx(this.proxy.size());
            __delitem__(indices[0], indices[1], indices[2], indices[3]);
        } else {
            throw Py.TypeError("only integer keys accepted");
        }
    }




    protected void __setitem__(PySlice slice, int start, int stop, int step, int n, PyObject value) {
        if(stop < start) {
            stop = start;
        }
        if(value instanceof PySequence) {
            PySequence sequence = (PySequence) value;
            setslicePySequence(slice, start, stop, step, n, sequence);
        } else if(value instanceof List) {
            List list = (List)value.__tojava__(List.class);
            if(list != null && list != Py.NoConversion) {
                setsliceList(slice, start, stop, step, list);
            }
        } else {
            setsliceIterable(slice, start, stop, step, n, value);
        }
    }

    protected void setslicePySequence(PySlice slice, int start, int stop, int step, int n, PySequence value) {
        if(slice.step != Py.None) {
            if(n != value.__len__()) {
                throw Py.ValueError("attempt to assign sequence of size " + value.__len__() + " to extended slice of size " + n);
            }
        }
        if(step == 1) {
            PyObject[] srcArray;
            Object[] jArray;

            if(value instanceof PySequenceList) {
                srcArray = ((PySequenceList)value).getArray();
            } else {
                srcArray = Py.unpackSequence(value, value.__len__());
            }
            jArray = new Object[srcArray.length];
            for (int i = 0; i < srcArray.length; i++) {
                jArray[i] = Py.tojava(srcArray[i], Object.class);
            }
            List sub = this.proxy.subList(start, stop);
            sub.clear();
            this.proxy.addAll( start, Arrays.asList(jArray) );
        } else if(step != 0) {
            for (int i = 0, j = start; i < n; i++, j += step) {
                this.proxy.set(j, Py.tojava(value.pyget(i), Object.class));
            }
        }
    }

    protected void setsliceList(PySlice slice, int start, int stop, int step, List value) {
        if(step != 1) {
            throw Py.TypeError("setslice with java.util.List and step != 1 not supported yet");
        }
        int n = value.size();
        for(int i = 0; i < n; i++) {
            this.proxy.add(i + start, value.get(i));
        }
    }

    protected void setsliceIterable(PySlice slice, int start, int stop, int step, int n, PyObject value) {
        PyObject[] seq;
        try {
            seq = Py.make_array(value);
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.TypeError)) {
                throw Py.TypeError("can only assign an iterable");
            }
            throw pye;
        }
        setslicePySequence(slice, start, stop, step, n, new PyList(seq));
    }
}




class VectorProxy extends ListProxy {
    public VectorProxy(Vector proxy) {
        super(proxy);
    }

    public PyObject __finditem__(int key) {
        int k = key < 0  ?  key + this.proxy.size()  :  key;
        try {
            return Py.java2py(((Vector)this.proxy).elementAt(k));
        } catch (IndexOutOfBoundsException exc) {
            throw Py.IndexError("index out of range: " + String.valueOf(key));
        }
    }

    public void __setitem__(int key, PyObject value) {
        int k = key < 0  ?  key + this.proxy.size()  :  key;
        try {
            ((Vector)this.proxy).setElementAt(Py.tojava(value, Object.class), k);
        } catch (IndexOutOfBoundsException exc) {
            throw Py.IndexError("assignment index out of range: " + String.valueOf(key));
        }
    }

    public void __delitem__(int key) {
        int k = key < 0  ?  key + this.proxy.size()  :  key;
        try {
            ((Vector)this.proxy).removeElementAt(k);
        } catch (IndexOutOfBoundsException exc) {
            throw Py.IndexError("assignment index out of range: " + String.valueOf(key));
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
