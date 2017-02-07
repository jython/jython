package org.python.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.python.core.util.StringUtil;
import org.python.util.Generic;


public class PyJavaType extends PyType {

    private final static Class<?>[] OO = {PyObject.class, PyObject.class};

    /** Deprecated methods in java.awt.* that have bean property equivalents we prefer. */
    private final static Set<String> BAD_AWT_METHODS = Generic.set("layout",
                                                                   "insets",
                                                                   "size",
                                                                   "minimumSize",
                                                                   "preferredSize",
                                                                   "maximumSize",
                                                                   "bounds",
                                                                   "enable");


    // Add well-known immutable classes from standard packages of
    // java.lang, java.net, java.util that are not marked Cloneable.
    // This was found by hand, there are likely more!
    private final static Set<Class<?>> immutableClasses = Generic.set(
            Boolean.class,
            Byte.class,
            Character.class,
            Class.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            String.class,
            java.net.InetAddress.class,
            java.net.Inet4Address.class,
            java.net.Inet6Address.class,
            java.net.InetSocketAddress.class,
            java.net.Proxy.class,
            java.net.URI.class,
            java.util.concurrent.TimeUnit.class);


    /**
     * Other Java classes this type has MRO conflicts with. This doesn't matter for Java method
     * resolution, but if Python methods are added to the type, the added methods can't overlap with
     * methods added to any of the types in this set. If this type doesn't have any known conflicts,
     * this is null.
     */
    private Set<PyJavaType> conflicted;

    /**
     * The names of methods that have been added to this class.
     */
    private Set<String> modified;

    public static PyObject wrapJavaObject(Object o) {
        PyObject obj = new PyObjectDerived(PyType.fromClass(o.getClass(), false));
        JyAttribute.setAttr(obj, JyAttribute.JAVA_PROXY_ATTR, o);
        return obj;
    }

    public PyJavaType() {
        super(TYPE == null ? fromClass(PyType.class) : TYPE);
    }

    @Override
    protected boolean useMetatypeFirst(PyObject attr) {
        return !(attr instanceof PyReflectedField || attr instanceof PyReflectedFunction ||
                attr instanceof PyBeanEventProperty);
    }

    // Java types are ok with things being added and removed from their dicts as long as there isn't
    @Override
    void type___setattr__(String name, PyObject value) {
        PyObject field = lookup(name);// If we have a static field that takes this, go with that
        if (field != null) {
            if (field._doset(null, value)) {
                return;
            }
        }
        if (modified == null) {
            modified = Generic.set();
        }
        if (modified.add(name)) {
            if (conflicted != null) {
                for (PyJavaType conflict : conflicted) {
                    if (conflict.modified != null && conflict.modified.contains(name)) {
                        throw Py.TypeError(getName()
                                + " does not have a consistent method resolution order with "
                                + conflict.getName() + ", and it already has " + name
                                + " added for Python");
                    }
                }
            }
        }
        object___setattr__(name, value);
        postSetattr(name);
    }

    @Override
    void type___delattr__(String name) {
        PyObject field = lookup(name);
        if (field == null) {
            throw Py.NameError("attribute not found: "+name);
        }
        if (!field.jdontdel()) {
            object___delattr__(name);
        }
        if (modified != null) {
            modified.remove(name);
        }
        postDelattr(name);
    }

