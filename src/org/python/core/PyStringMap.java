// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Special fast dict implementation for __dict__ instances. Allows interned String keys in addition
 * to PyObject unlike PyDictionary.
 */
public class PyStringMap extends PyObject {

    private final Map<Object, PyObject> table;

    public PyStringMap() {
        this(4);
    }

    public PyStringMap(int capacity) {
        table = new ConcurrentHashMap<Object, PyObject>(capacity);
    }

    public PyStringMap(Map<Object, PyObject> map) {
        table = new ConcurrentHashMap<Object, PyObject>(map);
    }

    public PyStringMap(PyObject elements[]) {
        this(elements.length);
        for (int i = 0; i < elements.length; i += 2) {
            __setitem__(elements[i], elements[i + 1]);
        }
    }

    public int __len__() {
        return table.size();
    }

    public boolean __nonzero__() {
        return table.size() != 0;
    }

    public PyObject __finditem__(String key) {
        if (key == null) {
            return null;
        }
        return table.get(key);
    }

    public PyObject __finditem__(PyObject key) {
        if (key instanceof PyString) {
            return __finditem__(((PyString)key).internedString());
        }
        return table.get(key);
    }

    public PyObject __getitem__(String key) {
        PyObject o = __finditem__(key);
        if (null == o) {
            throw Py.KeyError("'" + key + "'");
        } else {
            return o;
        }
    }

    public PyObject __getitem__(PyObject key) {
        if (key instanceof PyString) {
            return __getitem__(((PyString)key).internedString());
        } else {
            PyObject o = __finditem__(key);
            if (null == o) {
                throw Py.KeyError("'" + key.toString() + "'");
            } else {
                return o;
            }
        }
    }

    public PyObject __iter__() {
        return iterkeys();
    }

    public void __setitem__(String key, PyObject value) {
        if (value == null) {
            table.remove(key);
        } else {
            table.put(key, value);
        }
    }

    public void __setitem__(PyObject key, PyObject value) {
        if (value == null) {
            if (key instanceof PyString) {
                table.remove(((PyString)key).internedString());
            } else {
                table.remove(key);
            }
        } else if (key instanceof PyString) {
            __setitem__(((PyString)key).internedString(), value);
        } else {
            table.put(key, value);
        }
    }

    public void __delitem__(String key) {
        Object ret = table.remove(key);
        if (ret == null) {
            throw Py.KeyError(key);
        }
    }

    public void __delitem__(PyObject key) {
        if (key instanceof PyString) {
            __delitem__(((PyString)key).internedString());
        } else {
            Object ret = table.remove(key);
            if (ret == null) {
                throw Py.KeyError(key.toString());
            }
        }
    }

    /**
     * Remove all items from the dictionary.
     */
    public void clear() {
        table.clear();
    }

