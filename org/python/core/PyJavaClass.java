package org.python.core;
import java.lang.reflect.*;
import java.beans.*;

public class PyJavaClass extends PyClass {
	public PyReflectedConstructor __init__;

	//Deal with a few "magic" java methods
	/*
    private static java.util.Hashtable magicMethods;
    private static PyJavaClass ObjectType;
    public static boolean getMagic(PyObject dict, PyString name) {
        if (magicMethods == null) {
            if (ObjectType == null) {
                return false;
                //System.err.println("no PyObject yet");
                //return false;
            }
           magicMethods = new java.util.Hashtable();
           PyString newName = PyString.__new("__getitem__");
           magicMethods.put(PyString.__new("__safe_getitem__"),
                            new NameObjectPair(newName,
                                                ObjectType.__dict__.__safe_getitem__(newName)));
          newName = PyString.__new("__getattr__");
           magicMethods.put(PyString.__new("__safe_getattr__"),
                            new NameObjectPair(newName,
                                                ObjectType.__dict__.__safe_getitem__(newName)));
          newName = PyString.__new("__coerce__");
           magicMethods.put(PyString.__new("__internal_coerce__"),
                            new NameObjectPair(newName,
                                                ObjectType.__dict__.__safe_getitem__(newName)));
          newName = PyString.__new("__repr__");
           magicMethods.put(PyString.__new("toString"),
                            new NameObjectPair(newName,
                                                ObjectType.__dict__.__safe_getitem__(newName)));
          newName = PyString.__new("__hash__");
           magicMethods.put(PyString.__new("hashCode"),
                            new NameObjectPair(newName,
                                                ObjectType.__dict__.__safe_getitem__(newName)));

        }
        Object no = magicMethods.get(name);
        if (no == null) return false;
        dict.__setitem__(((NameObjectPair)no).name, ((NameObjectPair)no).obj);
        return true;

    }*/
    

	private static java.util.Hashtable classes;
	private static boolean initDone;
	//private static java.util.Vector classList;
	public synchronized static final PyJavaClass lookup(Class c) {
	    //System.err.println("jclass: "+c.getName());
	    if (classes == null) {
	        //System.out.println("lookup in null");
	        initDone = false;
            classes = new java.util.Hashtable();
            //classList = new java.util.Vector();
            Py.initPython();
            
            //System.out.println("lookuping up PyClass");
            
            lookup(PyClass.class).init(PyClass.class);
            
            //System.out.println("looking up PyJavaClass");      
            
            PyJavaClass.__class__ = new PyJavaClass(PyJavaClass.class);
            
            initDone = true;
            
            // Finalize all intermediate classes
    		int n = classes.size();
            
            java.util.Enumeration ek = classes.keys();
            Class[] keys = new Class[n];
            
            for (int i=0; i<n; i++) {
                keys[i] = (Class)ek.nextElement();
            }
            
            //ek = classes.keys();
    		//java.util.Enumeration ev = classes.elements();

    		for(int i=0; i<n; i++) {
    			Class key = keys[i]; //(Class)ek.nextElement();
    			PyJavaClass value = (PyJavaClass)classes.get(key); //ev.nextElement();
    			//System.out.println("key: "+key.getName()+" : "+value.proxyClasses);
            
                if (value.proxyClasses == null) {
                    value.init(key);
                }
            }
            
            initDone = true;
        }
        //System.out.println("JavaClass 1");

		PyJavaClass ret = (PyJavaClass)classes.get(c);
		if (ret != null) return ret;
		
	    if (initDone) {
	        ret = new PyJavaClass();
	        classes.put(c, ret);
	        ret.init(c);
	        return ret;
	    }
	    
        if (c == PyJavaClass.class) {
            ret = new PyJavaClass(true);
        } else {
            //classList.addElement(c);
            ret = new PyJavaClass();
        }
	    if (ret != null) classes.put(c, ret);
	    return ret;
	}

    private static PyJavaClass superLookup(Class c) {
        PyJavaClass jc = lookup(c);
        if (jc.proxyClasses == null) {
            //System.err.println("superLookup: "+c.getName());
            jc.init(c);
        }
        return jc;
    }

    public static PyClass __class__;
    public PyJavaClass() {
        super(__class__);
    }

    private PyJavaClass(boolean fakeArg) {
        super(true);
    }

	public PyJavaClass(Class c) {
	    this();
	    init(c);
	}
	