    @Override
    void handleMroError(MROMergeState[] toMerge, List<PyObject> mro) {
        if (underlying_class != null) {
            // If this descends from PyObject, don't do the Java mro cleanup
            super.handleMroError(toMerge, mro);
        }
        Set<PyJavaType> inConflict = Generic.set();
        PyJavaType winner = null;
        for (MROMergeState mergee : toMerge) {
            for (int i = mergee.next; i < mergee.mro.length; i++) {
                if (mergee.mro[i] == PyObject.TYPE
                        || mergee.mro[i] == PyType.fromClass(Object.class)) {
                    continue;
                }
                if (winner == null) {
                    // Pick an arbitrary class to be added to the mro next and break the conflict.
                    // If method name conflicts were allowed between methods added to Java types,
                    // it would go first, but that's prevented, so being a winner doesn't actually
                    // get it anything
                    winner = (PyJavaType)mergee.mro[i];
                }
                inConflict.add((PyJavaType)mergee.mro[i]);
            }
        }

        Set<String> allModified = Generic.set();
        PyJavaType[] conflictedAttributes = inConflict.toArray(new PyJavaType[inConflict.size()]);
        for (PyJavaType type : conflictedAttributes) {
            if (type.modified == null) {
                continue;
            }
            for (String method : type.modified) {
                if (!allModified.add(method)) { // Another type in conflict has this method, possibly fail
                    PyList types = new PyList();
                    Set<Class<?>> proxySet = Generic.set();
                    for (PyJavaType othertype : conflictedAttributes) {
                        if (othertype.modified != null && othertype.modified.contains(method)) {
                            types.add(othertype);
                            proxySet.add(othertype.getProxyType());
                        }
                    }
                    // Need to special case collections that implement both Iterable and Map. Ignore the conflict
                    // in having duplicate __iter__ added (see getCollectionProxies), while still allowing each
                    // path on the inheritance hierarchy to get an __iter__. Annoying but necessary logic.
                    // See http://bugs.jython.org/issue1878
                    if (method.equals("__iter__") && proxySet.equals(Generic.set(Iterable.class, Map.class))) {
                        continue;
                    }
                    throw Py.TypeError(String.format("Supertypes that share a modified attribute "
                            + "have an MRO conflict[attribute=%s, supertypes=%s, type=%s]",
                            method, types, this.getName()));
                }
            }
        }

        // We can keep trucking, there aren't any existing method name conflicts.  Mark the
        // conflicts in all the classes so further method additions can check for trouble
        for (PyJavaType type : conflictedAttributes) {
            for (PyJavaType otherType : inConflict) {
                if (otherType != type) {
                    if (type.conflicted == null) {
                        type.conflicted = Generic.set();
                    }
                    type.conflicted.add(otherType);
                }
            }
        }

        // Add our winner to the mro, clear the clog, and try to finish the rest
        mro.add(winner);
        for (MROMergeState mergee : toMerge) {
            mergee.removeFromUnmerged(winner);
        }
        computeMro(toMerge, mro);
    }

    @Override
    protected void init(Class<?> forClass, Set<PyJavaType> needsInners) {
        name = forClass.getName();
        // Strip the java fully qualified class name from Py classes in core
        if (name.startsWith("org.python.core.Py")) {
            name = name.substring("org.python.core.Py".length()).toLowerCase();
        }
        dict = new PyStringMap();
        Class<?> baseClass = forClass.getSuperclass();
        if (PyObject.class.isAssignableFrom(forClass)) {
            // Non-exposed subclasses of PyObject use a simple linear mro to PyObject that ignores
            // their interfaces
            underlying_class = forClass;
            computeLinearMro(baseClass);
        } else {
            needsInners.add(this);
            JyAttribute.setAttr(this, JyAttribute.JAVA_PROXY_ATTR, forClass);
            objtype = PyType.fromClassSkippingInners(Class.class, needsInners);
            // Wrapped Java types fill in their mro first using all of their interfaces then their
            // super class.
            List<PyObject> visibleBases = Generic.list();
            for (Class<?> iface : forClass.getInterfaces()) {
                if (iface == PyProxy.class || iface == ClassDictInit.class) {
                    // Don't show the interfaces added by proxy type construction; otherwise Python
                    // subclasses of proxy types and another Java interface can't make a consistent
                    // mro
                    continue;
                }
                if (baseClass != null && iface.isAssignableFrom(baseClass)) {
                    // Don't include redundant interfaces. If the redundant interface has methods
                    // that were combined with methods of the same name from other interfaces higher
                    // in the hierarchy, adding it here hides the forms from those interfaces.
                    continue;
                }
                visibleBases.add(PyType.fromClassSkippingInners(iface, needsInners));
            }

            Object javaProxy = JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR);

            if (javaProxy == Object.class) {
                base = PyType.fromClassSkippingInners(PyObject.class, needsInners);
            } else if(baseClass == null) {
                base = PyType.fromClassSkippingInners(Object.class, needsInners);
            } else if (javaProxy == Class.class) {
                base = PyType.fromClassSkippingInners(PyType.class, needsInners);
            } else {
                base = PyType.fromClassSkippingInners(baseClass, needsInners);
            }
            visibleBases.add(base);
            this.bases = visibleBases.toArray(new PyObject[visibleBases.size()]);
            mro = computeMro();
        }

        // PyReflected* can't call or access anything from non-public classes that aren't in
        // org.python.core
        if (!Modifier.isPublic(forClass.getModifiers()) &&
                !name.startsWith("org.python.core") && Options.respectJavaAccessibility) {
            handleSuperMethodArgCollisions(forClass);
            return;
        }

        // Add methods and determine bean properties declared on this class
        Map<String, PyBeanProperty> props = Generic.map();
        Map<String, PyBeanEvent<?>> events = Generic.map();
        Method[] methods;
        if (Options.respectJavaAccessibility) {
            // returns just the public methods
            methods = forClass.getMethods();
        } else {
            // Grab all methods on this class and all of its superclasses and make them accessible
            List<Method> allMethods = Generic.list();
            for(Class<?> c = forClass; c != null; c = c.getSuperclass()) {
                for (Method meth : c.getDeclaredMethods()) {
                    allMethods.add(meth);
                    meth.setAccessible(true);
                }
            }
            methods = allMethods.toArray(new Method[allMethods.size()]);
        }

