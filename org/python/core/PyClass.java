// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.util.Vector;
import java.io.Serializable;

/**
 * A python class.
 */

public class PyClass extends PyObject
{
    /**
     * Holds the namespace for this class
     **/
    public PyObject __dict__;

    /**
     * The base classes of this class
     **/
    public PyTuple __bases__;

    /**
     * The name of this class
     **/
    public String __name__;

    // Store these methods for performance optimization
    // These are only used by PyInstance
    PyObject __getattr__, __setattr__, __delattr__, __tojava__,
             __del__, __contains__;

    // Holds the classes for which this is a proxy
    // Only used when subclassing from a Java class
    protected Class proxyClass;

    public static PyClass __class__;

    PyClass(boolean fakeArg) {
        super(fakeArg);
        proxyClass = null;
    }

    protected PyClass(PyClass c) {
        super(c);
        proxyClass = null;
    }


    /**
     * Create a python class.
     *
     * @param name   name of the class.
     * @param bases  A list of base classes.
     * @param dict   The class dict. Normally this dict is returned by
     *               the class code object.
     *
     * @see org.python.core.Py#makeClass(String, PyObject[], PyCode, PyObject)
     */
    public PyClass(String name, PyTuple bases, PyObject dict) {
        this(name, bases, dict, null);
    }

    /**
     * Create a python class which inherit from a java class and where
     * we already have generated a proxyclass. If we do not have a
     * pre-generated proxyclass, the class initialization method will
     * create such a proxyclass if bases contain a java class.
     *
     * @param name   name of the class.
     * @param bases  A list of base classes.
     * @param dict   The class dict. Normally this dict is returned by
     *               the class code object.
     *
     * @see org.python.core.Py#makeClass(String, PyObject[], PyCode,
     *                                   PyObject, Class)
     */
    public PyClass(String name, PyTuple bases, PyObject dict,
                   Class proxyClass)
    {
        super(__class__);
        this.proxyClass = proxyClass;
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

        findModule(dict);

        if (proxyClass == null) {
            Vector interfaces = new Vector();
            Class baseClass = null;
            for (int i=0; i<bases.list.length; i++) {
                Class proxy = ((PyClass)bases.list[i]).getProxyClass();
                if (proxy != null) {
                    if (proxy.isInterface()) {
                        interfaces.addElement(proxy);
                    } else {
                        if (baseClass != null) {
                            throw Py.TypeError("no multiple inheritance "+
                                               "for Java classes: "+
                                               proxy.getName()+
                                               " and "+
                                               baseClass.getName());
                        }
                        baseClass = proxy;
                    }
                }
            }
            if (baseClass != null || interfaces.size() != 0) {
                String proxyName = __name__;
                PyObject module = dict.__finditem__("__module__");
                if (module != null)
                    proxyName = module.toString() + "$" + __name__;
                proxyClass = MakeProxies.makeProxy(baseClass, interfaces,
                                                   __name__, proxyName,
                                                   __dict__);
            }
        }

        if (proxyClass != null) {
            PyObject superDict =
                PyJavaClass.lookup(proxyClass).__findattr__("__dict__");
            // This code will add in the needed super__ methods to the class
            PyObject snames = superDict.__finditem__("__supernames__");
            if (snames != null) {
                PyObject sname;
                for (int i = 0; (sname = snames.__finditem__(i)) != null; i++) {
                    if (__dict__.__finditem__(sname) == null) {
                        PyObject superFunc = superDict.__finditem__(sname);
                        if (superFunc != null)
                            __dict__.__setitem__(sname, superFunc);
                    }
                }
            }
        }

        //System.out.println("proxyClasses: "+proxyClasses+", "+
        //                   proxyClasses[0]);
        if (dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", Py.None);
        }

        // Setup cached references to methods where performance really counts
        __getattr__ = lookup("__getattr__", false);
        __setattr__ = lookup("__setattr__", false);
        __delattr__ = lookup("__delattr__", false);
        __tojava__ = lookup("__tojava__", false);
        __del__ = lookup("__del__", false);
        __contains__ = lookup("__contains__", false);
    }