	protected void findModule(PyObject dict) {}	

	private void init(Class c)  {
	    //System.err.println("initing jclass: "+c.getName());
	    
	    PyStringMap d = new PyStringMap();
	    d.__setitem__("__module__", Py.None);
		proxyClasses = new Class[] {c};
	    
		super.init(c.getName(), Py.EmptyTuple, d);
		
		Class interfaces[] = c.getInterfaces();
		int nInterfaces = interfaces.length;		
        int nBases = 0;
        
		InitModule initModule = null;
		int i;
		for (i=0; i<nInterfaces; i++) {
			if (interfaces[i] == InitModule.class) {
			    try {
				    initModule = (InitModule)c.newInstance();
				} catch (Exception exc) {
				    throw Py.JavaError(exc);
				}
				continue;
			}
			if (interfaces[i] == PyProxy.class) {
			    continue;
			}
			nBases++;
		}
		
		Class sc = c.getSuperclass();
		int index=0;
		PyObject[] bases;
		if (sc == null || sc == PyObject.class) {
		    bases = new PyObject[nBases];
		} else {
		    bases = new PyObject[nBases+1];
		    bases[0] = superLookup(sc);
		    index++;
		}

		for (i=0; i<nInterfaces; i++) {
		    Class inter = interfaces[i];
			if (inter == InitModule.class || inter == PyProxy.class) continue;
			bases[index++] = superLookup(inter);
		}

		__bases__ = new PyTuple(bases);

		setBeanInfo(c, sc);
		setFields(c);
		setMethods(c);
	    //System.err.println("setting constructors: "+c.getName());
		
		setConstructors(c);
		
	    //System.err.println("set constructors: "+c.getName());
		
		
		if (initModule != null) {
		    initModule.initModule(d);
		}
	}

	private void setFields(Class c) {
		Field[] fields = c.getFields();
		for(int i=0; i<fields.length; i++) {
			Field field = fields[i];
			if (field.getDeclaringClass() != c) continue;

            String name = getName(field.getName());
            boolean isstatic = Modifier.isStatic(field.getModifiers());
            
            // Handle the special static __class__ fields on PyObject instances
            if (name == "__class__" &&  isstatic && 
                    PyObject.class.isAssignableFrom(field.getDeclaringClass()) &&
                    field.getType().isAssignableFrom(PyJavaClass.class)) {
                try {
                    field.set(null, this);
                    continue;
                } catch (Throwable t) {
                    System.err.println("invalid __class__ field on: "+c.getName());
                }
            }
            
            if (isstatic) {
                PyObject prop = lookup(name, false);
                if (prop != null && prop instanceof PyBeanProperty) {
                    ((PyBeanProperty)prop).field = field;
                    continue;
                }
            }
			__dict__.__setitem__(name, new PyReflectedField(field));
		}
	}
	
	/* Produce a good Python name for a Java method
	If the Java method ends in '$', strip it (this handles resvered Java keywords)
	If the new name conflicts with a Python keyword, add an '_'
	Finally, ensure that the returned string is intern'd
	to make life easier for using code.
	*/
	private static java.util.Hashtable keywords=null;
	private String getName(String name) {
		if (name.endsWith("$")) name = name.substring(0, name.length()-1);
		
		if (keywords == null) {
			keywords = new java.util.Hashtable();
			String[] words = new String[]
				{"or", "and", "not", "is", "in", "lambda", "if", "else", "elif",
				 "while", "for", "try", "except", "def", "class", "finally", "print",
				 "pass", "break", "continue", "return", "import", "from", "del",
				 "raise", "global", "exec", "assert"};
			for (int i=0; i<words.length; i++) {
				keywords.put(words[i], (words[i]+"_").intern());
			}
		}
		String newName = (String)keywords.get(name);
		if (newName != null) {
		    return newName;
		} else {
		    return name.intern();
		}
	}
	
