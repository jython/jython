// Copyright (c)2019 Jython Developers.
// Licensed to PSF under a Contributor Agreement.
package org.python.core;

import org.python.core.util.StringUtil;
import org.python.util.Generic;

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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class PyJavaType extends PyType {

    private final static Class<?>[] OO = {PyObject.class, PyObject.class};

    // @formatter:off
    /** Deprecated methods in java.awt.* that have bean property equivalents we prefer. */
    private final static Set<String> BAD_AWT_METHODS = Generic.set(
            "layout",
            "insets",
            "size",
            "minimumSize",
            "preferredSize",
            "maximumSize",
            "bounds",
            "enable");
    // @formatter:on
    /*
     * Add well-known immutable classes from standard packages of java.lang, java.net, java.util
     * that are not marked Cloneable. This was found by hand, there are likely more!
     */
    // @formatter:off
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
    // @formatter:on

    /**
     * Constant <code>PyJavaType</code>s used frequently. The inner class pattern ensures they can
     * be constructed before <code>PyJavaType</code> is initialised, and a unique instance of
     * type(Class) exists for {@link PyJavaType#PyJavaType(Class, boolean)} to use.
     */
    static class Constant extends PyType.Constant {

        // Note that constructing type(Class.class) does not register it in the type system.
        static final PyType CLASS = new PyJavaType(false);
        static final PyType OBJECT = fromClass(Object.class);
    }

    static {
        // Ensure that type(Class.class) is registered in the type system
        fromClass(Class.class);
    }

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
        PyObject obj = new PyObjectDerived(fromClass(o.getClass()));
        setProxyAttr(obj, o);
        return obj;
    }

    /**
     * Create a Python type for a Java class that does not descend from PyObject. (This will be
     * indicated by a null {@link PyType#underlying_class}.)
     */
    public PyJavaType() {
        super(Constant.PYTYPE);
    }

    /**
     * Create a Python type for a Java class that is not exposed, but may descend from
     * <code>PyObject</code>. This ancestry, implicit in the argument, makes a big difference when a
     * subsequent {@link #init(Set)} is called.
     * <p>
     * If the class descends from <code>PyObject</code>, it records the class it represents in
     * {@link PyType#underlying_class}. Otherwise, it records the class it represents in the
     * attribute {@link JyAttribute#JAVA_PROXY_ATTR}, and is said to be a proxy for it. In either
     * case {@link PyType#builtin} is <code>true</code>.
     *
     * @param c Java class for which this is a Python type.
     */
    PyJavaType(Class<?> c) {
        this(c, PyObject.class.isAssignableFrom(c));
    }

    /**
     * Create a Python <code>type</code> object for a given Java class that is not an exposed type,
     * but may still be a <code>PyObject</code>. This implements {@link #PyJavaType(Class)}, which
     * works it out from the class, but may be called directly when you already know which case
     * you're in.
     *
     * @param c Java class for which this is a Python type.
     * @param isPyObject true iff the class descends from PyObject
     */
    PyJavaType(Class<?> c, boolean isPyObject) {
        // A little messy as the super call must come first.
        super(isPyObject ? Constant.PYTYPE : Constant.CLASS, isPyObject ? c : null);
        // And if this is not for a PyObject :
        if (!isPyObject) {
            setProxyAttr(this, c);
        }
    }

    /**
     * Create the <code>PyJavaType</code> instance (proxy) for <code>java.lang.Class</code> itself.
     * The argument just exists to give the constructor a distinct signature. The
     * {@link #underlying_class} is <code>null</code>, and it is a built-in.
     */
    PyJavaType(boolean ignored) {
        // Use the special super constructor.
        super(true);
        setProxyAttr(this, Class.class);
    }

    @Override
    protected boolean useMetatypeFirst(PyObject attr) {
        return !(attr instanceof PyReflectedField || attr instanceof PyReflectedFunction
                || attr instanceof PyBeanEventProperty);
    }

    // Java types are ok with things being added and removed from their dicts as long as there isn't
    @Override
    void type___setattr__(String name, PyObject value) {
        PyObject field = lookup(name);
        // If we have a static field that takes this, go with that
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
            throw Py.NameError("attribute not found: " + name);
        }
        if (!field.jdontdel()) {
            object___delattr__(name);
        }
        if (modified != null) {
            modified.remove(name);
        }
        postDelattr(name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * An override specifically for Java classes (that are not {@link PyObject}) has the possibility
     * of completing the MRO in {@code mro}, by additional steps affecting the {@code mro} and
     * {@code toMerge} passed in. This divergence from the Python rules is acceptable for Java.
     */
    @Override
    void handleMroError(MROMergeState[] toMerge, List<PyObject> mro) {

        if (underlying_class != null) {
            // This descends from PyObject (but is not exposed): don't attempt recovery.
            super.handleMroError(toMerge, mro);
        }

        // Make a set of all the PyJavaTypes still in the lists to merge.
        Set<PyJavaType> inConflict = new LinkedHashSet<>();
        for (MROMergeState mergee : toMerge) {
            for (int i = mergee.next; i < mergee.mro.length; i++) {
                PyObject m = mergee.mro[i];
                if (m instanceof PyJavaType && m != Constant.OBJECT) {
                    inConflict.add((PyJavaType) m);
                }
            }
        }

        /*
         * Collect the names of all the methods added to any of these types (with certain
         * exclusions) that occur in more than one of these residual types. If a name is found in
         * more than one of these types, raise an error.
         */
        Set<String> allModified = new HashSet<>();
        for (PyJavaType type : inConflict) {
            if (type.modified != null) {
                // For every method name modified in type ...
                for (String method : type.modified) {
                    if (!allModified.add(method)) {
                        /*
                         * The method name was already in the set, so has appeared already. Work out
                         * which one that was by rescanning.
                         */
                        List<PyJavaType> types = new ArrayList<>();
                        Set<Class<?>> proxySet = new HashSet<>();
                        Class<?> proxyType = type.getProxyType();
                        for (PyJavaType othertype : inConflict) {
                            /*
                             * Ignore any pairings of types that are in a superclass/superinterface
                             * relationship with each other. This problem is a false positive that
                             * happens because of the automatic addition of methods so that Java
                             * classes behave more like their corresponding Python types, such as
                             * adding sort or remove. See http://bugs.jython.org/issue2445
                             */
                            if (othertype.modified != null && othertype.modified.contains(method)) {
                                Class<?> otherProxyType = othertype.getProxyType();
                                if (otherProxyType.isAssignableFrom(proxyType)) {
                                    continue;
                                } else if (proxyType.isAssignableFrom(otherProxyType)) {
                                    continue;
                                } else {
                                    types.add(othertype);
                                    proxySet.add(otherProxyType);
                                }
                            }
                        }

                        /*
                         * Need to special case __iter__ in certain circumstances to ignore the
                         * conflict in having duplicate __iter__ added (see getCollectionProxies),
                         * while still allowing each path on the inheritance hierarchy to get an
                         * __iter__. Annoying but necessary logic.
                         */
                        if (method.equals("__iter__")) {
                            if (Generic.set(Iterable.class, Map.class).containsAll(proxySet)) {
                                /*
                                 * Need to special case __iter__ in collections that implement both
                                 * Iterable and Map. See http://bugs.jython.org/issue1878
                                 */
                                continue;
                            } else if (Generic.set(Iterator.class, Enumeration.class)
                                    .containsAll(proxySet)) {
                                /*
                                 * Need to special case __iter__ in iterators that Iterator and
                                 * Enumeration. Annoying but necessary logic. See
                                 * http://bugs.jython.org/issue2445
                                 */
                                continue;
                            }
                        }

                        String fmt = "Supertypes that share a modified attribute "
                                + "have an MRO conflict[attribute=%s, supertypes=%s, type=%s]";
                        if (types.size() > 0) {
                            throw Py.TypeError(String.format(fmt, method, types, this.getName()));
                        }
                    }
                }
            }
        }

        /*
         * We can keep trucking, there aren't any existing method name conflicts. Mark the conflicts
         * in all the classes so further method additions can check for trouble.
         */
        for (PyJavaType type : inConflict) {
            for (PyJavaType otherType : inConflict) {
                if (otherType != type) {
                    if (type.conflicted == null) {
                        type.conflicted = Generic.set();
                    }
                    type.conflicted.add(otherType);
                }
            }
        }

        /*
         * Emit the first conflicting type we encountered to the MRO, and remove it from the working
         * lists. Forcing a step like this is ok for classes compiled from Java as the order of
         * bases is not significant as long as hierarchy is preserved.
         */
        PyJavaType winner = inConflict.iterator().next();
        mro.add(winner);
        for (MROMergeState mergee : toMerge) {
            mergee.removeFromUnmerged(winner);
        }

        // Restart the MRO generation algorithm from the current state.
        computeMro(toMerge, mro);
    }

    /**
     * Complete the initialisation of the <code>PyType</code> for non-exposed <code>PyObject</code>
     * or any other Java class for which we need a Python <code>type</code> object. This call will
     * fill {@link #dict}, {@link #name} and all other descriptive state using characteristics of
     * the class obtained by reflection.
     * <p>
     * Where the class is not a <code>PyObject</code> at all, the method registers all the "visible
     * bases" (according to Java) of the class represented, which will result in further calls to
     * register their visible bases in turn. In the process, this method, recursively, saves this
     * and the <code>PyJavaType</code>s of further non-PyObject bases into into
     * <code>needsInners</code>, so that these may be processed later.
     *
     * @param needsInners collects <code>PyJavaType</code>s that need further processing
     */
    @Override
    protected void init(Set<PyJavaType> needsInners) {

        // Get the class for which the type is to be initialised.
        Class<?> forClass = underlying_class != null ? underlying_class : getProxyType();
        name = forClass.getName();

        // Strip the java fully qualified class name from Py classes in core
        if (name.startsWith("org.python.core.Py")) {
            name = name.substring("org.python.core.Py".length()).toLowerCase();
        }
        dict = new PyStringMap();

        Class<?> baseClass = forClass.getSuperclass();

        if (underlying_class != null) {
            /*
             * Although not exposed, this is a subclass of PyObject, so it uses a simple linear mro
             * to PyObject that ignores interfaces.
             */
            computeLinearMro(baseClass);

        } else {
            /*
             * This is a wrapped Java type: fill in the mro first with the interfaces then the
             * superclass.
             */
            needsInners.add(this);
            LinkedList<PyObject> visibleBases = new LinkedList<>();
            for (Class<?> iface : forClass.getInterfaces()) {
                if (iface == PyProxy.class || iface == ClassDictInit.class) {
                    /*
                     * Don't show the interfaces added by proxy type construction; otherwise Python
                     * subclasses of proxy types and another Java interface can't make a consistent
                     * mro.
                     */
                    continue;
                }
                if (baseClass != null && iface.isAssignableFrom(baseClass)) {
                    /*
                     * Don't include redundant interfaces. If the redundant interface has methods
                     * that were combined with methods of the same name from other interfaces higher
                     * in the hierarchy, adding it here hides the forms from those interfaces.
                     */
                    continue;
                }
                visibleBases.add(fromClass(iface));
            }

            if (forClass == Object.class) {
                base = Constant.PYOBJECT;
            } else if (baseClass == null) {
                /*
                 * It would be most like Java to have no base (like PyNone) but in that case, the
                 * MRO calculation puts Object ahead of the interface. Our patching of Java
                 * container interfaces to behave like Python container objects requires the
                 * opposite.
                 */
                base = Constant.OBJECT;
            } else if (forClass == Class.class) {
                base = Constant.PYTYPE;
            } else {
                base = fromClass(baseClass);
            }

            if (baseClass == null) {
                // Object, an interface, a primitive or void: base goes last.
                visibleBases.add(base);
            } else {
                // forClass represents a (concrete or abstract) class: base comes before interfaces.
                visibleBases.push(base);
            }

            this.bases = visibleBases.toArray(new PyObject[visibleBases.size()]);
            mro = computeMro();
        }

        /*
         * PyReflected* can't call or access anything from non-public classes that aren't in
         * org.python.core
         */
        if (!isAccessibleClass(forClass) && !name.startsWith("org.python.core")) {
            handleSuperMethodArgCollisions(forClass);
            return;
        }

        /*
         * Compile lists of the methods, fields and constructors to be exposed through this type. If
         * we respect Java accessibility, this is simple.
         */
        Method[] methods;
        Field[] fields;
        Constructor<?>[] constructors;

        if (Options.respectJavaAccessibility) {
            // Just the public methods, fields and constructors
            methods = forClass.getMethods();
            fields = forClass.getFields();
            constructors = forClass.getConstructors();

        } else {
            // All methods on this class and all of its super classes
            List<Method> allMethods = Generic.list();
            for (Class<?> c = forClass; c != null; c = c.getSuperclass()) {
                for (Method meth : c.getDeclaredMethods()) {
                    allMethods.add(meth);
                    meth.setAccessible(true);
                }
            }
            methods = allMethods.toArray(new Method[allMethods.size()]);

            // All the fields on just this class
            fields = forClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }

            // All the constructors (except if this is for class Class)
            if (forClass == Class.class) {
                // No matter the security manager, cannot set constructors accessible
                constructors = forClass.getConstructors();
            } else {
                constructors = forClass.getDeclaredConstructors();
                for (Constructor<?> ctr : constructors) {
                    ctr.setAccessible(true);
                }
            }
        }

        // Methods must be in resolution order. See issue bjo #2391 for detail.
        Arrays.sort(methods, new MethodComparator(new ClassComparator()));

        // Add methods, also accumulating them in reflectedFuncs, and spotting Java Bean members.
        ArrayList<PyReflectedFunction> reflectedFuncs = new ArrayList<>(methods.length);
        Map<String, PyBeanProperty> props = Generic.map();
        Map<String, PyBeanEvent<?>> events = Generic.map();

        // First pass skip inherited (and certain "ignored") methods.
        addMethods(baseClass, reflectedFuncs, props, events, methods);
        // Add inherited and previously ignored methods
        addInheritedMethods(reflectedFuncs, methods);

        // Add fields declared on this type
        addFields(baseClass, fields);

        // Fill in the bean events and properties picked up while going through the methods
        addBeanEvents(events);
        addBeanProperties(props);

        // Add constructors declared on this type
        addConstructors(forClass, constructors);

        // Special handling for Java collection types
        addCollectionProxies(forClass);

        // Handle classes that use the ClassDictInit pattern for their definition
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

        // Fill in the __module__ attribute of PyReflectedFunctions.
        if (reflectedFuncs.size() > 0) {
            if (nameSpecified == null) {
                nameSpecified = Py.newString(name);
            }
            for (PyReflectedFunction func : reflectedFuncs) {
                func.__module__ = nameSpecified;
            }
        }

        // Handle descriptor classes
        if (baseClass != Object.class) {
            hasGet = getDescrMethod(forClass, "__get__", OO) != null
                    || getDescrMethod(forClass, "_doget", PyObject.class) != null
                    || getDescrMethod(forClass, "_doget", OO) != null;
            hasSet = getDescrMethod(forClass, "__set__", OO) != null
                    || getDescrMethod(forClass, "_doset", OO) != null;
            hasDelete = getDescrMethod(forClass, "__delete__", PyObject.class) != null
                    || getDescrMethod(forClass, "_dodel", PyObject.class) != null;
        }

        /*
         * Certain types get particular implementations of__lt__, __le__, __ge__, __gt__, __copy__
         * and __deepcopy__.
         */
        if (forClass == Object.class) {
            addMethodsForObject();
        } else if (forClass == Comparable.class) {
            addMethodsForComparable();
        } else if (forClass == Cloneable.class) {
            addMethodsForCloneable();
        } else if (forClass == Serializable.class) {
            addMethodsForSerializable();
        } else if (immutableClasses.contains(forClass)) {
            // __deepcopy__ just works for these objects since it uses serialization instead
            addMethod(new PyBuiltinMethodNarrow("__copy__") {

                @Override
                public PyObject __call__() {
                    return self;
                }
            });
        }
    }

    /**
     * A class containing a test that a given class is accessible to Jython, in the Modular Java
     * sense. It is in its own class simply to contain the state we need and its messy
     * initialisation.
     */
    private static class Modular {

        private static final Lookup LOOKUP = MethodHandles.lookup();

        /**
         * Test we need is constructed as a method handle, reflectively (so it works on Java less
         * than 9).
         */
        private static final MethodHandle accessibleMH;
        static {
            MethodHandle acc;
            try {
                Class<?> moduleClass = Class.forName("java.lang.Module");
                // mod = λ(c) c.getModule() : Class → Module
                MethodHandle mod = LOOKUP.findVirtual(Class.class, "getModule",
                        MethodType.methodType(moduleClass));
                // pkg = λ(c) c.getPackageName() : Class → String
                MethodHandle pkg = LOOKUP.findVirtual(Class.class, "getPackageName",
                        MethodType.methodType(String.class));
                // exps = λ(m, pn) m.isExported(pn) : Module, String → boolean
                MethodHandle exps = LOOKUP.findVirtual(moduleClass, "isExported",
                        MethodType.methodType(boolean.class, String.class));
                // expc = λ(m, c) exps(m, pkg(c)) : Module, Class → boolean
                MethodHandle expc = MethodHandles.filterArguments(exps, 1, pkg);
                // acc = λ(c) expc(mod(c), c) : Class → boolean
                acc = MethodHandles.foldArguments(expc, mod);
            } catch (ReflectiveOperationException | SecurityException e) {
                // Assume not a modular Java platform: acc = λ(c) true : Class → boolean
                acc = MethodHandles.dropArguments(MethodHandles.constant(boolean.class, true), 0,
                        Class.class);
            }
            accessibleMH = acc;
        }

        /**
         * Test whether a given class is in a package exported (accessible) to Jython, in the
         * Modular Java sense. Jython code is all in the unnamed module, so in fact we are testing
         * accessibility to the package of the class.
         *
         * If this means nothing on this platform (if the classes and methods needed are not found),
         * decide that we are not on a modular platform, in which case all invocations return
         * <code>true</code>.
         *
         * @param c the class
         * @return true iff accessible to Jython
         */
        static boolean accessible(Class<?> c) {
            try {
                return (boolean) accessibleMH.invokeExact(c);
            } catch (Throwable e) {
                return true;
            }
        }
    }

    /**
     * Test whether a given class is accessible, meaning it is in a package exported to Jython and
     * public (or we are ignoring accessibility).
     *
     * @param c the class
     * @return true iff accessible to Jython
     */
    static boolean isAccessibleClass(Class<?> c) {
        if (!Modular.accessible(c)) {
            return false;
        } else {
            return !Options.respectJavaAccessibility || Modifier.isPublic(c.getModifiers());
        }
    }

    /**
     * Add descriptors to this type's dictionary ({@code __dict__}) for methods defined on the
     * target class itself (the {@code fromClass}), where not inherited. One descriptor is created
     * for each simple name and a signature for every method with that simple name is added to the
     * descriptor. See also {@link #addInheritedMethods(List, Method[])}. This is exclusively a
     * helper method for {@link #init(Set)}.
     *
     * @param baseClass ancestor of the target class
     * @param reflectedFuncs to which reflected functions are added for further processing
     * @param props to which Java Bean properties are added for further processing
     * @param events to which Java Bean events are added for further processing
     * @param methods of the target class
     */
    private void addMethods(Class<?> baseClass, List<PyReflectedFunction> reflectedFuncs,
            Map<String, PyBeanProperty> props, Map<String, PyBeanEvent<?>> events,
            Method[] methods) {

        boolean isInAwt = name.startsWith("java.awt.") && name.indexOf('.', 9) == -1;

        for (Method meth : methods) {
            if (!declaredHere(baseClass, meth) || ignore(meth)) {
                continue;
            }

            String methname = meth.getName();

            /*
             * Special case a few troublesome methods in java.awt.*. These methods are all
             * deprecated and interfere too badly with bean properties to be tolerated. This is
             * totally a hack but a lot of code that uses java.awt will break without it.
             */
            if (isInAwt && BAD_AWT_METHODS.contains(methname)) {
                continue;
            }

            String nmethname = normalize(methname);

            PyReflectedFunction reflfunc = (PyReflectedFunction) dict.__finditem__(nmethname);
            if (reflfunc == null) {
                // A new descriptor is required
                reflfunc = new PyReflectedFunction(meth);
                reflectedFuncs.add(reflfunc);
                dict.__setitem__(nmethname, reflfunc);
            } else {
                // A descriptor for the same simple name exists: add a signature to it.
                reflfunc.addMethod(meth);
            }

            // Check if this is a Java Bean method, indicating the "bean nature" of the class
            checkBeanMethod(props, events, meth);
        }
    }

    /**
     * Consider whether the given method of the current class indicates the existence of a JavaBean
     * property or event.
     *
     * @param props in which to store properties we discover
     * @param events in which to store events we discover
     * @param meth under consideration
     */
    private void checkBeanMethod(Map<String, PyBeanProperty> props,
            Map<String, PyBeanEvent<?>> events, Method meth) {

        // If this is a bean method at all, it must be an instance method
        if (Modifier.isStatic(meth.getModifiers())) {
            return;
        }

        // First check if this is a bean event addition method
        int n = meth.getParameterTypes().length;
        String methname = meth.getName();
        if ((methname.startsWith("add") || methname.startsWith("set"))
                && methname.endsWith("Listener") && n == 1 && meth.getReturnType() == Void.TYPE
                && EventListener.class.isAssignableFrom(meth.getParameterTypes()[0])) {
            // Yes, we have discovered an event type. Save the information for later.
            Class<?> eventClass = meth.getParameterTypes()[0];
            String ename = eventClass.getName();
            int idot = ename.lastIndexOf('.');
            if (idot != -1) {
                ename = ename.substring(idot + 1);
            }
            ename = normalize(StringUtil.decapitalize(ename));
            events.put(ename, new PyBeanEvent<>(ename, eventClass, meth));
            return;
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
            // Ok, its a bean property. Save this information for later.
            beanPropertyName = normalize(StringUtil.decapitalize(beanPropertyName));
            PyBeanProperty prop = props.get(beanPropertyName);
            if (prop == null) {
                prop = new PyBeanProperty(beanPropertyName, null, null, null);
                props.put(beanPropertyName, prop);
            }
            // getX and setX should be getting and setting the same type of thing.
            if (get) {
                prop.getMethod = meth;
                prop.myType = meth.getReturnType();
            } else {
                prop.setMethod = meth;
                /*
                 * Needed for readonly properties. Getter will be used instead if there is one. Only
                 * works if setX method has exactly one param, which is the only reasonable case.
                 */
                // XXX: should we issue a warning if setX and getX have different types?
                if (prop.myType == null) {
                    Class<?>[] params = meth.getParameterTypes();
                    if (params.length == 1) {
                        prop.myType = params[0];
                    }
                }
            }
        }
    }

    /**
     * Add descriptors to this type's dictionary ({@code __dict__}) for methods inherited from
     * ancestor classes. This is exclusively a helper method for {@link #init(Set)}.
     * <p>
     * Python finds an inherited method by looking in the dictionaries of types along the MRO. This
     * is does not directly emulate the signature polymorphism of Java. Even though the entries of
     * the MRO include the {@code PyType}s of the Java ancestors of this class, each type's
     * dictionary is keyed on the simple name of the method. For the present class, and at the point
     * where this method is called, any method defined on this class has created a descriptor entry
     * for that method name (see {@link #addMethods(Class, List, Map, Map, Method[])}), but only for
     * the signatures defined directly in this class. If any method of the same simple name is
     * inherited in Java from a super-class, it is now shadowed by this entry as far as Python is
     * concerned. The purpose of this method is to add the shadowed method signatures.
     * <p>
     * For example {@code AbstractCollection<E>} defines {@code add(E)}, and {@code AbstractList<E>}
     * inherits it but also defines {@code add(int, E)} (giving control of the insertion point).
     * When {@link #addMethods(Class, List, Map, Map, Method[])} completes, the "add" descriptor in
     * {@code type(AbstractList)}, represents only {@code add(int, E)}, and we must add the
     * inherited signature for {@code add(E)}.
     *
     * @param reflectedFuncs to which reflected functions are added for further processing
     * @param methods of the target class
     */
    private void addInheritedMethods(List<PyReflectedFunction> reflectedFuncs, Method[] methods) {
        for (Method meth : methods) {
            String nmethname = normalize(meth.getName());
            PyReflectedFunction reflfunc = (PyReflectedFunction) dict.__finditem__(nmethname);
            if (reflfunc != null) {
                /*
                 * The superclass method has the same name as one declared on this class, so add the
                 * superclass version's arguments.
                 */
                reflfunc.addMethod(meth);
            } else if (PyReflectedFunction.isPackagedProtected(meth.getDeclaringClass())
                    && lookup(nmethname) == null) {
                /*
                 * This method must be a public method from a package protected superclass. It's
                 * visible from Java on this class, so do the same for Python here. This is the
                 * flipside of what handleSuperMethodArgCollisions does for inherited public methods
                 * on package protected classes.
                 */
                reflfunc = new PyReflectedFunction(meth);
                reflectedFuncs.add(reflfunc);
                dict.__setitem__(nmethname, reflfunc);
            }
        }
    }

    /**
     * Process the given fields defined on the target class (the <code>fromClass</code>).
     *
     * This is exclusively a helper method for {@link #init(Set)}.
     *
     *
     * @param baseClass
     * @param fields
     */
    private void addFields(Class<?> baseClass, Field[] fields) {
        for (Field field : fields) {
            if (!declaredHere(baseClass, field)) {
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
                        ((PyReflectedFunction) memb).__doc__ = doc instanceof PyString
                                ? (PyString) doc : new PyString(doc.toString());
                    }
                }
            }
            if (dict.__finditem__(normalize(fldname)) == null) {
                dict.__setitem__(normalize(fldname), new PyReflectedField(field));
            }
        }
    }

    /** Add the methods corresponding to a each discovered JavaBean event. */
    private void addBeanEvents(Map<String, PyBeanEvent<?>> events) {
        for (PyBeanEvent<?> ev : events.values()) {
            if (dict.__finditem__(ev.__name__) == null) {
                dict.__setitem__(ev.__name__, ev);
            }

            for (Method meth : ev.eventClass.getMethods()) {
                String methodName = meth.getName().intern();
                if (dict.__finditem__(methodName) != null) {
                    continue;
                }
                dict.__setitem__(methodName,
                        new PyBeanEventProperty(methodName, ev.eventClass, ev.addMethod, meth));
            }
        }
    }

    /** Add the methods corresponding to a each discovered JavaBean property. */
    private void addBeanProperties(Map<String, PyBeanProperty> props) {
        for (PyBeanProperty prop : props.values()) {

            // Check for an existing __dict__ entry with this name
            PyObject prev = dict.__finditem__(prop.__name__);
            if (prev != null) {
                if (!(prev instanceof PyReflectedField)
                        || !Modifier.isStatic(((PyReflectedField) prev).field.getModifiers())) {
                    // Any methods or non-static fields take precedence over the bean property
                    continue;
                } else {
                    // Must've been a static field, so add it to the property
                    prop.field = ((PyReflectedField) prev).field;
                }
            }

            /*
             * If one of our super-classes has something defined for this name, check if it's a bean
             * property, and if so, try to fill in any gaps in our property from there.
             */
            PyObject fromType[] = new PyObject[] {null};
            PyObject superForName = lookup_where_mro(prop.__name__, fromType);

            if (superForName instanceof PyBeanProperty) {
                PyBeanProperty superProp = ((PyBeanProperty) superForName);
                /*
                 * If it has a set method and we don't, take it regardless. If the types don't line
                 * up, it'll be rejected below.
                 */
                if (prop.setMethod == null) {
                    prop.setMethod = superProp.setMethod;
                } else if (prop.getMethod == null
                        && superProp.myType == prop.setMethod.getParameterTypes()[0]) {
                    /*
                     * Only take a get method if the type on it agrees with the set method we
                     * already have. The bean on this type overrides a conflicting one of the
                     * parent.
                     */
                    prop.getMethod = superProp.getMethod;
                    prop.myType = superProp.myType;
                }

                if (prop.field == null) {
                    // If the parent bean is hiding a static field, we need it as well.
                    prop.field = superProp.field;
                }

            } else if (superForName != null && fromType[0] != this
                    && !(superForName instanceof PyBeanEvent)) {
                /*
                 * There is already an entry for this name. It came from a type which is not @this;
                 * it came from a superclass. It is not a bean event. Do not override methods
                 * defined in superclass.
                 */
                continue;
            }

            /*
             * If the return types on the set and get methods for a property don't agree, the get
             * method takes precedence.
             */
            if (prop.getMethod != null && prop.setMethod != null
                    && prop.myType != prop.setMethod.getParameterTypes()[0]) {
                prop.setMethod = null;
            }
            dict.__setitem__(prop.__name__, prop);
        }
    }

    /**
     * Process the given constructors defined on the target class (the <code>fromClass</code>).
     *
     * This is exclusively a helper method for {@link #init(Set)}.
     *
     * @param forClass
     * @param constructors
     */
    private void addConstructors(Class<?> forClass, Constructor<?>[] constructors) {

        final PyReflectedConstructor reflctr = new PyReflectedConstructor(name);
        for (Constructor<?> ctr : constructors) {
            reflctr.addConstructor(ctr);
        }

        if (PyObject.class.isAssignableFrom(forClass)) {
            PyObject new_ = new PyNewWrapper(forClass, "__new__", -1, -1) {

                @Override
                public PyObject new_impl(boolean init, PyType subtype, PyObject[] args,
                        String[] keywords) {
                    return reflctr.make(args, keywords);
                }
            };
            dict.__setitem__("__new__", new_);
        } else {
            dict.__setitem__("__init__", reflctr);
        }
    }

    /**
     * Add Python methods corresponding to the API of common Java collection types. This is
     * exclusively a helper method for {@link #init(Set)}.
     *
     * @param forClass the target class
     */
    private void addCollectionProxies(Class<?> forClass) {
        PyBuiltinMethod[] collectionProxyMethods = getCollectionProxies().get(forClass);
        if (collectionProxyMethods != null) {
            for (PyBuiltinMethod meth : collectionProxyMethods) {
                addMethod(meth);
            }
        }

        // Allow for some methods to override the Java type's methods as a late injection
        for (Class<?> type : getPostCollectionProxies().keySet()) {
            if (type.isAssignableFrom(forClass)) {
                for (PyBuiltinMethod meth : getPostCollectionProxies().get(type)) {
                    addMethod(meth);
                }
            }
        }
    }

    /** Add special methods when this PyJavaType represents <code>Object</code>. */
    private void addMethodsForObject() {
        addMethod(new PyBuiltinMethodNarrow("__copy__") {

            @Override
            public PyObject __call__() {
                throw Py.TypeError(
                        "Could not copy Java object because it is not Cloneable or known to be immutable. "
                                + "Consider monkeypatching __copy__ for "
                                + self.getType().fastGetName());
            }
        });
        addMethod(new PyBuiltinMethodNarrow("__deepcopy__") {

            @Override
            public PyObject __call__(PyObject memo) {
                throw Py.TypeError("Could not deepcopy Java object because it is not Serializable. "
                        + "Consider monkeypatching __deepcopy__ for "
                        + self.getType().fastGetName());
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
                 * java.lang.Object.toString returns Unicode: preserve as a PyUnicode, then let the
                 * repr() built-in decide how to handle it. (Also applies to __str__.)
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

    /** Add special methods when this PyJavaType represents interface <code>Comparable</code>. */
    private void addMethodsForComparable() {
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

    /** Add special methods when this PyJavaType represents interface <code>Cloneable</code>. */
    private void addMethodsForCloneable() {
        addMethod(new PyBuiltinMethodNarrow("__copy__") {

            @Override
            public PyObject __call__() {
                Object obj = self.getJavaProxy();
                Method clone;
                /*
                 * TODO we could specialize so that for well known objects like collections. This
                 * would avoid needing to use reflection in the general case, because Object#clone
                 * is protected (but most subclasses are not). Lastly we can potentially cache the
                 * method handle in the proxy instead of looking it up each time
                 */
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

    /** Add special methods when this PyJavaType represents interface <code>Serializable</code>. */
    private void addMethodsForSerializable() {
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

    /*
     * cloneX, CloneOutput, CloneInput are verbatim from Eamonn McManus'
     * http://weblogs.java.net/blog/emcmanus/archive/2007/04/cloning_java_ob.html blog post on deep
     * cloning through serialization - just what we need for __deepcopy__ support of Java objects
     */
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
                throw new InvalidClassException("Classes desynchronized: " + "found " + found
                        + " when expecting " + expected);
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
     * Methods implemented in private, protected or package protected classes, and classes not
     * exported by their module, may not be called directly through reflection. This is discussed in
     * JDK issue <a href=https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4283544>4283544</a>,
     * and is not likely to change. They may however be called through the Method object of the
     * accessible class or interface that they implement. In non-reflective code, the Java compiler
     * would generate such a call, based on the type <em>declared</em> for the target, which must
     * therefore be accessible.
     * <p>
     * An MRO lookup on the actual class will find a <code>PyReflectedFunction</code> that defines
     * the method to Python. The normal process for creating that object will add the
     * <em>inaccessible</em> implementation method(s) to the list, not those of the public API. (A
     * class can of course have several methods with the same name and different signatures and the
     * <code>PyReflectedFunction</code> lists them all.) We must therefore take care to enter the
     * corresponding accessible API methods instead.
     * <p>
     * Prior to Jython 2.5, this was handled by setting methods in package protected classes
     * accessible which made them callable through reflection. That had the drawback of failing when
     * running in a security environment that didn't allow setting accessibility, and will fail on
     * the modular Java platform, so this method replaced it.
     *
     * @param forClass of which the methods are currently being defined
     */
    private void handleSuperMethodArgCollisions(Class<?> forClass) {
        for (Class<?> iface : forClass.getInterfaces()) {
            mergeMethods(iface);
        }
        Class<?> parent = forClass.getSuperclass();
        if (parent != null) {
            if (isAccessibleClass(parent)) {
                mergeMethods(parent);
            } else {
                // The parent class is also not public: go up one more in the ancestry.
                handleSuperMethodArgCollisions(parent);
            }
        }
    }

    /**
     * From a given class that is an ancestor of the Java class for which this PyJavaType is being
     * initialised, or that is an interface implemented by it or an ancestor, process each method in
     * the ancestor so that, if the class or interface that declares the method is public, that
     * method becomes a method of the Python type we are constructing.
     *
     * @param parent class or interface
     */
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
                /*
                 * Nothing in our supertype hierarchy defines something with this name, so it must
                 * not be visible there.
                 */
                continue;
            } else if (where[0] == this) {
                /*
                 * This method is the only thing defining items in this class' dict, so it must be a
                 * PyReflectedFunction created here. See if it needs the current method added to it.
                 */
                if (!((PyReflectedFunction) obj).handles(meth)) {
                    ((PyReflectedFunction) obj).addMethod(meth);
                }
            } else {
                /*
                 * There's something in a superclass with the same name. Add an item to this type's
                 * dict to hide it. If it's this method, nothing's changed. If it's a field, we want
                 * to make the method visible. If it's a different method, it'll be added to the
                 * reflected function created here in a later call.
                 */
                dict.__setitem__(nmethname, new PyReflectedFunction(meth));
            }
        }
    }

    /**
     * True iff the target of this <code>PyJavaType</code>'s target (the <code>forClass</code>)
     * declares the given member of this target. For this, the method is supplied the super-class of
     * the target.
     *
     * @param baseClass super-class of the target of this <code>PyJavaType</code>, or
     *            <code>null</code>.
     * @param member of the <code>forClass</code> that might be exposed on this
     *            <code>PyJavaType</code>
     * @return true if the member is declared here on the <code>fromClass</code>
     */
    private static boolean declaredHere(Class<?> baseClass, Member member) {
        if (baseClass == null) {
            return true;
        } else {
            Class<?> declaring = member.getDeclaringClass();
            return declaring != baseClass && baseClass.isAssignableFrom(declaring);
        }
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

    /** Recognise certain methods as ignored (tagged as <code>throws PyIgnoreMethodTag</code>). */
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

        @Override
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
                compare = ((Comparable<Object>) self.getJavaProxy()).compareTo(asjava);
            } catch (ClassCastException classCast) {
                return Py.NotImplemented;
            }
            return getResult(compare) ? Py.True : Py.False;
        }

        protected abstract boolean getResult(int comparison);
    }

    /*
     * Traverseproc-note: Usually we would have to traverse this class, but we can leave this out,
     * since CollectionProxies is only used locally in private static fields.
     */
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
        proxies.put(Iterable.class, new PyBuiltinMethod[] {iterableProxy});

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
        proxies.put(Collection.class, new PyBuiltinMethod[] {lenProxy, containsProxy});

        PyBuiltinMethodNarrow iteratorProxy = new PyBuiltinMethodNarrow("__iter__") {

            @SuppressWarnings("unchecked")
            @Override
            public PyObject __call__() {
                return new JavaIterator(((Iterator<Object>) self.getJavaProxy()));
            }
        };
        proxies.put(Iterator.class, new PyBuiltinMethod[] {iteratorProxy});

        PyBuiltinMethodNarrow enumerationProxy = new PyBuiltinMethodNarrow("__iter__") {

            @SuppressWarnings("unchecked")
            @Override
            public PyObject __call__() {
                return new EnumerationIter(((Enumeration<Object>) self.getJavaProxy()));
            }
        };
        proxies.put(Enumeration.class, new PyBuiltinMethod[] {enumerationProxy});
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

    private class ClassComparator implements Comparator<Class<?>> {

        @Override
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

            for (String name : nameStack) {
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

        @Override
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
            for (PyObject ob : conflicted) {
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
            for (PyObject obj : conflicted) {
                if (obj == ob) {
                    return true;
                }
            }
        }
        return super.refersDirectlyTo(ob);
    }
}