    public String toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "{...}";
        }
        StringBuffer buf = new StringBuffer("{");
        for (Entry<Object, PyObject> entry : table.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                buf.append(key);
            } else {
                buf.append(((PyObject)entry.getKey()).__repr__().toString());
            }
            buf.append(": ");
            buf.append(entry.getValue().__repr__().toString());
            buf.append(", ");
        }
        if (buf.length() > 1) {
            buf.delete(buf.length() - 2, buf.length());
        }
        buf.append("}");
        ts.exitRepr(this);
        return buf.toString();
    }

    public int __cmp__(PyObject other) {
        if (!(other instanceof PyStringMap || other instanceof PyDictionary)) {
            return -2;
        }
        int an = __len__();
        int bn = other.__len__();
        if (an < bn) {
            return -1;
        }
        if (an > bn) {
            return 1;
        }
        PyList akeys = keys();
        PyList bkeys = null;
        if (other instanceof PyStringMap) {
            bkeys = ((PyStringMap)other).keys();
        } else {
            bkeys = ((PyDictionary)other).keys();
        }
        akeys.sort();
        bkeys.sort();
        for (int i = 0; i < bn; i++) {
            PyObject akey = akeys.pyget(i);
            PyObject bkey = bkeys.pyget(i);
            int c = akey._cmp(bkey);
            if (c != 0) {
                return c;
            }
            PyObject avalue = __finditem__(akey);
            PyObject bvalue = other.__finditem__(bkey);
            c = avalue._cmp(bvalue);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /**
     * Return true if the key exist in the dictionary.
     */
    public boolean has_key(String key) {
        return table.containsKey(key);
    }

    public boolean has_key(PyObject key) {
        if (key instanceof PyString) {
            return has_key(((PyString)key).internedString());
        }
        return table.containsKey(key);
    }

    /**
     * Return this[key] if the key exists in the mapping, default_object is returned otherwise.
     * 
     * @param key
     *            the key to lookup in the mapping.
     * @param default_object
     *            the value to return if the key does not exists in the mapping.
     */
    public PyObject get(PyObject key, PyObject default_object) {
        PyObject o = __finditem__(key);
        if (o == null) {
            return default_object;
        } else {
            return o;
        }
    }

    /**
     * Return this[key] if the key exists in the mapping, None is returned otherwise.
     * 
     * @param key
     *            the key to lookup in the mapping.
     */
    public PyObject get(PyObject key) {
        return get(key, Py.None);
    }

    /**
     * Return a shallow copy of the dictionary.
     */
    public PyStringMap copy() {
        return new PyStringMap(table);
    }

    /**
     * Insert all the key:value pairs from <code>map</code> into this mapping. Since this is a
     * PyStringMap, no need to coerce keys, like from PyDictionary below
     */
    public void update(PyStringMap map) {
        table.putAll(map.table);
    }

    /**
     * Insert all the key:value pairs from <code>dict</code> into this mapping.
     */
    public void update(PyDictionary dict) {
        for (Iterator it = dict.table.entrySet().iterator(); it.hasNext();) {
            Entry entry = (Entry)it.next();
            __setitem__((PyObject)entry.getKey(), (PyObject)entry.getValue());
        }
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with a None value and return None.
     * 
     * @param key
     *            the key to lookup in the mapping.
     */
    public PyObject setdefault(PyObject key) {
        return setdefault(key, Py.None);
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with the value of failobj and return
     * failobj
     * 
     * @param key
     *            the key to lookup in the mapping.
     * @param failobj
     *            the default value to insert in the mapping if key does not already exist.
     */
    public PyObject setdefault(PyObject key, PyObject failobj) {
        PyObject o = __finditem__(key);
        if (o == null) {
            __setitem__(key, o = failobj);
        }
        return o;
    }

    /**
     * Return a random (key, value) tuple pair and remove the pair from the mapping.
     */
    public PyObject popitem() {
        Iterator it = table.entrySet().iterator();
        if (!it.hasNext()) {
            throw Py.KeyError("popitem(): dictionary is empty");
        }
        Entry entry = (Entry)it.next();
        Object objKey = entry.getKey();
        PyObject value = (PyObject)entry.getValue();
        PyTuple tuple;
        if (objKey instanceof String) {
            tuple = new PyTuple(new PyString((String)objKey), value);
        } else {
            tuple = new PyTuple((PyObject)objKey, value);
        }
        it.remove();
        return tuple;
    }

    // not correct - we need to determine size and remove at the same time!
    public PyObject pop(PyObject key) {
        if (table.size() == 0) {
            throw Py.KeyError("pop(): dictionary is empty");
        }
        return pop(key, null);
    }

    public PyObject pop(PyObject key, PyObject failobj) {
        Object objKey;
        if (key instanceof PyString) {
            objKey = ((PyString)key).internedString();
        } else {
            objKey = key;
        }
        PyObject value = table.remove(objKey);
        if (value == null) {
            if (failobj == null) {
                throw Py.KeyError(key.__repr__().toString());
            } else {
                return failobj;
            }
        }
        return value;
    }

    /**
     * Return a copy of the mappings list of (key, value) tuple pairs.
     */
    public PyList items() {
        List<PyObject> list = new ArrayList<PyObject>(table.size());
        for (Entry<Object, PyObject> entry : table.entrySet()) {
            list.add(itemTuple(entry));
        }
        return new PyList(list);
    }
    
    private PyTuple itemTuple(Entry<Object, PyObject> entry){
        Object key = entry.getKey();
        PyObject pyKey;
        if (key instanceof String) {
            pyKey = PyString.fromInterned((String)key);
        } else {
            pyKey = (PyObject)key;
        }
        return new PyTuple(pyKey, entry.getValue());
    }

    /**
     * Return a copy of the mappings list of keys. We have to take in account that we could be
     * storing String or PyObject objects
     */
    public PyList keys() {
        List<PyObject> list = new ArrayList<PyObject>(table.size());
        for (Iterator it = table.keySet().iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof String) {
                list.add(PyString.fromInterned((String)obj));
            } else {
                list.add((PyObject)obj);
            }
        }
        return new PyList(list);
    }

    /**
     * Return a copy of the mappings list of values.
     */
    public PyList values() {
        return new PyList(table.values());
    }

    /**
     * return an iterator over (key, value) pairs
     */
    public PyObject iteritems() {
        return new ItemsIter(table.entrySet());
    }

    /**
     * return an iterator over the keys
     */
    // Python allows one to change the dict while iterating over it,
    // including deletion. Java does not. Can we resolve with CHM?
    public PyObject iterkeys() {
        return new KeysIter(table.keySet());
    }

    /**
     * return an iterator over the values
     */
    public PyObject itervalues() {
        return new ValuesIter(table.values());
    }

    private class ValuesIter extends PyIterator {

        private final Iterator<PyObject> iterator;

        public ValuesIter(Collection<PyObject> c) {
            this.iterator = c.iterator();
        }

        public PyObject __iternext__() {
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next();
        }
    }

    private class KeysIter extends PyIterator {

        private final Iterator iterator;

        public KeysIter(Set s) {
            this.iterator = s.iterator();
        }

        public PyObject __iternext__() {
            if (!iterator.hasNext()) {
                return null;
            }
            Object objKey = iterator.next();
            PyObject key = null;
            if (objKey instanceof String) {
                key = PyString.fromInterned((String)objKey);
            } else {
                key = (PyObject)objKey;
            }
            return key;
        }
    }

    private class ItemsIter extends PyIterator {

        private final Iterator<Entry<Object, PyObject>> iterator;

        public ItemsIter(Set<Entry<Object, PyObject>> s) {
            this.iterator = s.iterator();
        }

        public PyObject __iternext__() {
            if (!iterator.hasNext()) {
                return null;
            }
            return itemTuple(iterator.next());
        }
    }
}