	private void addMethod(Method meth) {
	    String name = getName(meth.getName());
	    
	    // See if any of my superclasses are using 'name' for something else
	    // Or if I'm already using it myself
	    PyObject o = lookup(name, false);
	    
	    // If it's being used as a bean property, and it's a "good" bean property
	    // Then add a "_" after name (we can fix anything if we add enough _'s
	    // Only do this tricky handling for java.awt classes - 
	    // which have seriously messed up api's
	    String classname = proxyClasses[0].getName();
	    
	    if (classname.startsWith("java.awt.") || 
	        classname.startsWith("org.python.proxies.java.awt.")) {
    	    if (o != null && o instanceof PyBeanProperty) {
                PyBeanProperty prop = (PyBeanProperty)o;
                if (prop.getMethod != null && prop.setMethod != null) {
                    name = (name+"_").intern();
                    o = super.lookup(name, false);
    	        }
    	    }
    	}
	    
	    // If it's being used as a function, then things get more interesting...
	    PyReflectedFunction func;
	    if (o != null && o instanceof PyReflectedFunction) {
	        func = (PyReflectedFunction)o;
	        
	        PyObject o1 = __dict__.__finditem__(name);
	        
	        /* If this function already exists, add this method to the
	        signature.  If this alters the signature of the function in
	        some significant way, then return a duplicate and stick it in
	        the __dict__ */
	        if (o1 != o) {
	            if (func.handles(meth)) return;
	            func = func.copy();
	        }

	        func.addMethod(meth);
	    } else {
	        func = new PyReflectedFunction(meth);
	    }
	    
	    __dict__.__setitem__(name, func);
	}

    /* Add all methods declared by this class */
	private void setMethods(Class c) {
		Method[] methods = c.getMethods();
		for(int i=0; i<methods.length; i++) {
			Method method = methods[i];
			Class dc = method.getDeclaringClass();
			if (dc != c) continue;
			
			addMethod(method);
		}
	}
	
	/* Adds a bean property to this class */
    void addProperty(String name, Class propClass, Method getMethod, Method setMethod) {
        // This will skip indexed property types
        if (propClass == null) return;
        
        boolean set = true;
        name = getName(name);

        PyBeanProperty prop = new PyBeanProperty(name, propClass, getMethod, setMethod);

        // Check to see if this name is already being used...
        PyObject o = lookup(name, false);
        
        if (o != null && o instanceof PyReflectedField) {
            if (o instanceof PyBeanProperty) {
                PyBeanProperty oldProp = (PyBeanProperty)o;
                if (prop.myType == oldProp.myType) {
                    // If this adds nothing over old property, do nothing
                    if ((prop.getMethod == null || oldProp.getMethod != null) &&
                        (prop.setMethod == null || oldProp.setMethod != null)) {
                            set = false;
                    }
                    
                    // Add old get/set methods to current prop
                    // Handles issues with private classes
                    if (oldProp.getMethod != null) {
                        prop.getMethod = oldProp.getMethod;
                    }
                    if (oldProp.setMethod != null) {
                        prop.setMethod = oldProp.setMethod;
                    }
                }
            } /* This is now handled in setFields which gets called after setBeanProperties
            else {
                // Keep static fields around...
                PyReflectedField field = (PyReflectedField)o;
                if (Modifier.isStatic(field.field.getModifiers())) {
                    prop.field = field.field;
                } else {
                    // If the field is not static (and thus subsumable) don't overwrite
                    return;
                }
            }*/
        }
        if (set) __dict__.__setitem__(name, prop);
    }

    /* Adds a bean event to this class */
    void addEvent(String name, Class eventClass, Method addMethod, Method[] meths) {
        String eventName = eventClass.getName();

        for (int i=0; i<meths.length; i++) {
            PyBeanEventProperty prop;
            prop = new PyBeanEventProperty(name, eventClass, addMethod, meths[i]);
            __dict__.__setitem__(prop.__name__, prop);
        }
        PyBeanEvent event = new PyBeanEvent(name, eventClass, addMethod);
        __dict__.__setitem__(event.__name__, event);
    }	
	

