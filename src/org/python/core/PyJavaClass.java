// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EventListener;

import org.python.core.packagecache.PackageManager;

/**
 * A wrapper around a java class.
 */
public class PyJavaClass extends PyClass {

    private static InternalTables tbl;

    public PyReflectedConstructor __init__;

    public PackageManager __mgr__;

    public String __module__;

    private boolean initialized = false;

    // Prevent recursive calls to initialize()
    private boolean initializing = false;

    public synchronized final static InternalTables getInternalTables() {
        if (tbl == null) {
            tbl = InternalTables.createInternalTables();
        }
        return tbl;
    }

    public final boolean isLazy() {
        return proxyClass == null;
    }

    public static final PyJavaClass lookup(String name, PackageManager mgr) {
        if (tbl.queryCanonical(name)) {
            Class<?> c = mgr.findClass(null, name, "forced java class");
            check_lazy_allowed(c); // xxx
            return lookup(c);
        }
        PyJavaClass ret = new PyJavaClass(name, mgr);
        tbl.putLazyCanonical(name, ret);
        return ret;
    }

    public synchronized static final PyJavaClass lookup(Class<?> c) {
        if (tbl == null) {
            tbl = new InternalTables();
            PyJavaClass jc = new PyJavaClass();
            jc.init(PyJavaClass.class);
            tbl.putCanonical(PyJavaClass.class, jc);
        }
        PyJavaClass ret = tbl.getCanonical(c);
        if (ret != null)
            return ret;
        PyJavaClass lazy = tbl.getLazyCanonical(c.getName());
        if (lazy != null) {
            initLazy(lazy);
            if (lazy.proxyClass == c) {
                return lazy;
            }
        }
        Class<?> parent = c.getDeclaringClass();
        ret = parent == null ? new PyJavaClass(c) : new PyJavaInnerClass(c, lookup(parent));
        tbl.putCanonical(c, ret);
        return ret;
    }

    private PyJavaClass() {}

    protected PyJavaClass(Class<?> c) {
        init(c);
    }

