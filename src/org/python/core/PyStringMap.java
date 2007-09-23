// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A faster Dictionary where the keys have to be strings.
 * <p>
 * This is the default for all __dict__ instances.
 */

public class PyStringMap extends PyObject
{
    protected java.util.Map table;

    public PyStringMap(int capacity) {
        table = java.util.Collections.synchronizedMap(new java.util.HashMap(capacity));
    }

    public PyStringMap(java.util.Map map) {
        table = java.util.Collections.synchronizedMap(new java.util.HashMap(map));
    }

    public PyStringMap() {
        this(4);
    }

    public PyStringMap(PyObject elements[]) {
        table = java.util.Collections.synchronizedMap(new java.util.HashMap(elements.length));
        for (int i=0; i<elements.length; i+=2) {
            __setitem__(elements[i], elements[i+1]);
        }
    }

    public int __len__() {
        return table.size();
    }

    public boolean __nonzero__() {
        return table.size() != 0;
    }

    public PyObject __finditem__(String key) {
        return (PyObject)table.get(key);
    }

    public PyObject __finditem__(PyObject key) {
        if (key instanceof PyString)
            return __finditem__(((PyString)key).internedString());
        return (PyObject)table.get(key);
    }

    public PyObject __getitem__(String key) {
        PyObject o=__finditem__(key);
        if (null == o) {
            throw Py.KeyError("'"+key+"'");
        } else {
            return o;
        }
    }
    
    public PyObject __getitem__(PyObject key) {
        if (key instanceof PyString) {
            return __getitem__(((PyString)key).internedString());
        } else {
            PyObject o=__finditem__(key);
            if (null == o) {
                throw Py.KeyError("'"+key.toString()+"'");
            } else {
                return o;
            }
        }
    }

    public PyObject __iter__() {
        return iterkeys();
    }

    public void __setitem__(String key, PyObject value) {
        table.put(key, value);
    }

