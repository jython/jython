// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.*;
import java.beans.*;

public class PyJavaClass extends PyClass
{
    public PyReflectedConstructor __init__;

    private static java.util.Hashtable classes;

    public static final PyJavaClass lookup(Class c) {
        return lookup(c.getName(), c);
    }
    
    public synchronized static final PyJavaClass lookup(String name, Class c) {
        if (classes == null) {
            classes = new java.util.Hashtable();
            PyJavaClass jc = new PyJavaClass(true);
            classes.put("org.python.core.PyJavaClass", jc);
            jc.init(PyJavaClass.class);
            Py.initPython();
        }
        PyJavaClass ret = (PyJavaClass)classes.get(name);
        if (ret != null)
            return ret;
        ret = new PyJavaClass(name);
        classes.put(name, ret);
        if (c != null) {
            ret.init(c);
        }
        return ret;
    }

    public static PyClass __class__;
    
    public PyJavaClass(PyClass c) {
        super(c);
    }
    
    public PyJavaClass() {
        this(__class__);
    }

    private PyJavaClass(boolean fakeArg) {
        super(true);
    }

    public PyJavaClass(Class c) {
        this();
        init(c);
    }
        
    public PyJavaClass(String name) {
        this();
        __name__ = name;
    }
        
    protected void findModule(PyObject dict) {} 
    
    private void init__dict__() {
        if (__dict__ != null)
            return;
        PyStringMap d = new PyStringMap();
//         d.__setitem__("__module__", Py.None);
        __dict__ = d;
        try {
            setBeanInfoCustom(proxyClass);
            setFields(proxyClass);
            setMethods(proxyClass);
        } catch (SecurityException se) {
            ;
        }
    }

    protected Class getProxyClass() {
        initialize();
        return proxyClass;
    }

    private boolean initialized=false;

    private void initialize() {
        if (initialized)
            return;
        if (proxyClass == null)
            init(Py.findClass(__name__));
        init__bases__(proxyClass);
        init__dict__();
        initialized = true;
    }

    private void init__class__(Class c) {
        if (!PyObject.class.isAssignableFrom(c))
            return;

//         // Handle the special static __class__ fields on PyObject instances
//         if (name == "__class__" &&  isstatic && 
//             PyObject.class.isAssignableFrom(field.getDeclaringClass()) &&
//             field.getType().isAssignableFrom(PyJavaClass.class)) {
//             try {
//                 field.set(null, this);
//                 continue;
//             } catch (Throwable t) {
//                 System.err.println("invalid __class__ field on: "+c.getName());
//             }
//         }

        try {
            Field field = c.getField("__class__");
            if (Modifier.isStatic(field.getModifiers()) && 
                field.getType().isAssignableFrom(PyJavaClass.class))
            {
                field.set(null, this);
            }
        }
        catch (NoSuchFieldException exc) {}
        catch (IllegalAccessException exc1) {}
    }
    
    private void init__bases__(Class c) {
        if (__bases__ != null) return;
        
        Class interfaces[] = c.getInterfaces();
        int nInterfaces = interfaces.length;            
        int nBases = 0;
        int i;
        for (i=0; i<nInterfaces; i++) {
            Class inter = interfaces[i];
            if (inter == InitModule.class || inter == PyProxy.class)
                continue;
            nBases++;
        }
            
        Class superclass = c.getSuperclass();
        int index=0;
        PyObject[] bases;
        PyJavaClass tmp;
        if (superclass == null || superclass == PyObject.class) {
            bases = new PyObject[nBases];
        } else {
            bases = new PyObject[nBases+1];
            tmp = PyJavaClass.lookup(superclass);
            bases[0] = tmp;
            tmp.initialize();
            index++;
        }

        for (i=0; i<nInterfaces; i++) {
            Class inter = interfaces[i];
            if (inter == InitModule.class || inter == PyProxy.class)
                continue;
            tmp = PyJavaClass.lookup(inter);
            tmp.initialize();
            bases[index++] = tmp;
        }

        __bases__ = new PyTuple(bases);
    }        

