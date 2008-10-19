// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ConcurrentMap;
import org.python.core.PyMapSet.PySetIter;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;


/**
 * A builtin python dictionary.
 */
@ExposedType(name = "dict")
public class PyDictionary extends PyObject implements ConcurrentMap {

    public static final PyType TYPE = PyType.fromClass(PyDictionary.class);
    
    protected final ConcurrentMap<PyObject, PyObject> table;

    /**
     * Create an empty dictionary.
     */
    public PyDictionary() {
        table = new ConcurrentHashMap<PyObject, PyObject>();
    }

    /**
     * For derived types
     * @param subtype
     */
    public PyDictionary(PyType subtype) {
        super(subtype);
        table = new ConcurrentHashMap<PyObject, PyObject>();
    }

    /**
     * Create an new dictionary which is based on the hashtable.
     * @param t  the hashtable used. The supplied hashtable is used as
     *           is and must only contain PyObject key:value pairs.
     */
    public PyDictionary(Map<PyObject, PyObject> t) {
        table = new ConcurrentHashMap<PyObject, PyObject>(t);
    }

     /**
     * Create an new dictionary which is based on the map and for derived types.
     * @param subtype
     * @param t  the hashtable used. The supplied hashtable is used as
     *           is and must only contain PyObject key:value pairs.
     */
    public PyDictionary(PyType subtype, Map<PyObject, PyObject> t) {
        super(subtype);
        table = new ConcurrentHashMap<PyObject, PyObject>(t);
    }

        
    /**
     * Create a new dictionary with the element as content.
     * @param elements The initial elements that is inserted in the
     *                 dictionary. Even numbered elements are keys,
     *                 odd numbered elements are values.
     */
    public PyDictionary(PyObject elements[]) {
        this();
        for (int i = 0; i < elements.length; i+=2) {
            table.put(elements[i], elements[i+1]);
        }
    }

    @ExposedMethod
    @ExposedNew
    protected final void dict___init__(PyObject[] args, String[] keywords) {
        updateCommon(args, keywords, "dict");
    }
    
    public static PyObject fromkeys(PyObject keys) {
        return fromkeys(keys, Py.None);
    }

    public static PyObject fromkeys(PyObject keys, PyObject value) {
        return dict_fromkeys(TYPE, keys, value);
    }

    @ExposedClassMethod(defaults = "Py.None")
    final static PyObject dict_fromkeys(PyType type, PyObject keys, PyObject value) {
        PyObject d = type.__call__();
        for (PyObject o : keys.asIterable()) {
            d.__setitem__(o, value);
        }
        return d;
    }

    public int __len__() {
        return dict___len__();
    }

    @ExposedMethod
    final int dict___len__() {
        return table.size();
    }

    public boolean __nonzero__() {
        return dict___nonzero__();
    }

    @ExposedMethod
    final boolean dict___nonzero__() {
        return table.size() != 0;
    }

    public PyObject __finditem__(int index) {
        throw Py.TypeError("loop over non-sequence");
    }

    public PyObject __finditem__(PyObject key) {
        return table.get(key);
    }

    @ExposedMethod
    protected final PyObject dict___getitem__(PyObject key) {
        PyObject result = table.get(key);
        if (result != null) {
            return result;
        }

        // Look up __missing__ method if we're a subclass.
        PyType type = getType();
        if (type != TYPE) {
            PyObject missing = type.lookup("__missing__");
            if (missing != null) {
                return missing.__get__(this, type).__call__(key);
            }
        }
        throw Py.KeyError(key);
    }

    public void __setitem__(PyObject key, PyObject value) {
        dict___setitem__(key,value);
    }

    @ExposedMethod
    final void dict___setitem__(PyObject key, PyObject value)  {
        table.put(key, value);
    }

    public void __delitem__(PyObject key) {
        dict___delitem__(key);
    }

    @ExposedMethod
    final void dict___delitem__(PyObject key) {
        Object ret = table.remove(key);
        if (ret == null)
            throw Py.KeyError(key.toString());
    }

    public PyObject __iter__() {
        return dict___iter__();
    }

