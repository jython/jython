package org.python.core;

public class PyDictionary extends PyObject {
	java.util.Hashtable table;

    public static PyClass __class__;
    public PyDictionary(java.util.Hashtable t) {
        super(__class__);
		table = t;
	}

	public PyDictionary() {
		this(new java.util.Hashtable()); //3, 0.9f);
	}

	public PyDictionary(PyObject elements[]) {
		this();
		for (int i=0;i<elements.length;i+=2) {
			table.put(elements[i], elements[i+1]);
		}
	}

	public int __len__() {
		return table.size();
	}

	public boolean __nonzero__() throws PyException {
		return table.size() != 0;
	}

	public PyObject __finditem__(PyObject key) {
		return (PyObject)table.get(key);
	}

	public void __setitem__(PyObject key, PyObject value)  {
		table.put(key, value);
	}

	public void __delitem__(PyObject key) throws PyException {
		table.remove(key);
	}

	public String toString() {
	    ThreadState ts = Py.getThreadState();
	    if (!ts.enterRepr(this)) {
	        return "{...}";
	    }
	    
		java.util.Enumeration ek = table.keys();
		java.util.Enumeration ev = table.elements();
		int n = table.size();
		StringBuffer buf = new StringBuffer("{");

		for(int i=0; i<n; i++) {
			buf.append(((PyObject)ek.nextElement()).__repr__().toString());
			buf.append(": ");
			buf.append(((PyObject)ev.nextElement()).__repr__().toString());
			if (i < n-1) buf.append(", ");
		}
		buf.append("}");
		
        ts.exitRepr(this);
		return buf.toString();
	}

	public int __cmp__(PyObject ob_other) throws PyException {
		if (ob_other.__class__ != __class__) return -2;

		PyDictionary other = (PyDictionary)ob_other;
		int an = table.size();
		int bn = other.table.size();
		if (an < bn) return -1;
		if (an > bn) return 1;

		PyList akeys = keys();
		PyList bkeys = other.keys();

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
		return table.containsKey(key);
	}

	public PyObject get(PyObject key, PyObject default_object) {
		PyObject o = __finditem__(key);
		if (o == null) return default_object;
		else return o;
	}

	public PyObject get(PyObject key) {
		return get(key, Py.None);
	}

	public PyDictionary copy() {
	    return new PyDictionary((java.util.Hashtable)table.clone());
	}

    public void clear() {
        table.clear();
    }

	public void update(PyDictionary d) {
		java.util.Hashtable otable = d.table;

		java.util.Enumeration ek = otable.keys();
		java.util.Enumeration ev = otable.elements();
		int n = otable.size();

		for(int i=0; i<n; i++) table.put(ek.nextElement(), ev.nextElement());
	}

	public PyList items() {
		java.util.Enumeration ek = table.keys();
		java.util.Enumeration ev = table.elements();
		int n = table.size();
		java.util.Vector l = new java.util.Vector(n);

		for(int i=0; i<n; i++)
			l.addElement(new PyTuple(new PyObject[]
				{(PyObject)ek.nextElement(), (PyObject)ev.nextElement()}));
		return new PyList(l);
	}

	public PyList keys() {
		java.util.Enumeration e = table.keys();
		int n = table.size();
		java.util.Vector l = new java.util.Vector(n);

		for(int i=0; i<n; i++) l.addElement(e.nextElement());
		return new PyList(l);
	}

	public PyList values() {
		java.util.Enumeration e = table.elements();
		int n = table.size();
		java.util.Vector l = new java.util.Vector(n);

		for(int i=0; i<n; i++) l.addElement(e.nextElement());
		return new PyList(l);
	}
}
