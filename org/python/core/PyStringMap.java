package org.python.core;

public class PyStringMap extends PyObject {
    //Table of primes to cycle through
    private static final int[] primes = {
       7, 13, 31, 61, 127, 251, 509, 1021, 2017, 4093,
        5987, 9551, 15683, 19609, 31397,
        65521, 131071, 262139, 524287, 1048573, 2097143,
        4194301, 8388593, 16777213, 33554393, 67108859,
        134217689, 268435399, 536870909, 1073741789,};

    private transient String[] keys;
    private transient PyObject[] values;
    private int size;
    private transient int filled;
    private transient int prime;

    /* Override serialization behavior */
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
        
	    String[] keyTable = keys;
	    PyObject[] valueTable = values;
	    int n = keyTable.length;

	    for(int i=0; i<n; i++) {
	        //String key = keyTable[i];
	        PyObject value = valueTable[i];
	        if (value == null) continue;
	        out.writeUTF(keys[i]);
	        out.writeObject(values[i]);
	    }
    }
    
    
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        prime = 1;
        keys = null;
        values = null;
        int n = size;
        
        resize(n);
        
	    for(int i=0; i<n; i++) {
	        String key = in.readUTF().intern();
	        insertkey(key, (PyObject)in.readObject());
	    }
    }
    public static PyClass __class__;

	public PyStringMap(int capacity) {
	    super(__class__);
	    prime = 0;
	    keys = null;
	    values = null;
	    resize(capacity);
	}

	public PyStringMap() {
	    this(4);
	}

	public PyStringMap(PyObject elements[]) {
		this(elements.length);
		for (int i=0;i<elements.length;i+=2) {
		    __setitem__(elements[i], elements[i+1]);
		}
	}
	
	/*public int debug(PyString pkey) {
	    String key = pkey.internedString(); 
	    
	    String[] table = keys;
	    int maxindex = table.length;
	    int index = (System.identityHashCode(key) & 0x7FFFFFFF) % maxindex;

        for(int i=0; i<maxindex; i++) {
            String k = table[i];
            if (k != null && k.equals(key)) {
                System.err.println("found at: "+i+", "+System.identityHashCode(k)+", "+System.identityHashCode(key));
            }
        }

        // Fairly aribtrary choice for stepsize...
	    int stepsize = maxindex / 5;
	    
	    // Cycle through possible positions for the key;
	    System.err.println("key: "+key+", "+index+", "+stepsize+", "+maxindex);
	    int iters = 0;
	    while (iters++ < maxindex) {
	        String tkey = table[index];
	        System.err.println("tkey: "+tkey+", "+index);
	        if (tkey == key || tkey == null) return index;
	        index = (index+stepsize) % maxindex;
	    }
	    
	    index = (System.identityHashCode(key) & 0x7FFFFFFF) % maxindex;
	    throw new PyValueError("Internal Error in StringMap :"+maxindex+", "+size+", "+stepsize+", "+index);
	}*/    
	

	/*private final int lookup(String key) {
	    String[] table = keys;
	    int maxindex = table.length;
	    int index = (System.identityHashCode(key) & 0x7FFFFFFF) % maxindex;

        // Fairly aribtrary choice for stepsize...
	    int stepsize = maxindex / 5;

	    // Cycle through possible positions for the key;
	    int iters = 0;
	    while (iters++ < maxindex) {
	        String tkey = table[index];
	        if (tkey == key || tkey == null) return index;
	        index = (index+stepsize) % maxindex;
	    }
	    
	    index = (System.identityHashCode(key) & 0x7FFFFFFF) % maxindex;
	    throw new PyValueError("Internal Error in StringMap :"+maxindex+", "+size+", "+stepsize+", "+index);
	}*/


	public synchronized int __len__() {
		return size;
	}

	public synchronized boolean __nonzero__() {
		return size != 0;
	}

    public synchronized PyObject __finditem__(String key) {
	    String[] table = keys;
	    int maxindex = table.length;
	    int index = (System.identityHashCode(key) & 0x7fffffff) % maxindex;

        // Fairly aribtrary choice for stepsize...
	    int stepsize = maxindex / 5;

	    // Cycle through possible positions for the key;
	    //int collisions = 0;
	    while (true) {
	        String tkey = table[index];
	        if (tkey == key) {
	            //if (collisions > 0) {
	            //    System.err.println("key: "+key+", "+collisions+", "+maxindex+", "+System.identityHashCode(key));
	            //}
	            return values[index];
	        }
	        if (tkey == null) return values[index];
	        //collisions++;
	        index = (index+stepsize) % maxindex;
	    }
    }

	public PyObject __finditem__(PyObject key) {
	    //System.err.println("oops: "+key);
	    if (key instanceof PyString) {
	        return __finditem__(((PyString)key).internedString());
	    } else {
	        return null;
	    }
	}

    private final void insertkey(String key, PyObject value) {
	    String[] table = keys;
	    int maxindex = table.length;
	    int index = (System.identityHashCode(key) & 0x7fffffff) % maxindex;

        // Fairly aribtrary choice for stepsize...
	    int stepsize = maxindex / 5;

	    // Cycle through possible positions for the key;
	    while (true) {
	        String tkey = table[index];
	        if (tkey == null) {
	            table[index] = key;
	            values[index] = value;
	            filled++;
	            size++;
	            break;
	        } else if (tkey == key) {
	            values[index] = value;
	            break;
	        } else if (tkey == "<deleted key>") {
	            table[index] = key;
	            values[index] = value;
	            size++;
	            break;
	        }
	        index = (index+stepsize) % maxindex;
	    }
    }

	private synchronized final void resize(int capacity) {
	    int p = prime;
	    for(; p<primes.length; p++) {
	        if (primes[p] >= capacity) break;
	    }
	    if (primes[p] < capacity) {
	        throw Py.ValueError("can't make hashtable of size: "+capacity);
	    }
	    //System.err.println("resize: "+(keys != null ? keys.length : -1)+", "+primes[p]);
	    capacity = primes[p];
	    prime = p;

	    String[] oldKeys = keys;
	    PyObject[] oldValues = values;

	    keys = new String[capacity];
	    values = new PyObject[capacity];
	    size = 0;
	    filled = 0;

        if (oldValues != null) {
            int n = oldValues.length;

    	    for(int i=0; i<n; i++) {
    	        PyObject value = oldValues[i];
    	        if (value == null) continue;
    	        insertkey(oldKeys[i], value);
    	    }
    	}
	}

	public synchronized void __setitem__(String key, PyObject value) {
	    if (2*filled > keys.length) resize(keys.length+1);
	    insertkey(key, value);
	}

	public void __setitem__(PyObject key, PyObject value) {
	    if (key instanceof PyString) {
	        __setitem__(((PyString)key).internedString(), value);
	    } else {
	        throw Py.TypeError("keys in namespace must be strings");
	    }
	}

    public synchronized void __delitem__(String key) {
	    String[] table = keys;
	    int maxindex = table.length;
	    int index = (System.identityHashCode(key) & 0x7fffffff) % maxindex;

        // Fairly aribtrary choice for stepsize...
	    int stepsize = maxindex / 5;

	    // Cycle through possible positions for the key;
	    while (true) {
	        //System.err.println("key: "+key+", "+index+", "+table[index]);
	        String tkey = table[index];
	        if (tkey == null) {
	            //System.err.println("key: "+key+", "+System.identityHashCode(key));
	            //for(int i=0; i<keys.length; i++) {
	                //System.err.println("k: "+keys[i]+", "+values[i]+", "+System.identityHashCode(keys[i]));
	            //}
	            throw Py.KeyError(key);
	        }
	        if (tkey == key) {
	            table[index] = "<deleted key>";
	            values[index] = null;
	            size--;
	            break;
	        }
	        index = (index+stepsize) % maxindex;
	    }
    }

	public void __delitem__(PyObject key) {
	    if (key instanceof PyString) {
	        __delitem__(((PyString)key).internedString());
	    } else {
	        throw Py.KeyError(key.toString());
	    }
	}
	
	public synchronized void clear() {
	    for(int i=0; i<keys.length; i++) {
	        keys[i] = null;
	        values[i] = null;
	    }
	    size = 0;
	}
	

	public synchronized String toString() {
	    String[] keyTable = keys;
	    PyObject[] valueTable = values;
	    int n = keyTable.length;
	    
	    StringBuffer buf = new StringBuffer("{");

	    for(int i=0; i<n; i++) {
	        //String key = keyTable[i];
	        PyObject value = valueTable[i];
	        if (value == null) continue;
	        buf.append("'");
	        buf.append(keyTable[i]);
	        buf.append("': ");
	        buf.append(value.__repr__().toString());
	        buf.append(", ");
	    }
	    
	    // A hack to remove the final ", " from the string repr
	    int len = buf.length();
	    if (len > 4) {
	        buf.setLength(len-2);
	    }
	    
	    buf.append("}");
	    return buf.toString();
	}        	   

	public synchronized int __cmp__(PyObject other) {
	    if (!(other instanceof PyStringMap || other instanceof PyDictionary)) {
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

		for(int i=0; i<bn; i++) {
		    PyObject akey = akeys.get(i);
		    PyObject bkey = bkeys.get(i);
		    int c = akey._cmp(bkey);
		    if (c != 0) return c;

		    PyObject avalue = __finditem__(akey);
		    PyObject bvalue = other.__finditem__(bkey);
		    c = avalue._cmp(bvalue);
		    if (c != 0) return c;
		}
		return 0;
	}

	public boolean has_key(PyObject key) {
	    return __finditem__(key) != null;
	}

	public PyObject get(PyObject key, PyObject default_object) {
		PyObject o = __finditem__(key);
		if (o == null) return default_object;
		else return o;
	}

	public PyObject get(PyObject key) {
		return get(key, Py.None);
	}

    public synchronized PyStringMap copy() {
        int n = keys.length;
        
        PyStringMap map = new PyStringMap(n);
        System.arraycopy(keys, 0, map.keys, 0, n);
        System.arraycopy(values, 0, map.values, 0, n);
        
        map.filled = filled;
        map.size = size;
        map.prime = prime;
        
        return map;
    }
    
    public synchronized void update(PyStringMap map) {
	    String[] keyTable = map.keys;
	    PyObject[] valueTable = map.values;
	    int n = keyTable.length;
	    
	    for(int i=0; i<n; i++) {
	        String key = keyTable[i];
	        if (key == null || key == "<deleted key>") continue;
	        insertkey(key, valueTable[i]);
	    }
    }
    
	public void update(PyDictionary dict) {
		java.util.Hashtable table = dict.table;

		java.util.Enumeration ek = table.keys();
		java.util.Enumeration ev = table.elements();
		int n = table.size();

		for(int i=0; i<n; i++) {
		    __setitem__((PyObject)ek.nextElement(), (PyObject)ev.nextElement());
		}
	}

	public synchronized PyList items() {
	    String[] keyTable = keys;
	    PyObject[] valueTable = values;
	    int n = keyTable.length;
	    
	    PyList l = new PyList();
	    for(int i=0; i<n; i++) {
	        String key = keyTable[i];
	        if (key == null || key == "<deleted key>") continue;
	        l.append(new PyTuple(new PyObject[] {new PyString(key), valueTable[i]}));
	    }
	    
	    return l;
	}
	

	synchronized String[] jkeys() {
	    String[] keyTable = keys;
	    //PyObject[] valueTable = values;
	    int n = keyTable.length;
	    
	    String[] newKeys = new String[size];
	    int j=0;

	    for(int i=0; i<n; i++) {
	        String key = keyTable[i];
	        if (key == null || key == "<deleted key>") continue;
	        newKeys[j++] = key;
	    }
	    
	    return newKeys;
	}


	public synchronized PyList keys() {
	    String[] keyTable = keys;
	    //PyObject[] valueTable = values;
	    int n = keyTable.length;
	    
	    PyList l = new PyList();
	    for(int i=0; i<n; i++) {
	        String key = keyTable[i];
	        if (key == null || key == "<deleted key>") continue;
	        l.append(new PyString(key));
	    }
	    
	    return l;
	}

	public synchronized PyList values() {
	    PyObject[] valueTable = values;
	    int n = valueTable.length;
	    
	    PyList l = new PyList();
	    for(int i=0; i<n; i++) {
	        PyObject value = valueTable[i];
	        if (value == null) continue;
	        l.append(value);
	    }
	    
	    return l;
	}
}