    void init(Class c)  {
        init__class__(c);
        proxyClass = c;
        __name__ = c.getName();
            
        if (InitModule.class.isAssignableFrom(c)) {
            initialize();
            try {
                InitModule m = (InitModule)c.newInstance();
                m.initModule(__dict__);
            }
            catch (Exception exc) {
//                 System.err.println("Got exception: " + exc);
                throw Py.JavaError(exc);
            }
        }
    }

     /**
      * Return the list of all accessible fields for a class.  This will
      * only the public fields unless Options.respectJavaAccessibility is
      * false, in which case all fields are returned.
      */
    private static Field[] getAccessibleFields(Class c) {
 	if (!JavaAccessibility.accessIsMutable())
            // returns just the public fields
 	    return c.getFields();
        // from here on out we know we must be using at least Java 1.2.
        // Note that an ArrayList would be better here because we don't
        // need access to be synchronized, but that would prevent this file 
        // from being compiled on Java 1.1 (as opposed to being compilable, 
        // but not having the feature available).
        java.util.Vector fields = new java.util.Vector();
        while (c != null) {
            // get all declared fields for this class, mutate their
            // accessibility and pop it into the array for later
            Field[] declared = c.getDeclaredFields();
            for (int i=0; i < declared.length; i++) {
                // TBD: this is a permanent change.  Should we provide a
                // way to restore the original accessibility flag?
                JavaAccessibility.setAccessible(declared[i], true);
                fields.addElement(declared[i]);
            }
            // walk down superclass chain.  no need to deal specially with
            // interfaces...
            c = c.getSuperclass();
        }
//        return (Field[])fields.toArray(new Field[fields.size()]);
        Field[] ret = new Field[fields.size()];
        fields.copyInto(ret);
        return ret;
    }