    /* A reimplementation of java.beans.Introspector.decapitalize.
        This is needed due to bugs in Netscape Navigator
    */
    private static String decapitalize(String s) {
        //return java.beans.Introspector.decapitalize(s);
        if (s.length() == 0) return s;
        char c0 = s.charAt(0);
        if (Character.isUpperCase(c0)) {
            if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) return s;
            char[] cs = s.toCharArray();
            cs[0] = Character.toLowerCase(c0);
            return new String(cs);
        } else {
            return s;
        }
    }

	//This method is a workaround for Netscape's stupid security bug!
	private void setBeanInfoCustom(Class c) {
	    try {
	        Method[] meths = c.getMethods();

    	    int i;
    	    int n = meths.length;
    	    for(i=0; i<n; i++) {
    	        Method method = meths[i];

    	        if (method.getDeclaringClass() != c ||
    	            Modifier.isStatic(method.getModifiers())) continue;

    	        String name = method.getName();
    	        Method getter = null;
    	        Method setter = null;
    	        Class[] args = method.getParameterTypes();
    	        Class ret = method.getReturnType();
    	        Class myType=null;

    	        String pname="";

    	        if (name.startsWith("get")) {
    	            if (args.length != 0) continue;
    	            getter = method;
    	            pname = decapitalize(name.substring(3));
    	            myType = ret;
    	            //System.out.println("get: "+name+", "+myType);
    	        } else {
        	        if (name.startsWith("is")) {
        	            if (args.length != 0 || ret != Boolean.TYPE) continue;
        	            getter = method;
        	            pname = decapitalize(name.substring(2));
        	            myType = ret;
        	        } else {
            	        if (name.startsWith("set")) {
            	            if (args.length != 1 || ret != Void.TYPE) continue;
            	            setter = method;
            	            pname = decapitalize(name.substring(3));
            	            myType = args[0];
    	                    //System.out.println("set: "+name+", "+myType);

            	        } else {
            	            continue;
            	        }
            	    }
            	}

    	        PyObject o = __dict__.__finditem__(new PyString(pname));
    	        PyBeanProperty prop;
    	        if (o == null || !(o instanceof PyBeanProperty) ) {
    	            addProperty(pname, myType, getter, setter);
    	        } else {
    	            prop = (PyBeanProperty)o;
    	            if (prop.myType != myType) {
    	                if (getter != null) {
    	                    addProperty(pname, myType, getter, setter);
    	                }
    	            } else {
    	            //System.out.println("p: "+prop.myType+", "+myType);
        	            if (getter != null) prop.getMethod = getter;
        	            if (setter != null) prop.setMethod = setter;
        	        }
    	        }
    	    }

    	    for(i=0; i<n; i++) {
    	        Method method = meths[i];

    	        if (method.getDeclaringClass() != c ||
    	            Modifier.isStatic(method.getModifiers())) continue;

    	        String mname = method.getName();

    	        if (!(mname.startsWith("add") || mname.startsWith("set")) ||
    	                !mname.endsWith("Listener")) continue;

    	        Class[] args = method.getParameterTypes();
    	        Class ret = method.getReturnType();
    	        String pname="";

    	        if (args.length != 1 || ret != Void.TYPE) continue;

    	        Class eClass = args[0];
                if (!(java.util.EventListener.class.isAssignableFrom(eClass))) continue;

    	        String name = eClass.getName();
    	        int idot = name.lastIndexOf('.');
    	        if (idot != -1)
    	            name = decapitalize(name.substring(idot+1));

                addEvent(name, eClass, method, eClass.getMethods());
    	    }
    	} catch (Throwable t) {
    	    System.err.println("Custom Bean error: "+t);
    	    t.printStackTrace();
    	}
	}


    private static boolean workingBeans=true;
	protected void setBeanInfo(Class c, Class sc) {
	    if (!c.getName().startsWith("org.python.core.")) {
	        if (workingBeans) {
	            try {
	                BeanInfoFinder.setBeanInfo(this, c, sc);
	            } catch (OutOfMemoryError e) {
	                throw Py.MemoryError("out of memory");
	            } catch (Throwable t) {
	                System.err.println("Your JVM has a broken java.beans package in: "+c.getName());
	                //t.printStackTrace();
	                System.err.println("Using JPython's partial reimplementation as work-around");
	                workingBeans = false;
	                setBeanInfoCustom(c);
	            }
	        } else {
	            setBeanInfoCustom(c);
	        }
	    }
	}

	private void setConstructors(Class c) {
	    //System.out.println("c: "+c.getName()+" "+Modifier.isAbstract(c.getModifiers()));
	    if (Modifier.isInterface(c.getModifiers())) {
	        __init__ = null;
	    } else {
    		java.lang.reflect.Constructor[] constructors = c.getConstructors();
    		if (constructors.length > 0) {
    			__init__ = new PyReflectedConstructor(constructors[0]);
    			for(int i=1; i<constructors.length; i++) {
    				__init__.addConstructor(constructors[i]);
    			}
    		    __dict__.__setitem__("__init__", __init__);
    		}
    	}
	}

	PyObject lookup(String name, boolean stop_at_java) {
		if (stop_at_java) return null;
		return super.lookup(name, false);
	}

    private PyJavaInstance classInstance;
    static boolean withinner = false;
	public PyObject __findattr__(String name) {
        PyObject ret = super.__findattr__(name);
        if (ret != null) {
            return ret._doget(null);
        }
	    if (classInstance == null) {
	        classInstance = new PyJavaInstance(proxyClasses[0]);
	    }
	    ret = classInstance.__findattr__(name);
	    
	    // Rudimentary inner class support
	    if (ret != null || !withinner) return ret;
	    
	    try {
	        //System.err.println("looking up inner: "+__name__+"$"+name);
	        Class innerClass = Class.forName(__name__+"$"+name);
	        PyJavaClass jinner = PyJavaClass.lookup(innerClass);
	        PyObject[] bases = jinner.__bases__.list;
	        int n = bases.length;
	        PyObject[] newBases = new PyObject[n+1];
	        
	        int i;
	        for(i=0; i<n; i++) {
	            if (bases[i] == this) break;
	            newBases[i] = bases[i];
	        }
	        if (i == n) newBases[i] = this;
	        
	        jinner.__bases__ = new PyTuple(newBases);
	        String innername = jinner.__name__;
	        int dollar = innername.indexOf('$');
	        if (dollar != -1) {
	            jinner.__name__ = innername.substring(0, dollar)+
	                "." + innername.substring(dollar+1, innername.length());
	        }
	        
	        /*if (jinner.__bases__
	        PyJavaClass jinner = new PyJavaClass();
	        jinner.__dict__ = new PyStringMap();
	        jinner.__name__ = __name__+"."+name;
	        jinner.__bases__ = new PyTuple(new PyObject[] {jc, this});
	        jinner.proxyClasses = new Class[] {innerClass};*/
	        
	        __dict__.__setitem__(name, jinner);
	        return jinner;
        } catch (ClassNotFoundException exc) {
            return null;
        } catch (Throwable t) {
            System.err.println("internal error looking for inner class");
            t.printStackTrace();
            return null;
        }
    }

	public void __setattr__(String name, PyObject value) {
		PyObject field = super.lookup(name, false);
		if (field != null) {
		    if (field._doset(null, value)) return;
		}
		
		__dict__.__setitem__(name, value);
	}

	public void __delattr__(String name) {
	    PyObject field = super.lookup(name, false);
	    if (field == null) {
	        throw Py.NameError("attribute not found: "+name);
	    }
	    
	    if (!field._dodel(null)) {
	        __dict__.__delitem__(name);
	        //throw Py.TypeError("attr not deletable: "+name);
	    }
	}

	public PyObject __call__(PyObject[] args, String[] keywords) {
		PyInstance inst = new PyJavaInstance(this);
		/*int n = keywords.length;
		PyObject[] newArgs = args;
        if (n > 0) {
            newArgs = new PyObject[args.length-n];
            System.arraycopy(args, 0, newArgs, 0, newArgs.length);
        }*/
		inst.__init__(args, keywords);

		/*int offset = newArgs.length;
		for(int i=0; i<n; i++) {
		    inst.__setattr__(keywords[i], args[i+offset]);
		}*/
		return inst;
	}


	public String toString()  {
		return "<jclass "+__name__+" at "+Py.id(this)+">";
	}
}

/* This method is pulled out into a seperate class to work around Netscape bugs */
class BeanInfoFinder {
	public static void setBeanInfo(PyJavaClass jclass, Class c, Class sc) throws Exception {
        int i, n;
        // Set no bean search path, this probably needs work in the future
	    Introspector.setBeanInfoSearchPath(new String[0] );
    	BeanInfo info = Introspector.getBeanInfo(c, sc);

        PropertyDescriptor[] descrs = info.getPropertyDescriptors();
        for(i=0, n=descrs.length; i<n; i++) {
            PropertyDescriptor d = descrs[i];
            jclass.addProperty(d.getName(), d.getPropertyType(), d.getReadMethod(), d.getWriteMethod());
        }

	    EventSetDescriptor[] events = info.getEventSetDescriptors();
	    for(i=0, n=events.length; i<n; i++) {
	        EventSetDescriptor e = events[i];

	        jclass.addEvent(e.getName()+"Listener", e.getListenerType(),
	                e.getAddListenerMethod(), e.getListenerMethods());
	    }
	}
}