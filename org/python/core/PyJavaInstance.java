package org.python.core;
import java.lang.reflect.Modifier;

public class PyJavaInstance extends PyInstance implements java.io.Externalizable {
    public PyJavaInstance() {
        super();
        javaProxies = new Object[1];
    }

	public PyJavaInstance(PyJavaClass iclass) {
		super(iclass, null);
	}
	
	public PyJavaInstance(Object proxy) {
	    super(PyJavaClass.lookup(proxy.getClass()), null);
	    javaProxies[0] = proxy;
	}

    public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
        Object o = in.readObject();
        javaProxies[0] = o;
        __class__ = PyJavaClass.lookup(o.getClass());
    }


    public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
        //System.out.println("writing java instance");
        out.writeObject(javaProxies[0]);
    }


	public void __init__(PyObject[] args, String[] keywords) {
	    PyReflectedConstructor init = ((PyJavaClass)__class__).__init__;
	    javaProxies = new Object[1];
	    if (init == null) {
	        Class[] pc = __class__.proxyClasses;
	        if (pc != null && pc.length == 1) {
	            int mods = pc[0].getModifiers();
	            if (Modifier.isInterface(mods)) {
	                throw Py.TypeError("can't instantiate interface ("+__class__.__name__+")");
	            } else if (Modifier.isAbstract(mods)) {
	                throw Py.TypeError("can't instantiate abstract class ("+__class__.__name__+")");
	            } 
	        }
	        throw Py.TypeError("no public constructors for "+__class__.__name__);
	    }
		init.__call__(this, args, keywords);
	}

    protected void noField(String name, PyObject value) {
		throw Py.TypeError("can't set arbitrary attribute in java instance: "+name);
    }
    
    protected void unassignableField(String name, PyObject value) {
		throw Py.TypeError("can't assign to this attribute in java instance: "+name);
    }

    public int hashCode() {
        if (javaProxies[0] != null) {
            return javaProxies[0].hashCode();
        } else {
            return super.hashCode();
        }
    }
    
    public int __cmp__(PyObject o) {
        if (!(o instanceof PyJavaInstance)) return -2;
        PyJavaInstance i = (PyJavaInstance)o;
        if (javaProxies[0].equals(i.javaProxies[0])) {
            return 0;
        } else {
            return System.identityHashCode(this) < System.identityHashCode(i) ? -1 : +1;
        }
    }


	public PyString __str__() {
        return new PyString(javaProxies[0].toString());
    }

    public PyString __repr__() {
        return __str__();
    }

	public void __delattr__(String attr) {
		throw Py.TypeError("can't delete attr from java instance: "+attr);
	}
}
