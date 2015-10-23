package org.python.core;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDict extends PyObject {

    public AbstractDict(PyType type) {
        super(type);
    }

    public abstract void clear();
    public abstract AbstractDict copy();
    public abstract PyObject get(PyObject key);
    public abstract PyObject get(PyObject key, PyObject defaultObj);
    public abstract ConcurrentMap<? extends Object, PyObject> getMap();
    public abstract boolean has_key(PyObject key);
    public abstract PyList items();
    public abstract PyObject iteritems();
    public abstract PyObject iterkeys();
    public abstract PyObject itervalues();
    public abstract PyList keys();
    public abstract void merge(PyObject other, boolean override);
    public abstract void mergeFromKeys(PyObject other, PyObject keys, boolean override);
    public abstract void mergeFromSeq(PyObject other, boolean override);
    public abstract PyObject pop(PyObject key);
    public abstract PyObject pop(PyObject key, PyObject defaultValue);
    public abstract PyObject popitem();
    public abstract PyObject setdefault(PyObject key);
    public abstract PyObject setdefault(PyObject key, PyObject failobj);
    public abstract void update(PyObject other);
    public abstract Collection<? extends Object> values();
    public abstract Set<PyObject> pyKeySet();

    public abstract Set entrySet();

    /**
     * Returns a dict_keys on the dictionary's keys
     */
    public PyObject viewkeys() {
        return new PyDictionary.PyDictionaryViewKeys(this);
    }

    /**
     * Returns a dict_items on the dictionary's items
     */
    public PyObject viewitems() {
        return new PyDictionary.PyDictionaryViewItems(this);
    }

    /**
     * Returns a dict_values on the dictionary's values
     */
    public PyObject viewvalues() {
        return new PyDictionary.PyDictionaryViewValues(this);
    }

    /** Convert return values to java objects */
    static Object tojava(Object val) {
        return val == null ? null : ((PyObject)val).__tojava__(Object.class);
    }

    static class ValuesIter extends PyIterator {

        private final Iterator<PyObject> iterator;

        private final int size;

        public ValuesIter(Collection<PyObject> values) {
            iterator = values.iterator();
            size = values.size();
        }

        @Override
        public PyObject __iternext__() {
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next();
        }
    }
}

/** Basic implementation of Entry that just holds onto a key and value and returns them. */
class SimpleEntry implements Entry {

    protected Object key;

    protected Object value;

    public SimpleEntry(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry e = (Map.Entry)o;
        return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    @Override
    public int hashCode() {
        return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    public Object setValue(Object val) {
        throw new UnsupportedOperationException("Not supported by this view");
    }
}

/**
 * Wrapper for a Entry object returned from the java.util.Set object which in turn is returned by
 * the entrySet method of java.util.Map. This is needed to correctly convert from PyObjects to java
 * Objects. Note that we take care in the equals and hashCode methods to make sure these methods are
 * consistent with Entry objects that contain java Objects for a value so that on the java side they
 * can be reliable compared.
 */
class PyToJavaMapEntry extends SimpleEntry {

    /** Create a copy of the Entry with Py.None converted to null */
    PyToJavaMapEntry(Entry entry) {
        super(entry.getKey(), entry.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Entry)) {
            return false;
        }
        Entry me = new JavaToPyMapEntry((Entry)o);
        return o.equals(me);
    }

    /* tojava is called in getKey and getValue so the raw key and value can be
       used to create a new SimpleEntry in getEntry. */
    @Override
    public Object getKey() {
        return AbstractDict.tojava(key);
    }

    @Override
    public Object getValue() {
        return AbstractDict.tojava(value);
    }

    /**
     * @return an entry that returns the original values given to this entry.
     */
    public Entry getEntry() {
        return new SimpleEntry(key, value);
    }

    @Override
    public int hashCode() {
        return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

}

/**
 * MapEntry Object for java MapEntry objects passed to the java.util.Set interface which is returned
 * by the entrySet method of PyDictionary. Essentially like PyTojavaMapEntry, but going the other
 * way converting java Objects to PyObjects.
 */
class JavaToPyMapEntry extends SimpleEntry {

    public JavaToPyMapEntry(Entry entry) {
        super(Py.java2py(entry.getKey()), Py.java2py(entry.getValue()));
    }
}

/**
 * Wrapper collection class for the keySet and values methods of java.util.Map
 */
class PyMapKeyValSet extends PyMapSet {

    PyMapKeyValSet(Collection coll) {
        super(coll);
    }

    @Override
    Object toJava(Object o) {
        return AbstractDict.tojava(o);
    }

    @Override
    Object toPython(Object o) {
        return Py.java2py(o);
    }
}

/**
 * Set wrapper for the entrySet method. Entry objects are wrapped further in JavaToPyMapEntry and
 * PyToJavaMapEntry. Note - The set interface is reliable for standard objects like strings and
 * integers, but may be inconsistent for other types of objects since the equals method may return
 * false for Entry object that hold more elaborate PyObject types. However, we insure that this
 * interface works when the Entry object originates from a Set object retrieved from a PyDictionary.
 */
class PyMapEntrySet extends PyMapSet {

    PyMapEntrySet(Collection coll) {
        super(coll);
    }

    /* We know that PyMapEntrySet will only contains entries, so if the object being passed in is
       null or not an Entry, then return null which will match nothing for remove and contains
       methods. */
    @Override
    Object toPython(Object o) {
        if (o == null || !(o instanceof Entry)) {
            return null;
        }
        if (o instanceof PyToJavaMapEntry) {
            /* Use the original entry from PyDictionary */
            return ((PyToJavaMapEntry)o).getEntry();
        } else {
            return new JavaToPyMapEntry((Entry)o);
        }
    }

    @Override
    Object toJava(Object o) {
        return new PyToJavaMapEntry((Entry)o);
    }
}

/**
 * PyMapSet serves as a wrapper around Set Objects returned by the java.util.Map interface of
 * PyDictionary. entrySet, values and keySet methods return this type for the java.util.Map
 * implementation. This class is necessary as a wrapper to convert PyObjects to java Objects for
 * methods that return values, and convert Objects to PyObjects for methods that take values. The
 * translation is necessary to provide java access to jython dictionary objects. This wrapper also
 * provides the expected backing functionality such that changes to the wrapper set or reflected in
 * PyDictionary.
 */
abstract class PyMapSet extends AbstractSet {

    private final Collection coll;

    PyMapSet(Collection coll) {
        this.coll = coll;
    }

    abstract Object toJava(Object obj);

    abstract Object toPython(Object obj);

    @Override
    public int size() {
        return coll.size();
    }

    @Override
    public boolean contains(Object o) {
        return coll.contains(toPython(o));
    }

    @Override
     public boolean remove(Object o) {
         return coll.remove(toPython(o));
    }

    @Override
    public void clear() {
        coll.clear();
    }

    /* Iterator wrapper class returned by the PyMapSet iterator
       method. We need this wrapper to return PyToJavaMapEntry objects
       for the 'next()' method. */
    class PySetIter implements Iterator {
        Iterator itr;

        PySetIter(Iterator itr) {
            this.itr = itr;
        }

        public boolean hasNext() {
            return itr.hasNext();
        }

        public Object next() {
            return toJava(itr.next());
        }

        public void remove() {
            itr.remove();
        }
    }

    @Override
    public Iterator iterator() {
        return new PySetIter(coll.iterator());
    }
}