    @ExposedMethod
    final PyObject dict___iter__() {
        return iterkeys();
    }

    public String toString() {
        return dict_toString();
    }

    @ExposedMethod(names = {"__repr__", "__str__"})
    final String dict_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "{...}";
        }

        StringBuffer buf = new StringBuffer("{");

        for (Entry<PyObject, PyObject> entry : table.entrySet()) {
            buf.append((entry.getKey()).__repr__().toString());
            buf.append(": ");
            buf.append((entry.getValue()).__repr__().toString());
            buf.append(", ");
        }
        if(buf.length() > 1){
            buf.delete(buf.length() - 2, buf.length());
        }
        buf.append("}");

        ts.exitRepr(this);
        return buf.toString();
    }

    public PyObject __eq__(PyObject ob_other) {
        return dict___eq__(ob_other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject dict___eq__(PyObject ob_other) {               
        PyType thisType = getType();
        PyType otherType = ob_other.getType();
        if (otherType != thisType && !thisType.isSubType(otherType) 
                && !otherType.isSubType(thisType)) {
            return null;
        }
        PyDictionary other = (PyDictionary)ob_other;
        int an = table.size();
        int bn = other.table.size();
        if (an != bn)
            return Py.False;

        PyList akeys = keys();
        for (int i=0; i<an; i++) {
            PyObject akey = akeys.pyget(i);
            PyObject bvalue = other.__finditem__(akey);
            if (bvalue == null)
                return Py.False;
            PyObject avalue = __finditem__(akey);
            if (!avalue._eq(bvalue).__nonzero__())
                return Py.False;
        }
        return Py.True;
    }

    public PyObject __ne__(PyObject ob_other) {
        return dict___ne__(ob_other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject dict___ne__(PyObject ob_other) {
        PyObject eq_result = __eq__(ob_other);
        if (eq_result == null) return null;
        return eq_result.__not__();
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject dict___lt__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result < 0 ? Py.True : Py.False;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject dict___gt__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result > 0 ? Py.True : Py.False;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject dict___le__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result <= 0 ? Py.True : Py.False;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject dict___ge__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result >= 0 ? Py.True : Py.False;
    }

    public int __cmp__(PyObject ob_other) {
        return dict___cmp__(ob_other);
    }

    @ExposedMethod(type = MethodType.CMP)
    final int dict___cmp__(PyObject ob_other) {
        PyType thisType = getType();
        PyType otherType = ob_other.getType();
        if (otherType != thisType && !thisType.isSubType(otherType) 
                && !otherType.isSubType(thisType)) {
            return -2;
        }
        PyDictionary other = (PyDictionary)ob_other;
        int an = table.size();
        int bn = other.table.size();
        if (an < bn) return -1;
        if (an > bn) return 1;

        PyList akeys = keys();
        PyList bkeys = other.keys();

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
            if(avalue == null){
                if(bvalue == null){
                    continue;
                }
                return -3;
            }else if(bvalue == null){
                return -3;
            }
            c = avalue._cmp(bvalue);
            if (c != 0)
                return c;
        }
        return 0;
    }

    /**
     * Return true if the key exist in the dictionary.
     */
    public boolean has_key(PyObject key) {
        return dict_has_key(key);
    }

    @ExposedMethod
    final boolean dict_has_key(PyObject key) {
        return table.containsKey(key);
    }

    public boolean __contains__(PyObject o) {
        return dict___contains__(o);
    }

    @ExposedMethod
    final boolean dict___contains__(PyObject o) {
        return dict_has_key(o);
    }

    /**
     * Return this[key] if the key exists in the mapping, default_object
     * is returned otherwise.
     *
     * @param key            the key to lookup in the dictionary.
     * @param default_object the value to return if the key does not
     *                       exists in the mapping.
     */
    public PyObject get(PyObject key, PyObject default_object) {
        return dict_get(key,default_object);
    }

    @ExposedMethod(defaults = "Py.None")
    final PyObject dict_get(PyObject key, PyObject default_object) {
        PyObject o = table.get(key);
        if (o == null)
            return default_object;
        else
            return o;
    }

    /**
     * Return this[key] if the key exists in the mapping, None
     * is returned otherwise.
     *
     * @param key  the key to lookup in the dictionary.
     */
    public PyObject get(PyObject key) {
        return dict_get(key, Py.None);
    }

    /**
     * Return a shallow copy of the dictionary.
     */
    public PyDictionary copy() {
        return dict_copy();
    }

    @ExposedMethod
    final PyDictionary dict_copy() {
        return new PyDictionary(table); // no need to clone()
    }

    /**
     * Remove all items from the dictionary.
     */
    public void clear() {
        dict_clear();
    }

    @ExposedMethod
    final void dict_clear() {
        table.clear();
    }

    /**
     * Insert all the key:value pairs from <code>d</code> into
     * this dictionary.
     */
    public void update(PyObject other) {
        dict_update(new PyObject[] {other}, Py.NoKeywords);
    }

    @ExposedMethod
    final void dict_update(PyObject[] args, String[] keywords) {
        updateCommon(args, keywords, "update");
    }

    private void updateCommon(PyObject[] args, String[] keywords, String methName) {
        int nargs = args.length - keywords.length;
        if (nargs > 1) {
            throw PyBuiltinCallable.DefaultInfo.unexpectedCall(nargs, false, methName, 0, 1);
        }
        if (nargs == 1) {
            PyObject arg = args[0];
            if (arg.__findattr__("keys") != null) {
                merge(arg);
            } else {
                mergeFromSeq(arg);
            }
        }
        for (int i = 0; i < keywords.length; i++) {
            dict___setitem__(Py.newString(keywords[i]), args[nargs + i]);
        }        
    }

    /**
     * Merge another PyObject that supports keys() with this
     * dict.
     *
     * @param other a PyObject with a keys() method
     */
    private void merge(PyObject other) {
        if (other instanceof PyDictionary) {
            table.putAll(((PyDictionary)other).table);
        } else if (other instanceof PyStringMap) {
            mergeFromKeys(other, ((PyStringMap)other).keys());
        } else {
            mergeFromKeys(other, other.invoke("keys"));
        }
    }

    /**
     * Merge another PyObject via its keys() method
     *
     * @param other a PyObject with a keys() method
     * @param keys the result of other's keys() method
     */
    private void mergeFromKeys(PyObject other, PyObject keys) {
        for (PyObject key : keys.asIterable()) {
            dict___setitem__(key, other.__getitem__(key));
        }
    }

    /**
     * Merge any iterable object producing iterable objects of length
     * 2 into this dict.
     *
     * @param other another PyObject
     */
    private void mergeFromSeq(PyObject other) {
        PyObject pairs = other.__iter__();
        PyObject pair;

        for (int i = 0; (pair = pairs.__iternext__()) != null; i++) {
            try {
                pair = PySequence.fastSequence(pair, "");
            } catch(PyException pye) {
                if (Py.matchException(pye, Py.TypeError)) {
                    throw Py.TypeError(String.format("cannot convert dictionary update sequence "
                                                     + "element #%d to a sequence", i));
                }
                throw pye;
            }
            int n;
            if ((n = pair.__len__()) != 2) {
                throw Py.ValueError(String.format("dictionary update sequence element #%d "
                                                  + "has length %d; 2 is required", i, n));
            }
            dict___setitem__(pair.__getitem__(0), pair.__getitem__(1));
        }
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with
     * a None value and return None.
     *
     * @param key   the key to lookup in the dictionary.
     */
    public PyObject setdefault(PyObject key) {
        return dict_setdefault(key, Py.None);
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with
     * the value of failobj and return failobj
     *
     * @param key     the key to lookup in the dictionary.
     * @param failobj the default value to insert in the dictionary
     *                if key does not already exist.
     */
    public PyObject setdefault(PyObject key, PyObject failobj) {
        return dict_setdefault(key,failobj);
    }

    @ExposedMethod(defaults = "Py.None")
    final PyObject dict_setdefault(PyObject key, PyObject failobj) {
        PyObject oldValue = table.putIfAbsent(key, failobj);
        if (oldValue == null) {
            return failobj;
        } else {
            return oldValue;
        }
    }

    @ExposedMethod(defaults = "Py.None")
    final PyObject dict_setifabsent(PyObject key, PyObject failobj) {
        PyObject oldValue = table.putIfAbsent(key, failobj);
        if (oldValue == null) {
            return Py.None;
        } else {
            return oldValue;
        }
    }

    
    /**
     * Return a value based on key
     * from the dictionary.
     */
    public PyObject pop(PyObject key) {
        return dict_pop(key, null);
    }

    /**
     * Return a value based on key
     * from the dictionary or default if that key is not found.
     */
    public PyObject pop(PyObject key, PyObject defaultValue) {
        return dict_pop(key, defaultValue);
    }

    @ExposedMethod(defaults = "null")
    final PyObject dict_pop(PyObject key, PyObject defaultValue) {
        if (!table.containsKey(key)) {
            if (defaultValue == null) {
                throw Py.KeyError("popitem(): dictionary is empty");
            }
            return defaultValue;
        }
        return table.remove(key);
    }


    /**
     * Return a random (key, value) tuple pair and remove the pair
     * from the dictionary.
     */
    public PyObject popitem() {
        return dict_popitem();
    }

    @ExposedMethod
    final PyObject dict_popitem() {
        Iterator it = table.entrySet().iterator();
        if (!it.hasNext())
            throw Py.KeyError("popitem(): dictionary is empty");
        Entry entry = (Entry)it.next();
        PyTuple tuple = new PyTuple(
                (PyObject)entry.getKey(), (PyObject)entry.getValue());
        it.remove();
        return tuple;
    }

    /**
     * Return a copy of the dictionarys list of (key, value) tuple
     * pairs.
     */
    public PyList items() {
        return dict_items();
    }

    @ExposedMethod
    final PyList dict_items() {
        List<PyObject> list = new ArrayList<PyObject>(table.size());
        for (Entry<PyObject, PyObject> entry : table.entrySet()) {
            list.add(new PyTuple(entry.getKey(), entry.getValue()));
        }
        return new PyList(list);
    }

    /**
     * Return a copy of the dictionarys list of keys.
     */
    public PyList keys() {
        return dict_keys();
    }

    @ExposedMethod
    final PyList dict_keys() {
        return new PyList(new ArrayList<PyObject>(table.keySet()));
    }

    @ExposedMethod
    final PyList dict_values() {
        return new PyList(new ArrayList<PyObject>(table.values()));
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject iteritems() {
        return dict_iteritems();
    }

    @ExposedMethod
    final PyObject dict_iteritems() {
        return new ItemsIter(table.entrySet());
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject iterkeys() {
        return dict_iterkeys();
    }

    @ExposedMethod
    final PyObject dict_iterkeys() {
        return new ValuesIter(table.keySet());
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject itervalues() {
        return dict_itervalues();
    }

    @ExposedMethod
    final PyObject dict_itervalues() {
        return new ValuesIter(table.values());
    }

    public int hashCode() {
        return dict___hash__();
    }

    @ExposedMethod
    final int dict___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
    }

    public boolean isSequenceType() {
        return false;
    }

    class ValuesIter extends PyIterator {

        private final Iterator<PyObject> iterator;

        private final int size;

        public ValuesIter(Collection<PyObject> values) {
            iterator = values.iterator();
            size = values.size();
        }

        public PyObject __iternext__() {
            if (table.size() != size) {
                throw Py.RuntimeError("dictionary changed size during iteration");
            }
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next();
        }
    }

    class ItemsIter extends PyIterator {

        private final Iterator<Entry<PyObject, PyObject>> iterator;

        private final int size;

        public ItemsIter(Set<Entry<PyObject, PyObject>> items) {
            iterator = items.iterator();
            size = items.size();
        }
        
        public PyObject __iternext__() {
            if (table.size() != size) {
                throw Py.RuntimeError("dictionary changed size during iteration");
            }
            if (!iterator.hasNext()) {
                return null;
            }
            Entry<PyObject, PyObject> entry = iterator.next();
            return new PyTuple(entry.getKey(), entry.getValue());
        }
    }

    /* The following methods implement the java.util.Map interface
    which allows PyDictionary to be passed to java methods that take
    java.util.Map as a parameter.  Basically, the Map methods are a
    wrapper around the PyDictionary's Map container stored in member
    variable 'table'. These methods simply convert java Object to
    PyObjects on insertion, and PyObject to Objects on retrieval. */

    /** @see java.util.Map#entrySet() */
    public Set entrySet() {
        return new PyMapEntrySet(table.entrySet());
    }

    /** @see java.util.Map#keySet() */
    public Set keySet() {
        return new PyMapKeyValSet(table.keySet());
    }

    /** Return a copy of the dictionarys list of values. */
    public Collection values() {
        return new PyMapKeyValSet(table.values());
    }

  
    /** @see java.util.Map#putAll(Map map) */
    public void putAll(Map map) {
        Iterator i = map.entrySet().iterator();
        while (i.hasNext()) {
            Entry entry = (Entry)i.next();
            table.put(Py.java2py(entry.getKey()), Py.java2py(entry.getValue()));
        }
    }

    /** @see java.util.Map#remove(Object key) */
    public Object remove(Object key) {
        return tojava(table.remove(Py.java2py(key)));
    }

    /** @see java.util.Map#put(Object key, Object value) */
    public Object put(Object key, Object value) {
        return tojava(table.put(Py.java2py(key), Py.java2py(value)));
    }

    /** @see java.util.Map#get(Object key) */
    public Object get(Object key) {
        return tojava(table.get(Py.java2py(key)));
    }

    /** @see java.util.Map#containsValue(Object key) */
    public boolean containsValue(Object value) {
        return table.containsValue(Py.java2py(value));
    }

    /** @see java.util.Map#containsValue(Object key) */
    public boolean containsKey(Object key) {
        return table.containsKey(Py.java2py(key));
    }
    
    /** @see java.util.Map#isEmpty(Object key) */
    public boolean isEmpty() {
        return table.isEmpty();
    }
    
    /** @see java.util.Map#size(Object key) */
    public int size() {
        return table.size();
    }

    /** Convert return values to java objects */
    static final Object tojava(Object val) {
        return val == null ? null : ((PyObject)val).__tojava__(Object.class);
    }

    public Object putIfAbsent(Object key, Object value) {
        return tojava(table.putIfAbsent(Py.java2py(key), Py.java2py(value)));
    }

    public boolean remove(Object key, Object value) {
        return table.remove(Py.java2py(key), Py.java2py(value));
    }

    public boolean replace(Object key, Object oldValue, Object newValue) {
        return table.replace(Py.java2py(key), Py.java2py(oldValue), Py.java2py(newValue));
    }

    public Object replace(Object key, Object value) {
        return tojava(table.replace(Py.java2py(key), Py.java2py(value)));
    }

}
/** Basic implementation of Entry that just holds onto a key and value and returns them. */
class SimpleEntry<K, V> implements Entry<K, V> {
    
    public SimpleEntry(K key, V value){
        this.key = key;
        this.value = value;
    }
    
    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public boolean equals(Object o) {
        if(!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry e = (Map.Entry)o;
        return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public int hashCode() {
        return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

    public String toString() {
        return key + "=" + value;
    }

    public V setValue(V val) {
        throw new UnsupportedOperationException("Not supported by this view");
    }

    protected K key;

    protected V value;
}

/**
 * Wrapper for a Entry object returned from the java.util.Set
 * object which in turn is returned by the entrySet method of
 * java.util.Map.  This is needed to correctly convert from PyObjects
 * to java Objects.  Note that we take care in the equals and hashCode
 * methods to make sure these methods are consistent with Entry
 * objects that contain java Objects for a value so that on the java
 * side they can be reliable compared.
 */
class PyToJavaMapEntry extends SimpleEntry {

    /** Create a copy of the Entry with Py.None coverted to null */
    PyToJavaMapEntry(Entry entry) {
        super(entry.getKey(), entry.getValue());
    }
    
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Entry)) return false;
        Entry me = new JavaToPyMapEntry((Entry)o);
        return o.equals(me);
    }

    // tojava is called in getKey and getValue so the raw key and value can be
    // used to create a new SimpleEntry in getEntry.
    public Object getKey() {
        return PyDictionary.tojava(key);
    }
    
    public Object getValue() {
        return PyDictionary.tojava(value);
    }

    /**
     * @return an entry that returns the original values given to this entry.
     */
    public Entry getEntry() {
        return new SimpleEntry(key, value);
    }

}

/**
 * MapEntry Object for java MapEntry objects passed to the java.util.Set
 * interface which is returned by the entrySet method of PyDictionary.
 * Essentially like PyTojavaMapEntry, but going the other way converting java
 * Objects to PyObjects.
 */
class JavaToPyMapEntry extends SimpleEntry {
    
    public JavaToPyMapEntry(Entry entry) {
        super(Py.java2py(entry.getKey()), Py.java2py(entry.getValue()));
    }
}

/**
 *  Wrapper collection class for the keySet and values methods of
 *  java.util.Map
 */
class PyMapKeyValSet extends PyMapSet {
    
    PyMapKeyValSet(Collection coll) {
        super(coll);
    }

    Object toJava(Object o) {
        return PyDictionary.tojava(o);
    }
    
    Object toPython(Object o) {
        return Py.java2py(o);
    }
}

/**
 * Set wrapper for the java.util.EntrySet method. Entry
 * objects are wrapped further in JavaToPyMapEntry and
 * PyToJavaMapEntry.  Note - The set interface is reliable for
 * standard objects like strings and integers, but may be inconstant
 * for other types of objects since the equals method may return false
 * for Entry object that hold more elaborate PyObject types.
 * However, We insure that this iterface works when the Entry
 * object originates from a Set object retrieved from a PyDictionary.
 */
class PyMapEntrySet extends PyMapSet {

    PyMapEntrySet(Collection coll) {
        super(coll);
    }

    // We know that PyMapEntrySet will only contains Entrys, so
    // if the object being passed in is null or not a Entry, then
    // return null which will match nothing for remove and contains methods.
    Object toPython(Object o) {
        if(o == null || !(o instanceof Entry))
            return null;
        if(o instanceof PyToJavaMapEntry) {
            // Use the original entry from PyDictionary
            return ((PyToJavaMapEntry)o).getEntry();
        } else {
            return new JavaToPyMapEntry((Entry)o);
        }
    }

    Object toJava(Object o) {
        return new PyToJavaMapEntry((Entry)o);
    }
}

/**
 * PyMapSet serves as a wrapper around Set Objects returned by the
 * java.util.Map interface of PyDictionary. entrySet, values and
 * keySet methods return this type for the java.util.Map
 * implementation.  This class is necessary as a wrapper to convert
 * PyObjects to java Objects for methods that return values, and
 * convert Objects to PyObjects for methods that take values. The
 * translation is necessary to provide java access to jython
 * dictionary objects. This wrapper also provides the expected backing
 * functionality such that changes to the wrapper set or reflected in
 * PyDictionary.
 */
abstract class PyMapSet extends AbstractSet {

    PyMapSet(Collection coll) {
        this.coll = coll;
    }

    abstract Object toJava(Object obj);

    abstract Object toPython(Object obj);

    public int size() {
        return coll.size();
    }
    
    public boolean contains(Object o) {
        return coll.contains(toPython(o));
    }
    
     public boolean remove(Object o) {
         return coll.remove(toPython(o));
    }
    
    public void clear() {
        coll.clear();
    }

    // Iterator wrapper class returned by the PyMapSet iterator
    // method. We need this wrapper to return PyToJavaMapEntry objects
    // for the 'next()' method.
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
    
    public Iterator iterator() {
        return new PySetIter(coll.iterator());
    }
    
    private final Collection coll;
}