    protected void findModule(PyObject dict) {
        PyObject module = dict.__finditem__("__module__");
        if (module == null || module == Py.None) {
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
        if ((c == Object.class || c == Class.class || c == Serializable.class)
                                      && proxyClass != null) {
            return proxyClass;
        }
        return super.__tojava__(c);
    }

    // returns [PyObject, PyClass]
    PyObject[] lookupGivingClass(String name, boolean stop_at_java) {
        PyObject result = __dict__.__finditem__(name);
        PyClass resolvedClass = this;
        if (result == null && __bases__ != null) {
            int n = __bases__.__len__();
            for (int i=0; i<n; i++) {
                resolvedClass = (PyClass)(__bases__.__getitem__(i));
                PyObject[] res = resolvedClass.lookupGivingClass(name,
                                                               stop_at_java);
                result = res[0];
                resolvedClass = (PyClass)(res[1]);
                if (result != null)
                    break;
            }
        }
        return new PyObject[] {result, resolvedClass};
    }

    PyObject lookup(String name, boolean stop_at_java) {
        PyObject[] result = lookupGivingClass(name, stop_at_java);
        return result[0];
    }

    public PyObject __findattr__(String name) {
        if (name == "__dict__") return __dict__;
        if (name == "__name__") return new PyString(__name__);
        if (name == "__bases__") return __bases__;

        PyObject[] result = lookupGivingClass(name, false);

        if (result[0] == null)
            return super.__findattr__(name);

        return result[0]._doget(null, result[1]);
    }

    public void __setattr__(String name, PyObject value) {
        if (name == "__dict__") {
            if (!value.isMappingType())
                throw Py.TypeError("__dict__ must be a dictionary object");
            __dict__ = value;
            return;
        }
        if (name == "__name__") {
            if (!(value instanceof PyString))
                throw Py.TypeError("__name__ must be a string object");
            __name__ = value.toString();
            return;
        }
        if (name == "__bases__") {
            if (!(value instanceof PyTuple))
                throw Py.TypeError("__bases__ must be a tuple object");
            __bases__ = (PyTuple) value;
            return;
        }

        __dict__.__setitem__(name, value);
    }

    public void __delattr__(String name) {
        __dict__.__delitem__(name);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        PyInstance inst;
        if (__del__ == null)
            inst = new PyInstance(this);
        else
            // the class defined an __del__ method
            inst = new PyFinalizableInstance(this);

        inst.__init__(args, keywords);

        if (proxyClass != null &&
                   PyObject.class.isAssignableFrom(proxyClass)) {
            // It would be better if we didn't have to create a PyInstance
            // in the first place.
            ((PyObject)inst.javaProxy).__class__ = this;
            return (PyObject)inst.javaProxy;
        }

        return inst;
    }

    /* PyClass's are compared based on __name__ */
    public int __cmp__(PyObject other) {
        if (!(other instanceof PyClass))
            return -2;
        int c = __name__.compareTo(((PyClass)other).__name__);
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    public PyString __str__() {
        // Current CPython standard is that str(class) prints as
        // module.class.  If the class has no module, then just the class
        // name is printed.
        if (__dict__ == null)
            return new PyString(__name__);
        PyObject mod = __dict__.__finditem__("__module__");
        if (mod == null || !(mod instanceof PyString))
            return new PyString(__name__);
        String smod = ((PyString)mod).toString();
        return new PyString(smod + "." + __name__);
    }

    public String toString()  {
        PyObject mod = __dict__.__finditem__("__module__");
        String smod;
        if (mod == null || !(mod instanceof PyString))
            smod = "<unknown>";
        else
            smod = ((PyString)mod).toString();
        return "<class "+smod+"."+__name__+" at "+Py.id(this)+">";
    }
}
