package org.python.core;
import java.util.Vector;

public class PyClass extends PyObject {
    /**
    Holds the namespace for this class
    **/
	public PyObject __dict__;
	
	/**
	The base classes of this class
	**/
	public PyTuple __bases__;
	
	/**
	The name of this class
	**/
	public String __name__;
	
	// Store these methods for performance optimization
	// These are only used by PyInstance
	PyObject __getattr__, __setattr__, __delattr__, __tojava__;

    // Holds the classes for which this is a proxy
    // Only used when subclassing from a Java class
	protected Class proxyClass;

    public static PyClass __class__;
    
    PyClass(boolean fakeArg) { super(fakeArg); proxyClass = null; }
    
    public PyClass() { super(__class__); proxyClass = null; }
    public PyClass(PyClass c) { super(c); proxyClass = null; }
	public PyClass(String name, PyTuple bases, PyObject dict) {
	    this();
	    init(name, bases, dict);
	}
	
	protected Class getProxyClass() {
	    return proxyClass;
	}

	void init(String name, PyTuple bases, PyObject dict) {
	    //System.out.println("bases: "+bases+", "+name.string);
	    //System.out.println("init class: "+name);
		__name__ = name;
		__bases__ = bases;
		__dict__ = dict;
		
		if (proxyClass == null) {
		    Vector interfaces = new Vector();
		    Class baseClass = null;
    		for (int i=0; i<bases.list.length; i++) {
    			Class previousProxy = ((PyClass)bases.list[i]).getProxyClass();
    			if (previousProxy != null) {
    			    if (previousProxy.isInterface()) {
    			        interfaces.addElement(previousProxy);
    			    } else {
    			        if (baseClass != null) {
    			            throw Py.TypeError("no multiple inheritance for Java classes: "+
    			                        previousProxy.getName()+" and "+baseClass.getName());
    			        }
    			        baseClass = previousProxy;
    			    }
    			}
    		}
    		if (baseClass != null || interfaces.size() != 0) {
    		    proxyClass = MakeProxies.makeProxy(baseClass, interfaces, __name__, __dict__);
    		}
    	}
    	
    	if (proxyClass != null) {
            PyObject superDict = PyJavaClass.lookup(proxyClass).__findattr__("__dict__");
            // This code will add in the needed super__ methods to the class
            if (superDict instanceof PyStringMap && dict instanceof PyStringMap) {
                PyStringMap superMap = ((PyStringMap)superDict).copy();
                superMap.update((PyStringMap)dict);
                dict = superMap;
                __dict__ = dict;
            }
    	}
    	//System.out.println("proxyClasses: "+proxyClasses+", "+proxyClasses[0]);

        if (dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", Py.None);
        }

        findModule(dict);

        // Setup cached references to methods where performance really counts
		__getattr__ = lookup("__getattr__", false);
		__setattr__ = lookup("__setattr__", false);
		__delattr__ = lookup("__delattr__", false);
		__tojava__ = lookup("__tojava__", false);
	}
	
	protected void findModule(PyObject dict) {
        if (dict.__finditem__("__module__") == null) {
            //System.out.println("in PyClass getFrame: "+__name__.string);
            PyFrame f = Py.getFrame();
            if (f != null) {
                PyObject nm = f.f_globals.__finditem__("__name__");
                if (nm != null)
                    dict.__setitem__("__module__", nm);
            }
        }
    }
	    

	public Object __tojava__(Class c) {
		if ((c == Object.class || c == Class.class) && proxyClass != null) {
		    return proxyClass;
		}
		return super.__tojava__(c);
	}	
	

	PyObject lookup(String name, boolean stop_at_java) {
		PyObject result;
		result = __dict__.__finditem__(name);
		if (result == null && __bases__ != null) {
		    int n = __bases__.__len__();
			for(int i=0; i<n; i++) {
				result = ((PyClass)__bases__.__getitem__(i)).lookup(name, stop_at_java);
				if (result != null) break;
			}
		}
		return result;
	}
	
	public PyObject __findattr__(String name) {
        if (name == "__dict__") return __dict__;
        if (name == "__name__") return new PyString(__name__);
        if (name == "__bases__") return __bases__;
    
	    PyObject result = lookup(name, false);
	    
		if (result == null) return super.__findattr__(name);
		return result._doget(null);
	}

	public void __setattr__(String name, PyObject value) {
	    if (name == "__dict__" || name == "__name__" || name == "__bases__") {
	        throw Py.TypeError("read-only special attribute: "+name);
	    }
		__dict__.__setitem__(name, value);
	}
	
	public void __delattr__(String name) {
		__dict__.__delitem__(name);
	}

	public PyObject __call__(PyObject[] args, String[] keywords) {
		PyInstance inst = new PyInstance(this);
		inst.__init__(args, keywords);
		return inst;
	}

    /* PyClass's are compared based on __name__ */
	public int __cmp__(PyObject other) {
		if (!(other instanceof PyClass)) return -2;

	    int c = __name__.compareTo(((PyClass)other).__name__);
		return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

	public String toString()  {
	    PyObject mod = __dict__.__finditem__("__module__");
	    String smod;
	    if (mod == null || !(mod instanceof PyString)) smod = "<unknown>";
	    else smod = ((PyString)mod).toString();
		return "<class "+smod+'.'+__name__+" at "+Py.id(this)+">";
	}
}
