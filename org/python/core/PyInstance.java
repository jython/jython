package org.python.core;
import java.util.Hashtable;

public class PyInstance extends PyObject {
	//This field is only used by Python subclasses of Java classes
	Object javaProxy;

    /**
    The namespace of this instance.  Contains all instance attributes.
    **/
	public PyObject __dict__;

    /* Override serialization behavior */
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        String module = in.readUTF();
        String name = in.readUTF();

        /* Check for types and missing members here */
        //System.out.println("module: "+module+", "+name);
		PyObject mod = imp.importName(module.intern(), false);
		PyClass pyc = (PyClass)mod.__getattr__(name.intern());

		__class__ = pyc;
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        //System.out.println("writing: "+getClass().getName());
        out.defaultWriteObject();
        PyObject name = __class__.__findattr__("__module__");
        if (!(name instanceof PyString) || name == Py.None || name == null) {
            throw Py.ValueError("Can't find module for class: "+__class__.__name__);
        }
        out.writeUTF(name.toString());
        name = __class__.__findattr__("__name__");
        if (!(name instanceof PyString) || name == Py.None || name == null) {
            throw Py.ValueError("Can't find module for class with no name");
        }

        out.writeUTF(name.toString());
    }


    /**
    Returns a new 
    **/

	public PyInstance(PyClass iclass, PyObject dict) {
	    super(iclass);
		//__class__ = iclass;
		__dict__ = dict;
		//Prepare array of proxy classes to be possibly filled in in __init__
		/*if (__class__.proxyClass != null) {
		    //System.err.println("proxies: "+__class__.__name__);
			//javaProxies = new Object[__class__.proxyClasses.length];
		}*/
	}

	public PyInstance(PyClass iclass) {
		this(iclass, new PyStringMap());
	}

	public PyInstance() { ; }

    private static Hashtable primitiveMap;


    protected void makeProxy() {
        Class c = __class__.proxyClass;
        PyProxy proxy;
        ThreadState ts = Py.getThreadState();
        try {
            ts.initializingProxy = this;
    	    try {
    	        proxy = (PyProxy)c.newInstance(); //(PyProxy)c.getConstructor(new Class[] {PyInstance.class}).newInstance(new Object[] {this});
            } catch (NoSuchMethodError nsme) {
                throw Py.TypeError("constructor requires arguments");
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        } finally {
            ts.initializingProxy = null;
        }
        //proxy._setPyInstance(this);
        //proxy._setPySystemState(Py.getSystemState());
        javaProxy = proxy;
    }

    /*protected void setProxy(PyProxy proxy, int index) {
        proxy._setPyInstance(this);
        proxy._setPySystemState(Py.getSystemState());
        javaProxies[index] = proxy;
    }*/

	public Object __tojava__(Class c) {
	    if (c == Object.class && javaProxy != null) {
	        return javaProxy;
	    }
		if (c.isInstance(this)) return this;
		
		if (c.isPrimitive()) {
		    if (primitiveMap == null) {
		        primitiveMap = new Hashtable();
		        primitiveMap.put(Character.TYPE, Character.class);
		        primitiveMap.put(Boolean.TYPE, Boolean.class);
		        primitiveMap.put(Byte.TYPE, Byte.class);
		        primitiveMap.put(Short.TYPE, Short.class);
		        primitiveMap.put(Integer.TYPE, Integer.class);
		        primitiveMap.put(Long.TYPE, Long.class);
		        primitiveMap.put(Float.TYPE, Float.class);
		        primitiveMap.put(Double.TYPE, Double.class);
		    }
		    Class tmp = (Class)primitiveMap.get(c);
		    if (tmp != null) c = tmp;
		}
		
		if (javaProxy == null && __class__.proxyClass != null) {
		    makeProxy();
		}
		if (c.isInstance(javaProxy)) return javaProxy;
		
		if (__class__.__tojava__ != null) {
		    //try {
    		    PyObject ret = __class__.__tojava__.__call__(this, PyJavaClass.lookup(c));
    		    if (ret == Py.None) return Py.NoConversion;
    		    if (ret != this) return ret.__tojava__(c);
    		/*} catch (PyException exc) {
    		    System.err.println("Error in __tojava__ method");
    		    Py.printException(exc);
    		}*/
		}
		return Py.NoConversion;
	}

	public void __init__(PyObject[] args, String[] keywords) {
	    /*// Init all interfaces from the start
	    Class proxyClass = __class__.proxyClass;
	    if (proxyClass != null) {
	        if (c.
	        for(int i=0; i<javaProxies.length; i++) {
	            if (javaProxies[i] != null) continue;
	            Class c = __class__.proxyClasses[i];
	            //System.out.println("class: "+c.getSuperclass().getName());
	            // This test is a hack to determine if the proxy class represents
	            // a interface.
	            if (c.getInterfaces().length > 1) {
	                PyProxy proxy;
	                try {
	                    proxy = createProxy(c); //(PyProxy)c.newInstance();
	                } catch (Exception exc) {
	                    throw Py.ValueError("Can't instantiate interface: "+c.getName());
	                }
                    setProxy(proxy, i);
	                //System.out.println("inited interface: "+c.getName());
	            }
	        }
	    }*/

		//Then invoke our own init function
		PyObject init = __class__.lookup("__init__", true);
		PyObject ret = null;
		if (init != null) {
			ret = init.__call__(this, args, keywords);
		}

		if (ret == null) {
			if (args.length != 0) {
			    init = __class__.lookup("__init__", false);
			    if (init != null) {
			        ret = init.__call__(this, args, keywords);
			    } else {
			        throw Py.TypeError("this constructor takes no arguments");
			    }
			}
		} else {
			if (ret != Py.None) {
				throw Py.TypeError("constructor has no return value");
			}
		}

		// Now init all superclasses that haven't already been initialized
		if (javaProxy == null && __class__.proxyClass != null) {
		    makeProxy();
		}
	}
	
	/*private PyProxy createProxy(Class c) {
	    try {
	        return (PyProxy)c.getConstructor(new Class[] {PyInstance.class}).newInstance(new Object[] {this});
	    } catch (Exception exc) {
	        throw Py.JavaError(exc);
	    }
	}*/
	
	/*public PyObject __jgetattr__(String name) {
	    System.err.println("jgetting: "+name);
	    PyObject ret = __findattr__(name, true);
	    if (ret != null) return ret;
	    throw Py.AttributeError(name);
	}*/
	
	public PyObject __jfindattr__(String name) {
	    //System.err.println("jfinding: "+name);
	    return __findattr__(name, true);
	}
	
	public PyObject __findattr__(String name) {
	    return __findattr__(name, false);
	}
	
	public PyObject __findattr__(String name, boolean stopAtJava) {
	    PyObject result = ifindlocal(name);
	    if (result != null) return result;
	    result = ifindclass(name, stopAtJava);
	    if (result != null) return result._doget(this);
	    return ifindfunction(name);
    }
    
    protected PyObject ifindlocal(String name) {
		if (__dict__ == null) return null;
		if (name == "__dict__") return __dict__;
		if (name == "__class__") return __class__;
		
		return __dict__.__finditem__(name);
	}
	
	protected PyObject ifindclass(String name, boolean stopAtJava) {
	    return __class__.lookup(name, stopAtJava);
	}
	
	protected PyObject ifindfunction(String name) {
		PyObject getter = __class__.__getattr__;
		if (getter == null) return null;
		
	    try {
		    return getter.__call__(this, new PyString(name));
		} catch (PyException exc) {
		    if (Py.matchException(exc, Py.AttributeError)) return null;
		    throw exc;
		}
	}
	
	public PyObject invoke(String name) {
		PyObject f = ifindlocal(name);
		if (f == null) {
		    f = ifindclass(name, false);
		    if (f != null) {
		        if (f instanceof PyFunction) {
		            return f.__call__(this);
		        } else {
		            f = f._doget(this);
		        }
		    }
		}
		if (f == null) f = ifindfunction(name);
	    if (f == null) throw Py.AttributeError(name);
	    return f.__call__();
    }
    
	public PyObject invoke(String name, PyObject arg1) {
		PyObject f = ifindlocal(name);
		if (f == null) {
		    f = ifindclass(name, false);
		    if (f != null) {
		        if (f instanceof PyFunction || f instanceof PyBuiltinFunctionSet) {
		            return f.__call__(this, arg1);
		        } else {
		            f = f._doget(this);
		        }
		    }
		}
		if (f == null) f = ifindfunction(name);
	    if (f == null) throw Py.AttributeError(name);
	    return f.__call__(arg1);
    }
    
	public PyObject invoke(String name, PyObject arg1, PyObject arg2) {
		PyObject f = ifindlocal(name);
		if (f == null) {
		    f = ifindclass(name, false);
		    if (f != null) {
		        if (f instanceof PyFunction || f instanceof PyBuiltinFunctionSet) {
		            return f.__call__(this, arg1, arg2);
		        } else {
		            f = f._doget(this);
		        }
		    }
		}
		if (f == null) f = ifindfunction(name);
	    if (f == null) throw Py.AttributeError(name);
	    return f.__call__(arg1, arg2);
    }
	
	
	public void __setattr__(String name, PyObject value) {
		PyObject setter = __class__.__setattr__;
		if (setter != null) {
			setter.__call__(this, new PyString(name), value);
		} else {
		    if (name == "__class__") {
	            if (value instanceof PyClass) {
	                __class__ = (PyClass)value;
	            } else {
	                throw Py.TypeError("__class__ must be set to a class");
	            }
	        } else if (name == "__dict__") {
		        __dict__ = value;
		    } else if (javaProxy != null) {
	            PyObject field = __class__.lookup(name, false);
	            if (field == null) {
	                noField(name, value);
	            } else if (!field._doset(this, value)) {
	                unassignableField(name, value);
	            }
	        } else {
		        __dict__.__setitem__(name, value);
		    }
		}
	}
	
    protected void noField(String name, PyObject value) {
        __dict__.__setitem__(name, value);
    }
    
    protected void unassignableField(String name, PyObject value) {
        __dict__.__setitem__(name, value);
    }
    
	public void __delattr__(String name) throws PyException {
	    // Need code to handle _dodel
		PyObject deller = __class__.__delattr__;
		if (deller != null) {
			deller.__call__(this, new PyString(name));
		} else {
			__dict__.__delitem__(name);
		}
	}

	public PyObject invoke_ex(String name, PyObject[] args, String[] keywords) {
		PyObject meth = __findattr__(name);
		if (meth == null) return null;
		return meth.__call__(args, keywords);
	}
	
	public PyObject invoke_ex(String name) {
		PyObject meth = __findattr__(name);
		if (meth == null) return null;
		return meth.__call__();
	}
	public PyObject invoke_ex(String name, PyObject arg1) {
		PyObject meth = __findattr__(name);
		if (meth == null) return null;
		return meth.__call__(arg1);
	}
	public PyObject invoke_ex(String name, PyObject arg1, PyObject arg2) {
		PyObject meth = __findattr__(name);
		if (meth == null) return null;
		return meth.__call__(arg1, arg2);
	}

	/* __del__ method is invoked upon object finalization. */
	/*protected void finalize() {
	    PyObject delfunc = __class__.__getattr__;
	    if (delfunc != null) {
	        delfunc.__call__(this);
	    }
		//invoke_ex("__del__");
	}*/

	public PyObject __call__(PyObject args[], String keywords[]) {
		return invoke("__call__", args, keywords);
	}

	public PyString __repr__() {
		PyObject ret = invoke_ex("__repr__");
		if (ret == null) {
            PyObject mod = __class__.__dict__.__finditem__("__module__");
    	    String smod;
    	    if (mod == Py.None) smod = "";
    	    else {
        	    if (mod == null || !(mod instanceof PyString)) smod = "<unknown>.";
        	    else smod = ((PyString)mod).toString()+'.';
        	}
			return new PyString("<"+smod+__class__.__name__+" instance at "+Py.id(this)+">");
        }

		if (!(ret instanceof PyString))
			throw Py.TypeError("__repr__ method must return a string");
		return (PyString)ret;
	}

	public PyString __str__() {
		PyObject ret = invoke_ex("__str__");
		if (ret == null) return __repr__();
		if (!(ret instanceof PyString))
			throw Py.TypeError("__str__ method must return a string");
		return (PyString)ret;
	}
	
	public int hashCode() {
	    PyObject ret;
		ret = invoke_ex("__hash__");

		if (ret == null) return super.hashCode();
		if (ret instanceof PyInteger) {
			return ((PyInteger)ret).getValue();
		}
		throw Py.TypeError("__hash__() must return int");
	}

	public int __cmp__(PyObject o) {
		PyObject ret = invoke_ex("__cmp__", o);
		if (ret == null) return -2;
		if (ret instanceof PyInteger) {
			int v = ((PyInteger)ret).getValue();
			return v < 0 ? -1 : v > 0 ? 1 : 0;
		}
		throw Py.TypeError("__cmp__() must return int");
	}

	public boolean __nonzero__() {
		PyObject ret = invoke_ex("__nonzero__");
		if (ret != null) {
		    return ret.__nonzero__();
		}
		
		try {
			return __len__() == 0 ? false : true;
		} catch (PyException exc) {
			if (Py.matchException(exc, Py.AttributeError)) return true;
			throw exc;
		}
	}

    private CollectionProxy collectionProxy=null;
    private CollectionProxy getCollection() {
        if (collectionProxy == null) collectionProxy = 
            CollectionProxy.findCollection(new Object[] {javaProxy});
        return collectionProxy;
    }

	public int __len__() {
	    CollectionProxy proxy = getCollection();
	    if (proxy != CollectionProxy.NoProxy) {
	        return proxy.__len__();
	    }
	    
		PyObject ret = invoke("__len__");
		if (ret instanceof PyInteger) return ((PyInteger)ret).getValue();
		throw Py.TypeError("__len__() should return an int");
	}

	public PyObject __finditem__(int key) {
	    CollectionProxy proxy = getCollection();
	    if (proxy != CollectionProxy.NoProxy) {
	        return proxy.__finditem__(key);
	    }
	    
	    return __finditem__(new PyInteger(key));
	}
	
	private PyObject trySlice(PyObject key, String name, PyObject extraArg) {
	    if (!(key instanceof PySlice)) return null;
	    
	    PySlice slice = (PySlice)key;
	    
	    if (slice.step != Py.None) return null;
	
	    PyObject func = __findattr__(name);
	    if (func == null) return null;
	    
	    PyObject start = slice.start;
	    PyObject stop = slice.stop;
	    
	    if (start == Py.None) start = Py.Zero;
	    if (stop == Py.None) stop = new PyInteger(PySystemState.maxint);
	    
        if (extraArg == null) {
            return func.__call__(start, stop);
        } else {
            return func.__call__(start, stop, extraArg);
	    }
    }
	
	public PyObject __finditem__(PyObject key) {
	    CollectionProxy proxy = getCollection();
	    if (proxy != CollectionProxy.NoProxy) {
	        return proxy.__finditem__(key);
	    }
	    
		try {
	        PyObject ret = trySlice(key, "__getslice__", null);
		    if (ret != null) return ret;
		    
			return invoke("__getitem__", key);
		} catch (PyException e) {
			if (Py.matchException(e, Py.IndexError)) return null;
			throw e;
		}
	}

	public void __setitem__(PyObject key, PyObject value) {
	    CollectionProxy proxy = getCollection();
	    if (proxy != CollectionProxy.NoProxy) {
	        proxy.__setitem__(key, value);
	        return;
	    }
	    
	    if (trySlice(key, "__setslice__", value) != null) return;
	    
		invoke("__setitem__", key, value);
	}

	public void __delitem__(PyObject key) {
	    CollectionProxy proxy = getCollection();
	    if (proxy != CollectionProxy.NoProxy) {
	        proxy.__delitem__(key);
	        return;
	    }
	    
	    if (trySlice(key, "__delslice__", null) != null) return;
		invoke("__delitem__", key);
	}

	//Begin the numeric methods here
	public Object __coerce_ex__(PyObject o) {
		PyObject ret = invoke_ex("__coerce__", o);
		if (ret == null || ret == Py.None) return ret;
		if (!(ret instanceof PyTuple))
			throw Py.TypeError("coercion should return None or 2-tuple");
		return ((PyTuple)ret).list;
	}


	// Generated by make_binops.py

	// Unary ops

	/**
	Implements the __hex__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyString __hex__() {
		PyObject ret = invoke("__hex__");
		if (ret instanceof PyString) return (PyString)ret;
		throw Py.TypeError("__hex__() should return a string");
	}
	
	/**
	Implements the __oct__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyString __oct__() {
		PyObject ret = invoke("__oct__");
		if (ret instanceof PyString) return (PyString)ret;
		throw Py.TypeError("__oct__() should return a string");
	}
	
	/**
	Implements the __int__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyInteger __int__() {
		PyObject ret = invoke("__int__");
		if (ret instanceof PyInteger) return (PyInteger)ret;
		throw Py.TypeError("__int__() should return a int");
	}
	
	/**
	Implements the __float__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyFloat __float__() {
		PyObject ret = invoke("__float__");
		if (ret instanceof PyFloat) return (PyFloat)ret;
		throw Py.TypeError("__float__() should return a float");
	}
	
	/**
	Implements the __long__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyLong __long__() {
		PyObject ret = invoke("__long__");
		if (ret instanceof PyLong) return (PyLong)ret;
		throw Py.TypeError("__long__() should return a long");
	}
	
	/**
	Implements the __complex__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyComplex __complex__() {
		PyObject ret = invoke("__complex__");
		if (ret instanceof PyComplex) return (PyComplex)ret;
		throw Py.TypeError("__complex__() should return a complex");
	}
	
	/**
	Implements the __pos__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __pos__() {
		return invoke("__pos__");
	}
	
	/**
	Implements the __neg__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __neg__() {
		return invoke("__neg__");
	}
	
	/**
	Implements the __abs__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __abs__() {
		return invoke("__abs__");
	}
	
	/**
	Implements the __invert__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __invert__() {
		return invoke("__invert__");
	}
	
	// Binary ops

	/**
	Implements the __add__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __add__(PyObject o) {
		return invoke_ex("__add__", o);
	}
	
	/**
	Implements the __radd__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __radd__(PyObject o) {
		return invoke_ex("__radd__", o);
	}
	
	/**
	Implements the __sub__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __sub__(PyObject o) {
		return invoke_ex("__sub__", o);
	}
	
	/**
	Implements the __rsub__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rsub__(PyObject o) {
		return invoke_ex("__rsub__", o);
	}
	
	/**
	Implements the __mul__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __mul__(PyObject o) {
		return invoke_ex("__mul__", o);
	}
	
	/**
	Implements the __rmul__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rmul__(PyObject o) {
		return invoke_ex("__rmul__", o);
	}
	
	/**
	Implements the __div__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __div__(PyObject o) {
		return invoke_ex("__div__", o);
	}
	
	/**
	Implements the __rdiv__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rdiv__(PyObject o) {
		return invoke_ex("__rdiv__", o);
	}
	
	/**
	Implements the __mod__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __mod__(PyObject o) {
		return invoke_ex("__mod__", o);
	}
	
	/**
	Implements the __rmod__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rmod__(PyObject o) {
		return invoke_ex("__rmod__", o);
	}
	
	/**
	Implements the __divmod__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __divmod__(PyObject o) {
		return invoke_ex("__divmod__", o);
	}
	
	/**
	Implements the __rdivmod__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rdivmod__(PyObject o) {
		return invoke_ex("__rdivmod__", o);
	}
	
	/**
	Implements the __pow__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __pow__(PyObject o) {
		return invoke_ex("__pow__", o);
	}
	
	/**
	Implements the __rpow__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rpow__(PyObject o) {
		return invoke_ex("__rpow__", o);
	}
	
	/**
	Implements the __lshift__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __lshift__(PyObject o) {
		return invoke_ex("__lshift__", o);
	}
	
	/**
	Implements the __rlshift__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rlshift__(PyObject o) {
		return invoke_ex("__rlshift__", o);
	}
	
	/**
	Implements the __rshift__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rshift__(PyObject o) {
		return invoke_ex("__rshift__", o);
	}
	
	/**
	Implements the __rrshift__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rrshift__(PyObject o) {
		return invoke_ex("__rrshift__", o);
	}
	
	/**
	Implements the __and__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __and__(PyObject o) {
		return invoke_ex("__and__", o);
	}
	
	/**
	Implements the __rand__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rand__(PyObject o) {
		return invoke_ex("__rand__", o);
	}
	
	/**
	Implements the __or__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __or__(PyObject o) {
		return invoke_ex("__or__", o);
	}
	
	/**
	Implements the __ror__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __ror__(PyObject o) {
		return invoke_ex("__ror__", o);
	}
	
	/**
	Implements the __xor__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __xor__(PyObject o) {
		return invoke_ex("__xor__", o);
	}
	
	/**
	Implements the __rxor__ method by looking it up
	in the instance's dictionary and calling it if it is found.
	**/
	public PyObject __rxor__(PyObject o) {
		return invoke_ex("__rxor__", o);
	}
}