        /* make sure we "sort" all methods so they resolve in the right order. See #2391 for details */
        Arrays.sort(methods, new MethodComparator(new ClassComparator()));

        boolean isInAwt = name.startsWith("java.awt.") && name.indexOf('.', 9) == -1;
        ArrayList<PyReflectedFunction> reflectedFuncs = new ArrayList<>(methods.length);
        PyReflectedFunction reflfunc;
        for (Method meth : methods) {
            if (!declaredOnMember(baseClass, meth) || ignore(meth)) {
                continue;
            }

            String methname = meth.getName();

            // Special case a few troublesome methods in java.awt.*. These methods are all
            // deprecated and interfere too badly with bean properties to be tolerated. This is
            // totally a hack but a lot of code that uses java.awt will break without it.
            if (isInAwt && BAD_AWT_METHODS.contains(methname)) {
                continue;
            }

            String nmethname = normalize(methname);
            reflfunc = (PyReflectedFunction) dict.__finditem__(nmethname);
            if (reflfunc == null) {
                reflfunc = new PyReflectedFunction(meth);
                reflectedFuncs.add(reflfunc);
                dict.__setitem__(nmethname, reflfunc);
            } else {
                reflfunc.addMethod(meth);
            }

            // Now check if this is a bean method, for which it must be an instance method
            if (Modifier.isStatic(meth.getModifiers())) {
                continue;
            }

            // First check if this is a bean event addition method
            int n = meth.getParameterTypes().length;
            if ((methname.startsWith("add") || methname.startsWith("set"))
                    && methname.endsWith("Listener") && n == 1 &&
                    meth.getReturnType() == Void.TYPE &&
                    EventListener.class.isAssignableFrom(meth.getParameterTypes()[0])) {
                Class<?> eventClass = meth.getParameterTypes()[0];
                String ename = eventClass.getName();
                int idot = ename.lastIndexOf('.');
                if (idot != -1) {
                    ename = ename.substring(idot + 1);
                }
                ename = normalize(StringUtil.decapitalize(ename));
                events.put(ename, new PyBeanEvent<>(ename, eventClass, meth));
                continue;
            }

            // Now check if it's a bean property accessor
            String beanPropertyName = null;
            boolean get = true;
            if (methname.startsWith("get") && methname.length() > 3 && n == 0) {
                beanPropertyName = methname.substring(3);
            } else if (methname.startsWith("is") && methname.length() > 2 && n == 0
                    && meth.getReturnType() == Boolean.TYPE) {
                beanPropertyName = methname.substring(2);
            } else if (methname.startsWith("set") && methname.length() > 3 && n == 1) {
                beanPropertyName = methname.substring(3);
                get = false;
            }
            if (beanPropertyName != null) {
                beanPropertyName = normalize(StringUtil.decapitalize(beanPropertyName));
                PyBeanProperty prop = props.get(beanPropertyName);
                if (prop == null) {
                    prop = new PyBeanProperty(beanPropertyName, null, null, null);
                    props.put(beanPropertyName, prop);
                }
                if (get) {
                    prop.getMethod = meth;
                    prop.myType = meth.getReturnType();
                } else {
                    prop.setMethod = meth;
                    // Needed for readonly properties.  Getter will be used instead
                    // if there is one.  Only works if setX method has exactly one
                    // param, which is the only reasonable case.
                    // XXX: should we issue a warning if setX and getX have different
                    // types?
                    if (prop.myType == null) {
                        Class<?>[] params = meth.getParameterTypes();
                        if (params.length == 1) {
                            prop.myType = params[0];
                        }
                    }
                }
            }
        }

        // Add superclass methods
        for (Method meth : methods) {
            String nmethname = normalize(meth.getName());
            reflfunc = (PyReflectedFunction) dict.__finditem__(nmethname);
            if (reflfunc != null) {
                // The superclass method has the same name as one declared on this class, so add
                // the superclass version's arguments
                reflfunc.addMethod(meth);
            } else if (PyReflectedFunction.isPackagedProtected(meth.getDeclaringClass())
                    && lookup(nmethname) == null) {
                // This method must be a public method from a package protected superclass.  It's
                // visible from Java on this class, so do the same for Python here.  This is the
                // flipside of what handleSuperMethodArgCollisions does for inherited public methods
                // on package protected classes.
                reflfunc = new PyReflectedFunction(meth);
                reflectedFuncs.add(reflfunc);
                dict.__setitem__(nmethname, reflfunc);
            }
        }