    private void setFields(Class c) {
        Field[] fields = getAccessibleFields(c);
        for (int i=0; i<fields.length; i++) {
            Field field = fields[i];
            if (field.getDeclaringClass() != c)
                continue;

            String name = getName(field.getName());
            boolean isstatic = Modifier.isStatic(field.getModifiers());
            
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
        
    /* Produce a good Python name for a Java method.  If the Java method
       ends in '$', strip it (this handles reserved Java keywords) Don't
       make any changes to keywords since this is now handled by parser
    */

    private String getName(String name) {
        if (name.endsWith("$")) name = name.substring(0, name.length()-1);
        return name.intern();
    }
    
    private void addMethod(Method meth) {
        String name = getName(meth.getName());
        if (name == "_getPyInstance" || name == "_setPyInstance" ||
            name == "_getPySystemState" || name == "_setPySystemState")
        {
            return;
        }
           
        // Special case to handle a few troublesome methods in java.awt.*.
        // These methods are all deprecated and interfere too badly with
        // bean properties to be tolerated.  This is totally a hack, but a
        // lot of code that uses java.awt will break without it.
        String classname = proxyClass.getName();
        if (classname.startsWith("java.awt.") &&
            classname.indexOf('.', 9) == -1)
        { 
            if (name == "layout" || name == "insets" || 
                name == "size" || name == "minimumSize" ||
                name == "preferredSize" || name == "maximumSize" ||
                name == "bounds" || name == "enable")
            {
                return;
            }
        }
        
        // See if any of my superclasses are using 'name' for something
        // else.  Or if I'm already using it myself
        PyObject o = lookup(name, false);
            
        // If it's being used as a function, then things get more
        // interesting...
        PyReflectedFunction func;
        if (o != null && o instanceof PyReflectedFunction) {
            func = (PyReflectedFunction)o;
                
            PyObject o1 = __dict__.__finditem__(name);
                
            /* If this function already exists, add this method to the
               signature.  If this alters the signature of the function in
               some significant way, then return a duplicate and stick it in
               the __dict__ */
            if (o1 != o) {
                if (func.handles(meth))
                    return;
                func = func.copy();
            }
            func.addMethod(meth);
        } else {
            func = new PyReflectedFunction(meth);
        }
        __dict__.__setitem__(name, func);
    }


     /**
      * Return the list of all accessible methods for a class.  This will
      * only the public methods unless Options.respectJavaAccessibility is
      * false, in which case all methods are returned.
      */
    private static Method[] getAccessibleMethods(Class c) {
 	if (!JavaAccessibility.accessIsMutable())
            // returns just the public methods
 	    return c.getMethods();
        // from here on out we know we must be using at least Java 1.2.
        // Note that an ArrayList would be better here because we don't
        // need access to be synchronized, but that would prevent this file
        // from being compiled on Java 1.1 (as opposed to being compilable,
        // but not having the feature available).
        java.util.Vector methods = new java.util.Vector();
        while (c != null) {
            // get all declared methods for this class, mutate their
            // accessibility and pop it into the array for later
            Method[] declared = c.getDeclaredMethods();
            for (int i=0; i < declared.length; i++) {
                // TBD: this is a permanent change.  Should we provide a way to
                // restore the original accessibility flag?
                JavaAccessibility.setAccessible(declared[i], true);
                methods.addElement(declared[i]);
            }
            // walk down superclass chain
            c = c.getSuperclass();
        }
//        return (Method[])methods.toArray(new Method[methods.size()]);
        Method[] ret = new Method[methods.size()];
        methods.copyInto(ret);
        return ret;
    }

    /* Add all methods declared by this class */
    private void setMethods(Class c) {
        Method[] methods = getAccessibleMethods(c);
        for (int i=0; i<methods.length; i++) {
            Method method = methods[i];
            Class dc = method.getDeclaringClass();
            if (dc != c)
                continue;
            addMethod(method);
        }
    }
        
    /* Adds a bean property to this class */
    void addProperty(String name, Class propClass,
                     Method getMethod, Method setMethod)
    {
        // This will skip indexed property types
        if (propClass == null)
            return;
        
        boolean set = true;
        name = getName(name);

        PyBeanProperty prop = new PyBeanProperty(name, propClass, getMethod,
                                                 setMethod);

        // Check to see if this name is already being used...
        PyObject o = lookup(name, false);
        
        if (o != null) {
            if (!(o instanceof PyReflectedField))
                return;
            
            if (o instanceof PyBeanProperty) {
                PyBeanProperty oldProp = (PyBeanProperty)o;
                if (prop.myType == oldProp.myType) {
                    // If this adds nothing over old property, do nothing
                    if ((prop.getMethod == null || oldProp.getMethod != null)
                        &&
                        (prop.setMethod == null || oldProp.setMethod != null))
                    {
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
            }
            // This is now handled in setFields which gets called after
            // setBeanProperties
//             else {
//                 // Keep static fields around...
//                 PyReflectedField field = (PyReflectedField)o;
//                 if (Modifier.isStatic(field.field.getModifiers())) {
//                     prop.field = field.field;
//                 } else {
//                     // If the field is not static (and thus subsumable)
//                     // don't overwrite
//                     return;
//                 }
//             }
        }
        if (set)
            __dict__.__setitem__(name, prop);
    }

    /* Adds a bean event to this class */
    void addEvent(String name, Class eventClass, Method addMethod,
                  Method[] meths)
    {
        String eventName = eventClass.getName();

        for (int i=0; i<meths.length; i++) {
            PyBeanEventProperty prop;
            prop = new PyBeanEventProperty(name, eventClass, addMethod,
                                           meths[i]);
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
        if (s.length() == 0)
            return s;
        char c0 = s.charAt(0);
        if (Character.isUpperCase(c0)) {
            if (s.length() > 1 && Character.isUpperCase(s.charAt(1)))
                return s;
            char[] cs = s.toCharArray();
            cs[0] = Character.toLowerCase(c0);
            return new String(cs);
        } else {
            return s;
        }
    }

    // This method is a workaround for Netscape's stupid security bug!
    private void setBeanInfoCustom(Class c) {
        //try {
        Method[] meths = c.getMethods();

        int i;
        int n = meths.length;
        for (i=0; i<n; i++) {
            Method method = meths[i];

            if (method.getDeclaringClass() != c ||
                Modifier.isStatic(method.getModifiers()))
            {
                continue;
            }

            String name = method.getName();
            Method getter = null;
            Method setter = null;
            Class[] args = method.getParameterTypes();
            Class ret = method.getReturnType();
            Class myType=null;

            String pname="";

            if (name.startsWith("get")) {
                if (args.length != 0)
                    continue;
                getter = method;
                pname = decapitalize(name.substring(3));
                myType = ret;
                //System.out.println("get: "+name+", "+myType);
            } else {
                if (name.startsWith("is")) {
                    if (args.length != 0 || ret != Boolean.TYPE)
                        continue;
                    getter = method;
                    pname = decapitalize(name.substring(2));
                    myType = ret;
                } else {
                    if (name.startsWith("set")) {
                        if (args.length != 1 || ret != Void.TYPE)
                            continue;
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

        for (i=0; i<n; i++) {
            Method method = meths[i];

            if (method.getDeclaringClass() != c ||
                Modifier.isStatic(method.getModifiers()))
            {
                continue;
            }

            String mname = method.getName();

            if (!(mname.startsWith("add") || mname.startsWith("set")) ||
                !mname.endsWith("Listener"))
            {
                continue;
            }

            Class[] args = method.getParameterTypes();
            Class ret = method.getReturnType();
            String pname="";

            if (args.length != 1 || ret != Void.TYPE)
                continue;

            Class eClass = args[0];
            if (!(java.util.EventListener.class.isAssignableFrom(eClass)))
                continue;

            String name = eClass.getName();
            int idot = name.lastIndexOf('.');
            if (idot != -1)
                name = decapitalize(name.substring(idot+1));

            addEvent(name, eClass, method, eClass.getMethods());
        }
        /*} catch (Throwable t) {
          System.err.println("Custom Bean error: "+t);
          t.printStackTrace();
          }*/
    }

     /**
      * Return the list of all accessible constructors for a class.  This
      * will only the public constructors unless
      * Options.respectJavaAccessibility is false, in which case all
      * constructors are returned.  Note that constructors are not
      * inherited like methods or fields.
      */
    private static Constructor[] getAccessibleConstructors(Class c) {
 	if (!JavaAccessibility.accessIsMutable())
            // returns just the public fields
 	    return c.getConstructors();
        // return all constructors
        return c.getDeclaredConstructors();
    }

    private void setConstructors(Class c) {
        if (Modifier.isInterface(c.getModifiers())) {
            __init__ = null;
        } else {
            Constructor[] constructors = getAccessibleConstructors(c);
            if (constructors.length > 0) {
                __init__ = new PyReflectedConstructor(constructors[0]);
                for (int i=1; i < constructors.length; i++) {
                    __init__.addConstructor(constructors[i]);
                }
                __dict__.__setitem__("__init__", __init__);
            }
        }
    }
    private boolean constructorsInitialized=false;
    void initConstructors() {
        if (constructorsInitialized)
            return;
        constructorsInitialized = true;
        initialize();
        setConstructors(proxyClass);
    }
      
    /*
      If the new name conflicts with a Python keyword, add an '_'
    */
    private static java.util.Hashtable keywords=null;
    private static String unmangleKeyword(String name) {
        if (keywords == null) {
            keywords = new java.util.Hashtable();
            String[] words = new String[]
            {"or", "and", "not", "is", "in", "lambda", "if", "else", "elif",
             "while", "for", "try", "except", "def", "class", "finally",
             "print",
             "pass", "break", "continue", "return", "import", "from", "del",
             "raise", "global", "exec", "assert"};
            for (int i=0; i<words.length; i++) {
                keywords.put(words[i]+"_", words[i].intern());
            }
        }
        String newName = (String)keywords.get(name);
        if (newName != null) {
            return newName;
        } else {
            return null;
        }
    }
    
    PyObject[] lookupGivingClass(String name, boolean stop_at_java) {
        if (stop_at_java)
            return new PyObject[] {null, null};
        if (!initialized)
            initialize();
        if (name == "__init__") {
            initConstructors();
            return new PyObject[] {__init__, null};
        }
        
        // For backwards compatibilty, support keyword_ as a substitute for
        // keyword.  An improved parser makes this no longer necessary.
        if (Options.deprecatedKeywordMangling && name.endsWith("_")) {
            String newName = unmangleKeyword(name);
            if (newName != null)
                name = newName;
        }
        return super.lookupGivingClass(name, stop_at_java);
    }

    public PyObject __dir__() {
        initialize();
        if (__dict__ instanceof PyStringMap) {
            return ((PyStringMap)__dict__).keys();
        } else {
            return __dict__.invoke("keys");
        }
    }

    private PyStringMap missingAttributes = null;
    public PyObject __findattr__(String name) {
        if (name == "__dict__") {
            if (__dict__ == null)
                init__dict__();
            return __dict__;
        }
        if (name == "__name__")
            return new PyString(__name__);
        if (name == "__bases__") {
            if (__bases__ == null)
                initialize();
            return __bases__;
        }
        if (name == "__init__") {
            initConstructors();
            return __init__;
        }
    
        PyObject result = lookup(name, false);
        if (result != null)
            return result._doget(null);
        
        // A cache of missing attributes to short-circuit later tests
        if (missingAttributes != null &&
            missingAttributes.__finditem__(name) != null)
        {
            return null;
        }
        
        // These two tests can be expensive, see above for short-circuiting
        result = findClassAttr(name);
        if (result != null)
            return result;
            
        result = findInnerClass(name);
        if (result != null)
            return result;
        
        // Add this attribute to missing attributes cache
        if (missingAttributes == null) {
            missingAttributes = new PyStringMap();
        }
        missingAttributes.__setitem__(name, this);
        return null;
    }
        
    private PyJavaInstance classInstance;
    private PyObject findClassAttr(String name) {
        if (classInstance == null) {
            classInstance = new PyJavaInstance(proxyClass);
        }
        PyObject result = classInstance.__findattr__(name);
        return result;
        //if (result == null) return null;
        //__dict__.__setitem__(name, result);
        //return result;
    }
        
    private PyObject findInnerClass(String name) {
        try {
            Class innerClass = Class.forName(__name__+"$"+name);
            PyJavaClass jinner = new PyJavaInnerClass(innerClass, this);
            __dict__.__setitem__(name, jinner);
            return jinner;
        } catch (ClassNotFoundException exc) {
            return null;
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
    }

    public void __setattr__(String name, PyObject value) {
        PyObject field = lookup(name, false);
        if (field != null) {
            if (field._doset(null, value))
                return;
        }
        __dict__.__setitem__(name, value);
    }

    public void __delattr__(String name) {
        PyObject field = lookup(name, false);
        if (field == null) {
            throw Py.NameError("attribute not found: "+name);
        }
            
        if (!field._dodel(null)) {
            __dict__.__delitem__(name);
            //throw Py.TypeError("attr not deletable: "+name);
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (!constructorsInitialized)
            initConstructors();
        PyInstance inst = new PyJavaInstance(this);
        inst.__init__(args, keywords);
        return inst;
    }

    public Object __tojava__(Class c) {
        initialize();
        return super.__tojava__(c);
    }

    public String toString()  {
        return "<jclass "+__name__+" at "+Py.id(this)+">";
    }
}