    public void __setitem__(PyObject key, PyObject value) {
        if (key instanceof PyString) {
            __setitem__(((PyString)key).internedString(), value);
        }
        else {
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
        }
        else {
            Object ret = table.remove(key);
            if (ret == null)
                throw Py.KeyError(key.toString());
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
        synchronized(table) {
            for (java.util.Iterator it = table.entrySet().iterator(); it.hasNext(); ) {
                java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
                Object key = entry.getKey();
                if (key instanceof String) {
                    buf.append(key);
                }
                else {
                    buf.append(((PyObject)entry.getKey()).__repr__().toString());
                }
                buf.append(": ");
                buf.append(((PyObject)entry.getValue()).__repr__().toString());
                buf.append(", ");
            }
        }
        if(buf.length() > 1){
            buf.delete(buf.length() - 2, buf.length());
        }
        buf.append("}");

        ts.exitRepr(this);
        return buf.toString();
    }
  
    public int __cmp__(PyObject other) {
        if (!(other instanceof PyStringMap ||
                  other instanceof PyDictionary)) {
            return -2;
        }
        int an = __len__();
        int bn = other.__len__();
        if (an < bn) return -1;
        if (an > bn) return 1;

        PyList akeys = keys();
        PyList bkeys = null;
        if (other instanceof PyStringMap) {
            bkeys = ((PyStringMap)other).keys();
        } else {
            bkeys = ((PyDictionary)other).keys();
        }
        akeys.sort();
        bkeys.sort();

        for (int i=0; i<bn; i++) {
            PyObject akey = akeys.pyget(i);
            PyObject bkey = bkeys.pyget(i);
            int c = akey._cmp(bkey);
            if (c != 0)
                return c;

            PyObject avalue = __finditem__(akey);
            PyObject bvalue = other.__finditem__(bkey);
            c = avalue._cmp(bvalue);
            if (c != 0)
                return c;
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
     * Return this[key] if the key exists in the mapping, default_object
     * is returned otherwise.
     *
     * @param key            the key to lookup in the mapping.
     * @param default_object the value to return if the key does not
     *                       exists in the mapping.
     */
    public PyObject get(PyObject key, PyObject default_object) {
        PyObject o = __finditem__(key);
        if (o == null)
            return default_object;
        else
            return o;
    }

    /**
     * Return this[key] if the key exists in the mapping, None
     * is returned otherwise.
     *
     * @param key  the key to lookup in the mapping.
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
     * Insert all the key:value pairs from <code>map</code> into this
     * mapping. Since this is a PyStringMap, no need to coerce keys,
     * like from PyDictionary below
     */
    public void update(PyStringMap map) {
        table.putAll(map.table);
    }

    /**
     * Insert all the key:value pairs from <code>dict</code> into
     * this mapping.
     */
    public void update(PyDictionary dict) {
        synchronized(dict.table) {
            for (java.util.Iterator it = dict.table.entrySet().iterator(); it.hasNext(); ) {
                java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
                __setitem__((PyObject)entry.getKey(), (PyObject)entry.getValue());
            }
        }
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with
     * a None value and return None.
     *
     * @param key   the key to lookup in the mapping.
     */
    public PyObject setdefault(PyObject key) {
        return setdefault(key, Py.None);
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with
     * the value of failobj and return failobj
     *
     * @param key     the key to lookup in the mapping.
     * @param failobj the default value to insert in the mapping
     *                if key does not already exist.
     */
    public PyObject setdefault(PyObject key, PyObject failobj) {
        PyObject o = __finditem__(key);
        if (o == null)
            __setitem__(key, o = failobj);
        return o;
    }

    /**
     * Return a random (key, value) tuple pair and remove the pair
     * from the mapping.
     */
    public PyObject popitem() {
        synchronized(table) {
            java.util.Iterator it = table.entrySet().iterator();
            if (!it.hasNext())
                throw Py.KeyError("popitem(): dictionary is empty");
            java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
            it.remove();
            return new PyTuple(new PyObject[] { (PyObject)entry.getKey(), (PyObject)entry.getValue() });
        }
    }

    /**
     * Return a copy of the mappings list of (key, value) tuple
     * pairs.
     */
    public PyList items() {
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);
        synchronized(table) {
            for (java.util.Iterator it = table.entrySet().iterator(); it.hasNext(); ) {
                java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
                l.addElement(new PyTuple(new PyObject[] { (PyObject)entry.getKey(), (PyObject)entry.getValue() }));
            }
        }
        return new PyList(l);
    }


    /**
     * Return a copy of the mappings list of keys. We have to take in
     * account that we could be storing String or PyObject objects
     */
    public PyList keys() {
        int n = table.size();
        java.util.Vector v = new java.util.Vector(n);
        for (java.util.Iterator it = table.keySet().iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if (obj instanceof String) {
                v.addElement(new PyString((String)obj));
            }
            else {
                v.addElement(obj);
            }
        }
        return new PyList(v);
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
        return new PDEntriesIter(table.entrySet());
    }
    
    /**
     * return an iterator over the keys
     */
    public PyObject iterkeys() {
        return new PDCollectionIter(table.keySet());
    }
    
    /**
     * return an iterator over the values
     */
    public PyObject itervalues() {
        return new PDCollectionIter(table.values());
    }

    // TODO: this is not going to work for String keys...
    // just a lightweight wrapper for now...
    class PDCollectionIter extends PyIterator {
        private java.util.Iterator iterator;
        public PDCollectionIter(java.util.Collection c) {
            this.iterator = c.iterator();
        }

        public PyObject __iternext__() {
            if (!iterator.hasNext())
                return null;
            return (PyObject)iterator.next();
        }
    }

    class PDEntriesIter extends PyIterator {
        private java.util.Iterator iterator;
        public PDEntriesIter(java.util.Set s) {
            this.iterator = s.iterator();
        }
        
        public PyObject __iternext__() {
        if (!iterator.hasNext())
            return null;
        java.util.Map.Entry entry =  (java.util.Map.Entry)iterator.next();
        return new PyTuple(new PyObject[] { (PyObject)entry.getKey(), (PyObject)entry.getValue() });
        }
    }
}


