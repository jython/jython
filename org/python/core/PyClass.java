package org.python.core;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Hashtable;
import org.python.compiler.ProxyMaker;

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
	protected Class[] proxyClasses;

    public static PyClass __class__;
    
    PyClass(boolean fakeArg) { super(fakeArg); proxyClasses = null; }
    
    public PyClass() { super(__class__); proxyClasses = null; }
    public PyClass(PyClass c) { super(c); proxyClasses = null; }
	public PyClass(String name, PyTuple bases, PyObject dict) {
	    this();
	    init(name, bases, dict);
	}

	void init(String name, PyTuple bases, PyObject dict) {
	    //System.out.println("bases: "+bases+", "+name.string);
	    //System.out.println("init class: "+name);
		__name__ = name;
		__bases__ = bases;
		if (proxyClasses == null) {
    		for (int i=0; i<bases.list.length; i++) {
    			Class[] pc = ((PyClass)bases.list[i]).proxyClasses;
    			if (pc != null) {
    				if (bases.list[i] instanceof PyJavaClass) {
    					Class[] new_pc = new Class[pc.length];
    					for(int j=0; j<pc.length; j++) {
    						new_pc[j] = lookupProxy(pc[j]);
    					}
    					pc = new_pc;
    					//System.out.println("proxy: "+PyJavaClass.lookup(pc[0]));
    					PyJavaClass pclass = PyJavaClass.lookup(pc[0]);
    					bases.list[i] = pclass;
    				}
    				if (proxyClasses == null) {
    					proxyClasses = pc;
    				} else {
    					Class[] new_proxy_classes = new Class[proxyClasses.length+pc.length];
    					System.arraycopy(proxyClasses, 0, new_proxy_classes, 0, proxyClasses.length);
    					System.arraycopy(pc, 0, new_proxy_classes, proxyClasses.length, pc.length);
    					proxyClasses = new_proxy_classes;
    				}
    			}
    		}
    	}
		__dict__ = dict;

        if (dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", Py.None);
        }

        if (dict.__finditem__("__module__") == null) {
            //System.out.println("in PyClass getFrame: "+__name__.string);
            PyFrame f = Py.getFrame();
            if (f != null) {
                PyObject nm = f.f_globals.__finditem__("__name__");
                if (nm != null)
                    dict.__setitem__("__module__", nm);
            }
        }

        // Setup cached references to methods where performance really counts
		__getattr__ = lookup("__getattr__", false);
		__setattr__ = lookup("__setattr__", false);
		__delattr__ = lookup("__delattr__", false);
		__tojava__ = lookup("__tojava__", false);
	}
	
	public Object __tojava__(Class c) {
		if (c == Class.class && proxyClasses != null) {
			if (proxyClasses.length == 1) {
				return proxyClasses[0];
			}
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
	
	/* Handle loading and caching of proxy classes
	    Used when subclassing from java classes
	*/
	private static Hashtable proxies = new Hashtable();
	private static final String proxyPrefix = "org.python.proxies.";
	private static final String proxyDirectoryKey = "python.proxy.savedir";
	
	protected static Class lookupProxy(Class c) {
		Object o = proxies.get(c);
		if (o != null) return (Class)o;
		Class pc = Py.findClass(proxyPrefix+c.getName());
		if (pc == null) {
			//No proxy found, will try to create one
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			String name;
			try {
				name = ProxyMaker.makeProxy(c.getName(), bytes);
			} catch (Exception exc) {
			    throw Py.JavaError(exc);
			}
			pc = BytecodeLoader.makeClass(name, bytes.toByteArray());
			String dir = sys.registry.getProperty(proxyDirectoryKey);
			//System.out.println("making proxy: "+dir);
			if (dir != null) {
				try {
					OutputStream file = ProxyMaker.getFile(dir, name);
					bytes.writeTo(file);
				} catch (IOException ioe) {
					throw Py.IOError(ioe);
				}
			}
			//throw Py.Error(new PyTypeError("no proxy available for "+c.getName()));
		}
		proxies.put(c, pc);
		return pc;

	}
}
