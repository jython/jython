package org.python.core;
import java.util.*;

class CollectionProxy {
    public static final CollectionProxy NoProxy = new EnumerationProxy(null);
    
    public static CollectionProxy findCollection(Object[] objects) {
        if (objects == null) return NoProxy;
        
        for(int i=0; i<objects.length; i++) {
            Object o = objects[i];
            if (o instanceof Vector) return new VectorProxy(((Vector)o));
            if (o instanceof Enumeration) return new EnumerationProxy(((Enumeration)o));
            if (o instanceof Dictionary) return new DictionaryProxy(((Dictionary)o));
            
            // for jdk 1.2 do: o instanceof Map, List, Iterator, ???
        }
        return NoProxy;
    }

	/**The basic functions to implement a mapping**/
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
		if (ret == null) throw Py.KeyError(""+key);
		return ret;
	}

	public PyObject __getitem__(PyObject key) {
		PyObject ret = __finditem__(key);
		if (ret == null) throw Py.KeyError(key.toString());
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
        counter=0;
    }

	public PyObject __finditem__(int key) {
	    if (key != counter) {
	        throw Py.ValueError("enumeration indices must be consecutive ints starting at 0");
	    }
	    counter++;
	    if (proxy.hasMoreElements()) {
	        return Py.java2py(proxy.nextElement());
	    } else {
	        return null;
	    }
	}

	public PyObject __finditem__(PyObject key) {
	    if (key instanceof PyInteger) {
	        return __finditem__(((PyInteger)key).getValue());
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
		return proxy.size();
	}	
    
    
    public PyObject __finditem__(int key) {
        return Py.java2py(proxy.elementAt(key));
    }
    
	public PyObject __finditem__(PyObject key) {
	    if (key instanceof PyInteger) {
	        return __finditem__(((PyInteger)key).getValue());
	    } else {
		    throw Py.TypeError("only integer keys accepted");
		}
	}
	
	public void __setitem__(PyObject key, PyObject value) {
	    if (key instanceof PyInteger) {
	        proxy.setElementAt(Py.tojava(value, Object.class),
	                ((PyInteger)key).getValue());
	    } else {
		    throw Py.TypeError("only integer keys accepted");
		}
	}
	public void __delitem__(PyObject key) {
	    if (key instanceof PyInteger) {
	        proxy.removeElementAt(((PyInteger)key).getValue());
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
		return proxy.size();
	}
    
	public PyObject __finditem__(PyObject key) {
	    return Py.java2py(proxy.get(Py.tojava(key, Object.class)));
	}
	
	public void __setitem__(PyObject key, PyObject value) {
	    proxy.put(Py.tojava(key, Object.class), Py.tojava(value, Object.class));
	}
	
	public void __delitem__(PyObject key) {
	    proxy.remove(Py.tojava(key, Object.class));
	}
}