    /**
     * Set the full name of this class.
     */
    private void setName(String name) {
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) {
            __name__ = name;
            __module__ = "";
        } else {
            __name__ = name.substring(name.lastIndexOf(".") + 1, name.length());
            __module__ = name.substring(0, name.lastIndexOf("."));
        }
    }

    private String fullName() {
        if (__module__ == "")
            return __name__;
        else
            return __module__ + "." + __name__;
    }

    protected PyJavaClass(String name, PackageManager mgr) {
        setName(name);
        this.__mgr__ = mgr;
    }

    protected void findModule(PyObject dict) {}

    protected Class<?> getProxyClass() {
        initialize();
        return proxyClass;
    }

    // for the moment trying to lazily load a PyObject subclass
    // is not allowed, (because of the PyJavaClass vs PyType class mismatch)
    // pending PyJavaClass becoming likely a subclass of PyType
    private static final void check_lazy_allowed(Class<?> c) {
        if (PyObject.class.isAssignableFrom(c)) { // xxx
            throw Py.TypeError("cannot lazy load PyObject subclass");
        }
    }

    private static final void initLazy(PyJavaClass jc) {
        Class<?> c = jc.__mgr__.findClass(null, jc.fullName(), "lazy java class");
        check_lazy_allowed(c); // xxx
        jc.init(c);
        tbl.putCanonical(jc.proxyClass, jc);
        jc.__mgr__ = null;
    }

    private synchronized void initialize() {
        if (initialized || initializing) {
            return;
        }
        initializing = true;
        synchronized (PyJavaClass.class) {
            if (proxyClass == null) {
                initLazy(this);
            }
        }
        init__bases__(proxyClass);
        init__dict__();
        if (ClassDictInit.class.isAssignableFrom(proxyClass) && proxyClass != ClassDictInit.class) {
            try {
                Method m = proxyClass.getMethod("classDictInit", PyObject.class);
                m.invoke(null, __dict__);
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        }
        if (InitModule.class.isAssignableFrom(proxyClass)) {
            try {
                InitModule m = (InitModule)proxyClass.newInstance();
                m.initModule(__dict__);
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        }
        initialized = true;
        initializing = false;
    }

    private synchronized void init__dict__() {
        if (__dict__ != null) {
            return;
        }
        PyStringMap d = new PyStringMap();
        __dict__ = d;
        try {
            Method[] methods = getAccessibleMethods(proxyClass);
            setBeanInfoCustom(proxyClass, methods);
            setFields(proxyClass);
            setMethods(proxyClass, methods);
        } catch (SecurityException se) {}
    }

    private synchronized void init__bases__(Class<?> c) {
        if (__bases__ != null)
            return;
        Class interfaces[] = getAccessibleInterfaces(c);
        int nInterfaces = interfaces.length;
        int nBases = 0;
        int i;
        for (i = 0; i < nInterfaces; i++) {
            Class inter = interfaces[i];
            if (inter == InitModule.class || inter == PyProxy.class || inter == ClassDictInit.class)
                continue;
            nBases++;
        }
        Class superclass = c.getSuperclass();
        int index = 0;
        PyObject[] bases;
        PyJavaClass tmp;
        if (superclass == null || superclass == PyObject.class) {
            bases = new PyObject[nBases];
        } else {
            bases = new PyObject[nBases + 1];
            tmp = PyJavaClass.lookup(superclass);
            bases[0] = tmp;
            tmp.initialize();
            index++;
        }
        for (i = 0; i < nInterfaces; i++) {
            Class inter = interfaces[i];
            if (inter == InitModule.class || inter == PyProxy.class || inter == ClassDictInit.class)
                continue;
            tmp = PyJavaClass.lookup(inter);
            tmp.initialize();
            bases[index++] = tmp;
        }
        __bases__ = new PyTuple(bases);
    }

    private void init(Class c) {
        proxyClass = c;
        setName(c.getName());
    }

    /**
     * Return the list of all accessible interfaces for a class. This will only the public
     * interfaces. Since we can't set accessibility on interfaces, the
     * Options.respectJavaAccessibility is not honored.
     */
    private static Class[] getAccessibleInterfaces(Class c) {
        // can't modify accessibility of interfaces in Java2
        // thus get only public interfaces
        Class[] in = c.getInterfaces();
        java.util.Vector v = new java.util.Vector();
        for (int i = 0; i < in.length; i++) {
            if (!Modifier.isPublic(in[i].getModifiers()))
                continue;
            v.addElement(in[i]);
        }
        if (v.size() == in.length)
            return in;
        Class[] ret = new Class[v.size()];
        v.copyInto(ret);
        return ret;
    }

    /**
     * Return the list of all accessible fields for a class. This will only be the public fields
     * unless Options.respectJavaAccessibility is false, in which case all fields are returned.
     */
    private static Field[] getAccessibleFields(Class c) {
        if (Options.respectJavaAccessibility)
            // returns just the public fields
            return c.getFields();
        java.util.ArrayList fields = new java.util.ArrayList();
        while (c != null) {
            // get all declared fields for this class, mutate their
            // accessibility and pop it into the array for later
            Field[] declared = c.getDeclaredFields();
            for (Field element : declared) {
                // TBD: this is a permanent change. Should we provide a
                // way to restore the original accessibility flag?
                element.setAccessible(true);
                fields.add(element);
            }
            // walk down superclass chain. no need to deal specially with
            // interfaces...
            c = c.getSuperclass();
        }
// return (Field[])fields.toArray(new Field[fields.size()]);
        Field[] ret = new Field[fields.size()];
        ret = (Field[])fields.toArray(ret);
        return ret;
    }

    private void setFields(Class c) {
        Field[] fields = getAccessibleFields(c);
        for (Field field : fields) {
            if (field.getDeclaringClass() != c)
                continue;
            String name = getName(field.getName());
            boolean isstatic = Modifier.isStatic(field.getModifiers());
            if (isstatic) {
                if (name.startsWith("__doc__") && name.length() > 7)
                    continue;
                PyObject prop = lookup(name, false);
                if (prop != null && prop instanceof PyBeanProperty) {
                    PyBeanProperty beanProp = ((PyBeanProperty)prop).copy();
                    beanProp.field = field;
                    __dict__.__setitem__(name, beanProp);
                    continue;
                }
            }
            __dict__.__setitem__(name, new PyReflectedField(field));
        }
    }

    /*
     * Produce a good Python name for a Java method. If the Java method ends in '$', strip it (this
     * handles reserved Java keywords) Don't make any changes to keywords since this is now handled
     * by parser
     */
    private String getName(String name) {
        if (name.endsWith("$"))
            name = name.substring(0, name.length() - 1);
        return name.intern();
    }

    private void addMethod(Method meth) {
        String name = getName(meth.getName());
        if (name == "_getPyInstance" || name == "_setPyInstance" || name == "_getPySystemState"
                || name == "_setPySystemState") {
            return;
        }
        // Special case to handle a few troublesome methods in java.awt.*.
        // These methods are all deprecated and interfere too badly with
        // bean properties to be tolerated. This is totally a hack, but a
        // lot of code that uses java.awt will break without it.
        String classname = proxyClass.getName();
        if (classname.startsWith("java.awt.") && classname.indexOf('.', 9) == -1) {
            if (name == "layout" || name == "insets" || name == "size" || name == "minimumSize"
                    || name == "preferredSize" || name == "maximumSize" || name == "bounds"
                    || name == "enable") {
                return;
            }
        }
        // See if any of my superclasses are using 'name' for something
        // else. Or if I'm already using it myself
        PyObject o = lookup(name, false);
        // If it's being used as a function, then things get more
        // interesting...
        PyReflectedFunction func;
        if (o != null && o instanceof PyReflectedFunction) {
            func = (PyReflectedFunction)o;
            PyObject o1 = __dict__.__finditem__(name);
            /*
             * If this function already exists, add this method to the signature. If this alters the
             * signature of the function in some significant way, then return a duplicate and stick
             * it in the __dict__
             */
            if (o1 != o) {
                if (func.handles(meth)) {
                    __dict__.__setitem__(name, func);
                    return;
                }
                func = func.copy();
            }
            func.addMethod(meth);
        } else {
            func = new PyReflectedFunction(meth);
            try {
                Field docField = proxyClass.getField("__doc__" + name);
                int mods = docField.getModifiers();
                if (docField.getType() == PyString.class && Modifier.isPublic(mods)
                        && Modifier.isStatic(mods))
                    ;
                func.__doc__ = (PyString)docField.get(null);
            } catch (NoSuchFieldException ex) {} catch (SecurityException ex) {} catch (IllegalAccessException ex) {}
        }
        __dict__.__setitem__(name, func);
    }

    /**
     * Return the list of all accessible methods for a class. This will only the public methods
     * unless Options.respectJavaAccessibility is false, in which case all methods are returned.
     */
    private static Method[] getAccessibleMethods(Class c) {
        if (Options.respectJavaAccessibility)
            // returns just the public methods
            return c.getMethods();
        Method[] declared = c.getDeclaredMethods();
        for (Method element : declared) {
            // TBD: this is a permanent change. Should we provide a way to
            // restore the original accessibility flag?
            element.setAccessible(true);
        }
        return declared;
    }

    private boolean ignoreMethod(Method method) {
        Class[] exceptions = method.getExceptionTypes();
        for (Class exception : exceptions) {
            if (exception == PyIgnoreMethodTag.class) {
                return true;
            }
        }
        return false;
    }

    /* Add all methods declared by this class */
    private void setMethods(Class c, Method[] methods) {
        for (Method method : methods) {
            Class dc = method.getDeclaringClass();
            if (dc != c)
                continue;
            if (isPackagedProtected(dc) && Modifier.isPublic(method.getModifiers())) {
                /*
                 * Set public methods on package protected classes accessible so that reflected
                 * calls to the method in subclasses of the package protected class will succeed.
                 * Yes, it's convoluted.
                 *
                 * This fails when done through reflection due to Sun JVM bug
                 * 4071957(http://tinyurl.com/le9vo). 4533479 actually describes the problem we're
                 * seeing, but there are a bevy of reflection bugs that stem from 4071957.
                 * Supposedly it'll be fixed in Dolphin but it's been promised in every version
                 * since Tiger so don't hold your breath.
                 */
                try {
                    method.setAccessible(true);
                } catch (SecurityException se) {}
            }
            if (ignoreMethod(method))
                continue;
            addMethod(method);
        }
    }

    public static boolean isPackagedProtected(Class c) {
        int mods = c.getModifiers();
        return !(Modifier.isPublic(mods) || Modifier.isPrivate(mods) || Modifier.isProtected(mods));
    }

    /* Adds a bean property to this class */
    void addProperty(String name, Class propClass, Method getMethod, Method setMethod) {
        // This will skip indexed property types
        if (propClass == null)
            return;
        boolean set = true;
        name = getName(name);
        PyBeanProperty prop = new PyBeanProperty(name, propClass, getMethod, setMethod);
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
                            && (prop.setMethod == null || oldProp.setMethod != null)) {
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
        }
        if (set)
            __dict__.__setitem__(name, prop);
    }

    /* Adds a bean event to this class */
    void addEvent(String name, Class eventClass, Method addMethod) {
        for (Method meth : eventClass.getMethods()) {
            PyBeanEventProperty prop = new PyBeanEventProperty(name, eventClass, addMethod, meth);
            __dict__.__setitem__(prop.__name__, prop);
        }
        PyBeanEvent event = new PyBeanEvent(name, eventClass, addMethod);
        __dict__.__setitem__(event.__name__, event);
    }

    // This method is a workaround for Netscape's stupid security bug!
    private void setBeanInfoCustom(Class c, Method[] meths) {
        for (Method method : meths) {
            if (ignoreMethod(method) || method.getDeclaringClass() != c
                    || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            Method getter = null;
            Method setter = null;
            Class[] args = method.getParameterTypes();
            Class ret = method.getReturnType();
            Class myType = null;
            String pname = "";
            if (name.startsWith("get")) {
                if (args.length != 0)
                    continue;
                getter = method;
                pname = Introspector.decapitalize(name.substring(3));
                myType = ret;
            } else {
                if (name.startsWith("is")) {
                    if (args.length != 0 || ret != Boolean.TYPE)
                        continue;
                    getter = method;
                    pname = Introspector.decapitalize(name.substring(2));
                    myType = ret;
                } else {
                    if (name.startsWith("set")) {
                        if (args.length != 1)
                            continue;
                        setter = method;
                        pname = Introspector.decapitalize(name.substring(3));
                        myType = args[0];
                    } else {
                        continue;
                    }
                }
            }
            PyObject o = __dict__.__finditem__(new PyString(pname));
            PyBeanProperty prop;
            if (o == null || !(o instanceof PyBeanProperty)) {
                addProperty(pname, myType, getter, setter);
            } else {
                prop = (PyBeanProperty)o;
                if (prop.myType != myType) {
                    if (getter != null) {
                        addProperty(pname, myType, getter, setter);
                    }
                } else {
                    if (getter != null)
                        prop.getMethod = getter;
                    if (setter != null && (ret == Void.TYPE || prop.setMethod == null))
                        prop.setMethod = setter;
                }
            }
        }
        for (Method method : meths) {
            if (method.getDeclaringClass() != c || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String mname = method.getName();
            if (!(mname.startsWith("add") || mname.startsWith("set"))
                    || !mname.endsWith("Listener")) {
                continue;
            }
            Class[] args = method.getParameterTypes();
            Class ret = method.getReturnType();
            if (args.length != 1 || ret != Void.TYPE)
                continue;
            Class eClass = args[0];
            if (!EventListener.class.isAssignableFrom(eClass))
                continue;
            String name = eClass.getName();
            int idot = name.lastIndexOf('.');
            if (idot != -1)
                name = Introspector.decapitalize(name.substring(idot + 1));
            addEvent(name, eClass, method);
        }
    }

    /**
     * Return the list of all accessible constructors for a class. This will only the public
     * constructors unless Options.respectJavaAccessibility is false, in which case all constructors
     * are returned. Note that constructors are not inherited like methods or fields.
     */
    private static Constructor[] getAccessibleConstructors(Class c) {
        if (Options.respectJavaAccessibility)
            // returns just the public fields
            return c.getConstructors();
        // return all constructors
        Constructor[] declared = c.getDeclaredConstructors();
        for (Constructor element : declared) {
            // TBD: this is a permanent change. Should we provide a way to
            // restore the original accessibility flag?
            element.setAccessible(true);
        }
        return declared;
    }

    private boolean ignoreConstructor(Constructor method) {
        Class[] exceptions = method.getExceptionTypes();
        for (Class exception : exceptions) {
            if (exception == PyIgnoreMethodTag.class) {
                return true;
            }
        }
        return false;
    }

    private void setConstructors(Class c) {
        if (Modifier.isInterface(c.getModifiers())) {
            __init__ = null;
        } else {
            Constructor[] constructors = getAccessibleConstructors(c);
            for (Constructor constructor : constructors) {
                if (ignoreConstructor(constructor)) {
                    continue;
                }
                if (__init__ == null) {
                    __init__ = new PyReflectedConstructor(constructor);
                } else {
                    __init__.addConstructor(constructor);
                }
            }
            if (__init__ != null) {
                __dict__.__setitem__("__init__", __init__);
            }
        }
    }

    private boolean constructorsInitialized = false;

    synchronized void initConstructors() {
        if (constructorsInitialized)
            return;
        initialize();
        setConstructors(proxyClass);
        constructorsInitialized = true;
    }

    /*
     * If the new name conflicts with a Python keyword, add an '_'
     */
    private static java.util.Hashtable keywords = null;

    private static String unmangleKeyword(String name) {
        if (keywords == null) {
            keywords = new java.util.Hashtable();
            String[] words =
                new String[] {"or", "and", "not", "is", "in", "lambda", "if", "else", "elif",
                              "while", "for", "try", "except", "def", "class", "finally", "print",
                              "pass", "break", "continue", "return", "import", "from", "del",
                              "raise", "global", "exec", "assert"};
            for (String word : words) {
                keywords.put(word + "_", word.intern());
            }
        }
        return (String)keywords.get(name);
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
        // keyword. An improved parser makes this no longer necessary.
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

    public PyObject __findattr_ex__(String name) {
        if (name == "__dict__") {
            if (__dict__ == null)
                initialize();
            return __dict__;
        }
        if (name == "__name__")
            return new PyString(__name__);
        if (name == "__module__")
            return new PyString(__module__);
        if (name == "__bases__") {
            if (__bases__ == null)
                initialize();
            return __bases__;
        }
        if (name == "__init__") {
            initConstructors();
            if (__init__ == null)
                return super.lookupGivingClass(name, false)[0];
            return __init__;
        }
        PyObject result = lookup(name, false);
        if (result != null)
            return result.__get__(null, null); // xxx messy
        // A cache of missing attributes to short-circuit later tests
        if (missingAttributes != null && missingAttributes.__finditem__(name) != null) {
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
        // if (result == null) return null;
        // __dict__.__setitem__(name, result);
        // return result;
    }

    private PyObject findInnerClass(String name) {
        Class<?> innerClass = null;
        for (Class<?> inner : getProxyClass().getClasses()) {
            if(inner.getSimpleName().equals(name)) {
                innerClass = inner;
                break;
            }
        }
        if (innerClass == null) {
            return null;
        }
        PyObject jinner = Py.java2py(innerClass); // xxx lookup(innerClass);
        __dict__.__setitem__(name, jinner);
        return jinner;
    }

    public void __setattr__(String name, PyObject value) {
        PyObject field = lookup(name, false);
        if (field != null) {
            if (field.jtryset(null, value))
                return;
        }
        __dict__.__setitem__(name, value);
    }

    public void __delattr__(String name) {
        PyObject field = lookup(name, false);
        if (field == null) {
            throw Py.NameError("attribute not found: " + name);
        }
        if (!field.jdontdel()) {
            __dict__.__delitem__(name);
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (!constructorsInitialized)
            initConstructors();
        PyInstance inst = new PyJavaInstance(this);
        inst.__init__(args, keywords);
        return inst;
    }

    public Object __tojava__(Class<?> c) {
        initialize();
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        if (!(other instanceof PyJavaClass)) {
            return -2;
        }
        int c = fullName().compareTo(((PyJavaClass)other).fullName());
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    public PyString __str__() {
        return new PyString(fullName());
    }

    public String toString() {
        return "<jclass " + fullName() + " " + Py.idstr(this) + ">";
    }
}