        // Add fields declared on this type
        Field[] fields;
        if (Options.respectJavaAccessibility) {
            // returns just the public fields
            fields = forClass.getFields();
        } else {
            fields = forClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }
        }
        for (Field field : fields) {
            if (!declaredOnMember(baseClass, field)) {
                continue;
            }
            String fldname = field.getName();
            if (Modifier.isStatic(field.getModifiers())) {
                if (fldname.startsWith("__doc__") && fldname.length() > 7
                        && CharSequence.class.isAssignableFrom(field.getType())) {
                    String fname = fldname.substring(7).intern();
                    PyObject memb = dict.__finditem__(fname);
                    if (memb != null && memb instanceof PyReflectedFunction) {
                        CharSequence doc = null;
                        try {
                            doc = (CharSequence) field.get(null);
                        } catch (IllegalAccessException e) {
                            throw Py.JavaError(e);
                        }
                        ((PyReflectedFunction)memb).__doc__ = doc instanceof PyString ?
                                (PyString) doc : new PyString(doc.toString());
                    }
                }
            }
            if (dict.__finditem__(normalize(fldname)) == null) {
                dict.__setitem__(normalize(fldname), new PyReflectedField(field));
            }
        }

        for (PyBeanEvent<?> ev : events.values()) {
            if (dict.__finditem__(ev.__name__) == null) {
                dict.__setitem__(ev.__name__, ev);
            }

            for (Method meth : ev.eventClass.getMethods()) {
                String methodName = meth.getName().intern();
                if (dict.__finditem__(methodName) != null) {
                    continue;
                }
                dict.__setitem__(methodName, new PyBeanEventProperty(methodName,
                                                               ev.eventClass,
                                                               ev.addMethod,
                                                               meth));
            }
        }

        // Fill in the bean properties picked up while going through the methods
        for (PyBeanProperty prop : props.values()) {
            PyObject prev = dict.__finditem__(prop.__name__);
            if (prev != null) {
                if (!(prev instanceof PyReflectedField)
                        || !Modifier.isStatic(((PyReflectedField)prev).field.getModifiers())) {
                    // Any methods or non-static fields take precedence over the bean property
                    continue;
                } else {
                    // Must've been a static field, so add it to the property
                    prop.field = ((PyReflectedField)prev).field;
                }
            }

            // If one of our superclasses  has something defined for this name, check if its a bean
            // property, and if so, try to fill in any gaps in our property from there
            PyObject fromType[] = new PyObject[] { null };
            PyObject superForName = lookup_where_mro(prop.__name__, fromType);
            if (superForName instanceof PyBeanProperty) {
                PyBeanProperty superProp = ((PyBeanProperty)superForName);
                // If it has a set method and we don't, take it regardless.  If the types don't line
                // up, it'll be rejected below
                if (prop.setMethod == null) {
                    prop.setMethod = superProp.setMethod;
                } else if (prop.getMethod == null
                           && superProp.myType == prop.setMethod.getParameterTypes()[0]) {
                    // Only take a get method if the type on it agrees with the set method
                    // we already have. The bean on this type overrides a conflicting one
                    // of the parent
                    prop.getMethod = superProp.getMethod;
                    prop.myType = superProp.myType;
                }

                if (prop.field == null) {
                    // If the parent bean is hiding a static field, we need it as well.
                    prop.field = superProp.field;
                }
            } else if (superForName != null  &&  fromType[0] != this  &&  !(superForName instanceof PyBeanEvent)) {
                // There is already an entry for this name
                // It came from a type which is not @this; it came from a superclass
                // It is not a bean event
                // Do not override methods defined in superclass
                continue;
            }
            // If the return types on the set and get methods for a property don't agree, the get
            // method takes precedence
            if (prop.getMethod != null && prop.setMethod != null
                    && prop.myType != prop.setMethod.getParameterTypes()[0]) {
                prop.setMethod = null;
            }
            dict.__setitem__(prop.__name__, prop);
        }

        final PyReflectedConstructor reflctr = new PyReflectedConstructor(name);
        Constructor<?>[] constructors;
        // No matter the security manager, trying to set the constructor on class to accessible
        // blows up
        if (Options.respectJavaAccessibility || Class.class == forClass) {
            // returns just the public constructors
            constructors = forClass.getConstructors();
        } else {
            constructors = forClass.getDeclaredConstructors();
            for (Constructor<?> ctr : constructors) {
                ctr.setAccessible(true);
            }
        }
        for (Constructor<?> ctr : constructors) {
            reflctr.addConstructor(ctr);
        }
        if (PyObject.class.isAssignableFrom(forClass)) {
            PyObject new_ = new PyNewWrapper(forClass, "__new__", -1, -1) {
                @Override public PyObject new_impl(boolean init,
                                         PyType subtype,
                                         PyObject[] args,
                                         String[] keywords) {
                    return reflctr.make(args, keywords);
                }
            };
            dict.__setitem__("__new__", new_);
        } else {
            dict.__setitem__("__init__", reflctr);
        }
        PyBuiltinMethod[] collectionProxyMethods = getCollectionProxies().get(forClass);
        if (collectionProxyMethods != null) {
            for (PyBuiltinMethod meth : collectionProxyMethods) {
                addMethod(meth);
            }
        }
        // allow for some methods to override the Java type's methods as a late injection
        for (Class<?> type : getPostCollectionProxies().keySet()) {
            if (type.isAssignableFrom(forClass)) {
                for (PyBuiltinMethod meth : getPostCollectionProxies().get(type)) {
                    addMethod(meth);
                }
            }
        }

        PyObject nameSpecified = null;
        if (ClassDictInit.class.isAssignableFrom(forClass) && forClass != ClassDictInit.class) {
            try {
                Method m = forClass.getMethod("classDictInit", PyObject.class);
                m.invoke(null, dict);
                // allow the class to override its name after it is loaded
                nameSpecified = dict.__finditem__("__name__");
                if (nameSpecified != null) {
                    name = nameSpecified.toString();
                }
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        }

        // Fill __module__ attribute of PyReflectedFunctions...
        if (reflectedFuncs.size() > 0) {
            if (nameSpecified == null) {
                nameSpecified = Py.newString(name);
            }
            for (PyReflectedFunction func: reflectedFuncs) {
                func.__module__ = nameSpecified;
            }
        }

        if (baseClass != Object.class) {
            hasGet = getDescrMethod(forClass, "__get__", OO) != null
                    || getDescrMethod(forClass, "_doget", PyObject.class) != null
                    || getDescrMethod(forClass, "_doget", OO) != null;
            hasSet = getDescrMethod(forClass, "__set__", OO) != null
                    || getDescrMethod(forClass, "_doset", OO) != null;
            hasDelete = getDescrMethod(forClass, "__delete__", PyObject.class) != null
                    || getDescrMethod(forClass, "_dodel", PyObject.class) != null;
        }

        if (forClass == Object.class) {
            addMethod(new PyBuiltinMethodNarrow("__copy__") {
                @Override
                public PyObject __call__() {
                    throw Py.TypeError("Could not copy Java object because it is not Cloneable or known to be immutable. "
                            + "Consider monkeypatching __copy__ for " + self.getType().fastGetName());
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__deepcopy__") {
                @Override
                public PyObject __call__(PyObject memo) {
                    throw Py.TypeError("Could not deepcopy Java object because it is not Serializable. "
                            + "Consider monkeypatching __deepcopy__ for " + self.getType().fastGetName());
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__eq__", 1) {
                @Override
                public PyObject __call__(PyObject o) {
                    Object proxy = self.getJavaProxy();
                    Object oProxy = o.getJavaProxy();
                    return proxy.equals(oProxy) ? Py.True : Py.False;
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__ne__", 1) {
                @Override
                public PyObject __call__(PyObject o) {
                    Object proxy = self.getJavaProxy();
                    Object oProxy = o.getJavaProxy();
                    return !proxy.equals(oProxy) ? Py.True : Py.False;
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__hash__") {
                @Override
                public PyObject __call__() {
                    return Py.newInteger(self.getJavaProxy().hashCode());
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__repr__") {
                @Override
                public PyObject __call__() {
                    /*
                     * java.lang.Object.toString returns Unicode: preserve as a PyUnicode, then let
                     * the repr() built-in decide how to handle it. (Also applies to __str__.)
                     */
                    String toString = self.getJavaProxy().toString();
                    return toString == null ? Py.EmptyUnicode : Py.newUnicode(toString);
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__unicode__") {
                @Override
                public PyObject __call__() {
                    return new PyUnicode(self.toString());
                }
            });
        }

        if(forClass == Comparable.class) {
            addMethod(new ComparableMethod("__lt__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison < 0;
                }
            });
            addMethod(new ComparableMethod("__le__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison <= 0;
                }
            });
            addMethod(new ComparableMethod("__gt__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison > 0;
                }
            });
            addMethod(new ComparableMethod("__ge__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison >= 0;
                }
            });
        }

        if (immutableClasses.contains(forClass)) {
            // __deepcopy__ just works for these objects since it uses serialization instead
            addMethod(new PyBuiltinMethodNarrow("__copy__") {
                @Override
                public PyObject __call__() {
                    return self;
                }
            });
        }

        if(forClass == Cloneable.class) {
            addMethod(new PyBuiltinMethodNarrow("__copy__") {
                @Override
                public PyObject __call__() {
                    Object obj = self.getJavaProxy();
                    Method clone;
                    // TODO we could specialize so that for well known objects like collections.
                    // This would avoid needing to use reflection in the general case,
                    // because Object#clone is protected (but most subclasses are not).
                    //
                    // Lastly we can potentially cache the method handle in the proxy instead of looking it up each time
                    try {
                        clone = obj.getClass().getMethod("clone");
                        Object copy = clone.invoke(obj);
                        return Py.java2py(copy);
                    } catch (Exception ex) {
                        throw Py.TypeError("Could not copy Java object");
                    }
                }
            });
        }

        if(forClass == Serializable.class) {
            addMethod(new PyBuiltinMethodNarrow("__deepcopy__") {
                @Override
                public PyObject __call__(PyObject memo) {
                    Object obj = self.getJavaProxy();
                    try {
                        Object copy = cloneX(obj);
                        return Py.java2py(copy);
                    } catch (Exception ex) {
                        throw Py.TypeError("Could not copy Java object");
                    }
                }
            });
        }
    }

    // cloneX, CloneOutput, CloneInput are verbatim from Eamonn McManus'
    // http://weblogs.java.net/blog/emcmanus/archive/2007/04/cloning_java_ob.html
    // blog post on deep cloning through serialization -
    // just what we need for __deepcopy__ support of Java objects
    private static <T> T cloneX(T x) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        CloneOutput cout = new CloneOutput(bout);
        cout.writeObject(x);
        byte[] bytes = bout.toByteArray();

        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        CloneInput cin = new CloneInput(bin, cout);

        @SuppressWarnings("unchecked")  // thanks to Bas de Bakker for the tip!
        T clone = (T) cin.readObject();
        cin.close();
        return clone;
    }

    private static class CloneOutput extends ObjectOutputStream {
        Queue<Class<?>> classQueue = new LinkedList<Class<?>>();

        CloneOutput(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void annotateClass(Class<?> c) {
            classQueue.add(c);
        }

        @Override
        protected void annotateProxyClass(Class<?> c) {
            classQueue.add(c);
        }
    }

    private static class CloneInput extends ObjectInputStream {
        private final CloneOutput output;

        CloneInput(InputStream in, CloneOutput output) throws IOException {
            super(in);
            this.output = output;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass osc)
                throws IOException, ClassNotFoundException {
            Class<?> c = output.classQueue.poll();
            String expected = osc.getName();
            String found = (c == null) ? null : c.getName();
            if (!expected.equals(found)) {
                throw new InvalidClassException("Classes desynchronized: " +
                        "found " + found + " when expecting " + expected);
            }
            return c;
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaceNames)
                throws IOException, ClassNotFoundException {
            return output.classQueue.poll();
        }
    }

    /**
     * Private, protected or package protected classes that implement public interfaces or extend
     * public classes can't have their implementations of the methods of their supertypes called
     * through reflection due to Sun VM bug 4071957(http://tinyurl.com/le9vo). They can be called
     * through the supertype version of the method though. Unfortunately we can't just let normal
     * mro lookup of those methods handle routing the call to the correct version as a class can
     * implement interfaces or classes that each have methods with the same name that takes
     * different number or types of arguments. Instead this method goes through all interfaces
     * implemented by this class, and combines same-named methods into a single PyReflectedFunction.
     *
     * Prior to Jython 2.5, this was handled in PyJavaClass.setMethods by setting methods in package
     * protected classes accessible which made them callable through reflection. That had the
     * drawback of failing when running in a security environment that didn't allow setting
     * accessibility, so this method replaced it.
     */
    private void handleSuperMethodArgCollisions(Class<?> forClass) {
        for (Class<?> iface : forClass.getInterfaces()) {
            mergeMethods(iface);
        }
        if (forClass.getSuperclass() != null) {
            mergeMethods(forClass.getSuperclass());
            if (!Modifier.isPublic(forClass.getSuperclass().getModifiers())) {
                // If the superclass is also not public, it needs to get the same treatment as we
                // can't call its methods either.
                handleSuperMethodArgCollisions(forClass.getSuperclass());
            }
        }
    }

    private void mergeMethods(Class<?> parent) {
        for (Method meth : parent.getMethods()) {
            if (!Modifier.isPublic(meth.getDeclaringClass().getModifiers())) {
                // Ignore methods from non-public interfaces as they're similarly bugged
                continue;
            }
            String nmethname = normalize(meth.getName());
            PyObject[] where = new PyObject[1];
            PyObject obj = lookup_where_mro(nmethname, where);
            if (obj == null) {
                // Nothing in our supertype hierarchy defines something with this name, so it
                // must not be visible there.
                continue;
            } else if (where[0] == this) {
                // This method is the only thing defining items in this class' dict, so it must
                // be a PyReflectedFunction created here. See if it needs the current method
                // added to it.
                if (!((PyReflectedFunction)obj).handles(meth)) {
                    ((PyReflectedFunction)obj).addMethod(meth);
                }
            } else {
                // There's something in a superclass with the same name. Add an item to this type's
                // dict to hide it.  If it's this method, nothing's changed.  If it's a field, we
                // want to make the method visible.  If it's a different method, it'll be added to
                // the reflected function created here in a later call.
                dict.__setitem__(nmethname, new PyReflectedFunction(meth));
            }
        }
    }

    private static boolean declaredOnMember(Class<?> base, Member declaring) {
        return base == null || (declaring.getDeclaringClass() != base &&
                base.isAssignableFrom(declaring.getDeclaringClass()));
    }

    private static String normalize(String name) {
        if (name.endsWith("$")) {
            name = name.substring(0, name.length() - 1);
        }
        return name.intern();
    }

    private static Method getDescrMethod(Class<?> c, String name, Class<?>... parmtypes) {
        Method meth;
        try {
            meth = c.getMethod(name, parmtypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
        if (!Modifier.isStatic(meth.getModifiers()) && meth.getDeclaringClass() != PyObject.class) {
            return meth;
        }
        return null;
    }

    private static boolean ignore(Method meth) {
        Class<?>[] exceptions = meth.getExceptionTypes();
        for (Class<?> exception : exceptions) {
            if (exception == PyIgnoreMethodTag.class) {
                return true;
            }
        }
        return false;
    }

    private static class EnumerationIter extends PyIterator {
        private Enumeration<Object> proxy;

        public EnumerationIter(Enumeration<Object> proxy) {
            this.proxy = proxy;
        }

        public PyObject __iternext__() {
            return proxy.hasMoreElements() ? Py.java2py(proxy.nextElement()) : null;
        }
    }

    private static abstract class ComparableMethod extends PyBuiltinMethodNarrow {

        protected ComparableMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        @SuppressWarnings("unchecked")
        @Override
        public PyObject __call__(PyObject arg) {
            Object asjava = arg.__tojava__(Object.class);
            int compare;
            try {
                compare = ((Comparable<Object>)self.getJavaProxy()).compareTo(asjava);
            } catch(ClassCastException classCast) {
                return Py.NotImplemented;
            }
            return getResult(compare) ? Py.True : Py.False;
        }

        protected abstract boolean getResult(int comparison);
    }

    // Traverseproc-note: Usually we would have to traverse this class, but we can
    // leave this out, since CollectionProxies is only used locally in private
    // static fields.
    private static class CollectionProxies {
        final Map<Class<?>, PyBuiltinMethod[]> proxies;
        final Map<Class<?>, PyBuiltinMethod[]> postProxies;

        CollectionProxies() {
            proxies = buildCollectionProxies();
            postProxies = buildPostCollectionProxies();
        }
    }

    private static class CollectionsProxiesHolder {
        static final CollectionProxies proxies = new CollectionProxies();
    }

    private static Map<Class<?>, PyBuiltinMethod[]> getCollectionProxies() {
        return CollectionsProxiesHolder.proxies.proxies;
    }

    private static Map<Class<?>, PyBuiltinMethod[]> getPostCollectionProxies() {
        return CollectionsProxiesHolder.proxies.postProxies;
    }

    /**
     * Build a map of common Java collection base types (Map, Iterable, etc) that need to be
     * injected with Python's equivalent types' builtin methods (__len__, __iter__, iteritems, etc).
     *
     * @return A map whose key is the base Java collection types and whose entry is a list of
     *         injected methods.
     */
    private static Map<Class<?>, PyBuiltinMethod[]> buildCollectionProxies() {
        final Map<Class<?>, PyBuiltinMethod[]> proxies = new HashMap<>();

        PyBuiltinMethodNarrow iterableProxy = new PyBuiltinMethodNarrow("__iter__") {
            @SuppressWarnings("unchecked")
            @Override
            public PyObject __call__() {
                return new JavaIterator(((Iterable<Object>) self.getJavaProxy()));
            }
        };
        proxies.put(Iterable.class, new PyBuiltinMethod[]{iterableProxy});

        PyBuiltinMethodNarrow lenProxy = new PyBuiltinMethodNarrow("__len__") {
            @Override
            public PyObject __call__() {
                return Py.newInteger(((Collection<?>) self.getJavaProxy()).size());
            }
        };
        PyBuiltinMethodNarrow containsProxy = new PyBuiltinMethodNarrow("__contains__", 1) {
            @Override
            public PyObject __call__(PyObject obj) {
                boolean contained = false;
                Object proxy = obj.getJavaProxy();
                if (proxy == null) {
                    for (Object item : (Collection<?>) self.getJavaProxy()) {
                        if (Py.java2py(item)._eq(obj).__nonzero__()) {
                            contained = true;
                            break;
                        }
                    }
                } else {
                    Object other = obj.__tojava__(Object.class);
                    contained = ((Collection<?>) self.getJavaProxy()).contains(other);

                }
                return contained ? Py.True : Py.False;
            }
        };
        proxies.put(Collection.class, new PyBuiltinMethod[]{lenProxy, containsProxy});

        PyBuiltinMethodNarrow iteratorProxy = new PyBuiltinMethodNarrow("__iter__") {
            @SuppressWarnings("unchecked")
            @Override
            public PyObject __call__() {
                return new JavaIterator(((Iterator<Object>) self.getJavaProxy()));
            }
        };
        proxies.put(Iterator.class, new PyBuiltinMethod[]{iteratorProxy});

        PyBuiltinMethodNarrow enumerationProxy = new PyBuiltinMethodNarrow("__iter__") {
            @SuppressWarnings("unchecked")
            @Override
            public PyObject __call__() {
                return new EnumerationIter(((Enumeration<Object>) self.getJavaProxy()));
            }
        };
        proxies.put(Enumeration.class, new PyBuiltinMethod[]{enumerationProxy});
        proxies.put(List.class, JavaProxyList.getProxyMethods());
        proxies.put(Map.class, JavaProxyMap.getProxyMethods());
        proxies.put(Set.class, JavaProxySet.getProxyMethods());
        return Collections.unmodifiableMap(proxies);
    }

    private static Map<Class<?>, PyBuiltinMethod[]> buildPostCollectionProxies() {
        final Map<Class<?>, PyBuiltinMethod[]> postProxies = new HashMap<>();
        postProxies.put(List.class, JavaProxyList.getPostProxyMethods());
        postProxies.put(Map.class, JavaProxyMap.getPostProxyMethods());
        postProxies.put(Set.class, JavaProxySet.getPostProxyMethods());
        return Collections.unmodifiableMap(postProxies);
    }

    private class ClassComparator implements Comparator<Class<?>>  {

        public int compare(Class<?> c1, Class<?> c2) {
            if (c1.equals(c2)) {
                return 0;
            } else if (c1.isAssignableFrom(c2)) {
                return -1;
            } else if (c2.isAssignableFrom(c1)) {
                return 1;
            }

            String s1 = hierarchyName(c1);
            String s2 = hierarchyName(c2);
            return s1.compareTo(s2);
        }

        private String hierarchyName(Class<?> c) {
            Stack<String> nameStack = new Stack<String>();
            StringBuilder namesBuilder = new StringBuilder();
            do {
                nameStack.push(c.getSimpleName());
                c = c.getSuperclass();
            } while (c != null);

            for (String name: nameStack) {
                namesBuilder.append(name);
            }

            return namesBuilder.toString();
        }
    }

    private class MethodComparator implements Comparator<Method> {

        private ClassComparator classComparator;

        public MethodComparator(ClassComparator classComparator) {
            this.classComparator = classComparator;
        }

        public int compare(Method m1, Method m2) {
            int result = m1.getName().compareTo(m2.getName());

            if (result != 0) {
                return result;
            }

            Class<?>[] p1 = m1.getParameterTypes();
            Class<?>[] p2 = m2.getParameterTypes();

            int n1 = p1.length;
            int n2 = p2.length;

            result = n1 - n2;

            if (result != 0) {
                return result;
            }

            result = classComparator.compare(m1.getDeclaringClass(), m2.getDeclaringClass());

            if (result != 0) {
                return result;
            }

            if (n1 == 0) {
                return classComparator.compare(m1.getReturnType(), m2.getReturnType());
            } else if (n1 == 1) {
                return classComparator.compare(p1[0], p2[0]);
            }
            return result;
        }
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        if (conflicted != null) {
            for (PyObject ob: conflicted) {
                if (ob != null) {
                    retVal = visit.visit(ob, arg);
                    if (retVal != 0) {
                        return retVal;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException {
        if (ob == null) {
            return false;
        }
        if (conflicted != null) {
            for (PyObject obj: conflicted) {
                if (obj == ob) {
                    return true;
                }
            }
        }
        return super.refersDirectlyTo(ob);
    }
}
