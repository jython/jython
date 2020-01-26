/* Copyright (c) Jython Developers */
package org.python.core;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.python.antlr.ast.cmpopType;
import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.expose.TypeBuilder;
import org.python.modules._weakref.WeakrefModule;
import org.python.util.Generic;

/**
 * This class implements the Python <code>type</code> object and the static methods and data
 * structures that support the Python type system in Jython (the type registry).
 * <p>
 * The class <code>PyType</code> contains static data that describes Python types, that are
 * consulted and modified through its static API (notably {@link #fromClass(Class)}). The data
 * structures are guarded against modification by concurrent threads (or consultation while being
 * modified). They support construction of <code>type</code> objects that are visible to Python.
 * <p>
 * <b>Bootstrapping of the type system:</b> The first attempt to construct or get a
 * <code>PyObject</code> (or instance of a subclass of <code>PyObject</code>), to use the {@link Py}
 * class utilities, or to call {@link PyType#fromClass(Class)}, causes the Jython type system to be
 * initialised. By the time that call returns, the type system will be in working order: any
 * <code>PyType</code>s the application sees will be fully valid. Also, provided that the static
 * initialisation of the <code>PyObject</code> subclass in question is not obstructed for any other
 * reason, the instance returned will also be fully functional. Note that it is possible to refer to
 * <code>C.class</code>, and (if it is not an exposed type) to produce a <code>PyType</code> for it,
 * without causing the static initialisation of <code>C</code>.
 * <p>
 * This may be no less than the reader expected, but we mention it because, for classes encountered
 * during the bootstrapping of the type system, this guarantee is not offered. The (static)
 * initialisation of the type system is highly reentrant. Classes that are used by the type system
 * itself, and their instances, <b>do</b> encounter defective <code>PyType</code> objects. Instances
 * of these classes, which include mundane classes like <code>PyNone</code> and
 * <code>PyString</code>, may exist before their class is statically initialised in the JVM sense.
 * The type system has been implemented with this fact constantly in mind. The <code>PyType</code>
 * encountered is always the "right" one &mdash; the unique instance representing the Python type of
 * that class &mdash; but it may not be completely filled in. Debugging that enters these classes
 * during bootstrapping will take surprising turns. Changes to these classes should also take this
 * into account.
 */
/*
 * As a Java object, PyType and its intimate subclass PyJavaType are not strongly encapsulated,
 * internal structures are, on the whole, open to package access. A set of constructors is provided
 * for different types of target class that allow us to set builtin, underlying_class and the Java
 * proxy at construction time. PyType.fromClass, uses these constructors, and then calls init() to
 * build the rest of the PyType state. After these steps, the PyType should be effectively
 * immutable.
 *
 * type___new__ and PyType.newType use "blank" PyType constructors and do their own initialisation
 * of the instance data.
 */
@ExposedType(name = "type", doc = BuiltinDocs.type_doc)
public class PyType extends PyObject implements Serializable, Traverseproc {

    /**
     * Constants (singletons) for <code>PyType</code>s that we use repeatedly in the logic of
     * <code>PyType</code> and <code>PyJavaType</code>. This avoids repeated calls to
     * {@link PyType#fromClass(Class)}. The inner class pattern ensures they can be constructed
     * before <code>PyType</code> is initialised, and a unique instance of <code>type(type)</code>
     * exists for {@link PyType#PyType(Class)} to use.
     */
    protected static class Constant {

        // Identify an object to be type(type). Warning: not initialised until fromClass is called.
        static final PyType PYTYPE = new PyType(false);
        static final PyType PYOBJECT = fromClass(PyObject.class);
        static final PyType PYSTRING = fromClass(PyString.class);
    }

    /** The <code>PyType</code> of <code>PyType</code> (or <code>type(type)</code>). */
    public static final PyType TYPE = fromClass(PyType.class);

    /**
     * The type's name. builtin types include their fully qualified name, e.g.: time.struct_time.
     */
    protected String name;

    /** __base__, the direct base type or null. */
    protected PyType base;

    /** __bases__, the base classes. */
    protected PyObject[] bases = Registry.EMPTY_PYOBJECT_ARRAY;

    /** The real, internal __dict__. */
    protected PyObject dict;

    /** __mro__, the method resolution. order */
    protected PyObject[] mro;

    /** __flags__, the type's options. */
    private long tp_flags;

    /**
     * The Java Class that instances of this type represent (when that is a <code>PyObject</code>),
     * or <code>null</code> if the type is not a <code>PyObject</code>, in which case the class is
     * indicated through a {@link JyAttribute#JAVA_PROXY_ATTR}.
     */
    protected Class<?> underlying_class;

    /** Whether this is a builtin type. */
    protected boolean builtin;

    /** Whether new instances of this type can be instantiated */
    protected boolean instantiable = true;

    /** Whether this type implements descriptor __get/set/delete__ methods. */
    boolean hasGet;
    boolean hasSet;
    boolean hasDelete;

    /** Whether this type allows subclassing. */
    private boolean isBaseType = true;

    /** Whether this type has a __dict__. */
    protected boolean needs_userdict;

    /** Whether this type has a __weakref__ slot (however all types are weakrefable). */
    protected boolean needs_weakref;

    /** Whether finalization is required for this type's instances (implements __del__). */
    protected boolean needs_finalizer;

    /** Whether this type's __getattribute__ is object.__getattribute__. */
    private volatile boolean usesObjectGetattribute;

    /** MethodCacheEntry version tag. */
    private volatile Object versionTag = new Object();

    /** The number of __slots__ defined by this type + bases. */
    private int numSlots;

    /** The number of __slots__ defined by this type itself. */
    private int ownSlots = 0;

    private transient ReferenceQueue<PyType> subclasses_refq = new ReferenceQueue<PyType>();
    private Set<WeakReference<PyType>> subclasses = Generic.linkedHashSet();

    /** Brevity for <code>JyAttribute.hasAttr(obj, JyAttribute.JAVA_PROXY_ATTR)</code>. */
    static boolean hasProxyAttr(PyObject obj) {
        return JyAttribute.hasAttr(obj, JyAttribute.JAVA_PROXY_ATTR);
    }

    /** Brevity for <code>JyAttribute.getAttr(obj, JyAttribute.JAVA_PROXY_ATTR)</code>. */
    static Object getProxyAttr(PyObject obj) {
        return JyAttribute.getAttr(obj, JyAttribute.JAVA_PROXY_ATTR);
    }

    /** Brevity for <code>JyAttribute.setAttr(obj, JyAttribute.JAVA_PROXY_ATTR, value)</code>. */
    static void setProxyAttr(PyObject obj, Object value) {
        JyAttribute.setAttr(obj, JyAttribute.JAVA_PROXY_ATTR, value);
    }

    /**
     * The class <code>PyType</code> contains a registry that describes Python types through a
     * system of indexes and <code>PyType</code> objects. Each <code>PyObject</code> calls
     * {@link PyType#fromClass(Class)}, which adds to this data, as it is being initialised. When
     * the first descriptor is created, as the first exposed type is initialised, the registry has
     * to be in a consistent, working state.
     * <p>
     * <code>PyType</code> itself is a <code>PyObject</code>. In order to guarantee that the
     * registry exists when we need it, we use a separate class whose initialisation is not hostage
     * to the type system itself, as it would be it it were static data in <code>PyType</code>. See
     * the classic
     * "<a href = "http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html#dcl">The
     * double-checked locking problem</a>", which we use here to be ready rather than lazy.
     * <p>
     * A further requirement is that the registry data structures be guarded against concurrent
     * access. This class also contains the methods that manipulate the state, and they synchronise
     * access to it on behalf of <code>PyType</code>.
     */
    private static final class Registry {

        /** Mapping of Java classes to their PyTypes, under construction. */
        private static final Map<Class<?>, PyType> classToNewType = new IdentityHashMap<>();

        /** Mapping of Java classes to their TypeBuilders, until these are used. */
        private static final Map<Class<?>, TypeBuilder> classToBuilder = new HashMap<>();

        /** Acts as a non-blocking cache for PyType look-up. */
        static ClassValue<PyType> classToType = new ClassValue<PyType>() {

            @Override
            protected PyType computeValue(Class<?> c) throws IncompleteType {
                synchronized (Registry.class) {
                    // Competing threads will block and this thread will win the return.
                    return resolveType(c);
                }
            }
        };

        /**
         * An exception used internally to signal a result from {@code classToType.get()} when the
         * type is still under construction.
         */
        private static class IncompleteType extends RuntimeException {

            /** The incompletely constructed {@code PyType}. */
            final PyType type;

            IncompleteType(PyType type) {
                this.type = type;
            }
        }

        /**
         * Mapping of Java classes to their PyTypes that have been thrown as {@link IncompleteType}
         * exceptions. That action causes the {@code computeValue()} in progress to be re-tried, so
         * we have to have the answer ready.
         */
        private static final Map<Class<?>, PyType> classToThrownType = new IdentityHashMap<>();

        /**
         * A list of <code>PyObject</code> sub-classes, instances of which are used in the type
         * system itself. We require to reference their <code>PyType</code>s before Java static
         * initialisation can produce a builder, because that initialisation itself depends on
         * <code>PyType</code>s of classes in this list, circularly. These classes must, however, be
         * Java initialised and have complete <code>PyType</code>s by the time the static
         * initialisation of <code>PyObject</code> completes.
         */
        // @formatter:off
        private static final Class<?>[] BOOTSTRAP_TYPES = {
                    PyObject.class,
                    PyType.class,
                    PyBuiltinCallable.class,
                    PyDataDescr.class,
                    PyString.class,
                };
        // @formatter:on

        /**
         * The set of classes that we should not, at the time we create their <code>PyType</code>,
         * try to Java-initialise if they do not have a builder. The PyType for such a class is
         * partially initialised, and needs to be completed when a builder appears. Classes come off
         * this list when fully initialised, and for bootstrap types, we'll check that in the static
         * initialisation of PyObject.
         */
        static final Set<Class<?>> deferredInit = new HashSet<>(Arrays.asList(BOOTSTRAP_TYPES));

        /**
         * True if the current {@link #resolveType(Class)} call is a top-level call; false if
         * {@link #resolveType(Class)} is called reentrantly, while attempting to type some other
         * class.
         */
        private static int depth = 0;

        /** Java types awaiting processing of their inner classes. */
        private static Set<PyJavaType> needsInners = new HashSet<>();

        /** Handy constant: zero-length array. */
        static final PyObject[] EMPTY_PYOBJECT_ARRAY = {};

        /**
         * Test whether a given class is an exposed <code>PyObject</code>. Amongst other things,
         * this may be used to decide that <code>type(c)</code> should be created as a
         * <code>PyType</code> instead of a <code>PyJavaType</code>.
         *
         * @param c class to test
         * @return whether an exposed type
         */
        private static boolean isExposed(Class<?> c) {
            if (c.getAnnotation(ExposedType.class) != null) {
                return true;
            } else {
                return ExposeAsSuperclass.class.isAssignableFrom(c);
            }
        }

        /**
         * Return the <code>PyType</code> for the given target Java class. During the execution of
         * this method, {@link #classToNewType} will hold any incompletely constructed
         * {@link PyType}s, and a second attempt to resolve the same class will throw an
         * {@link IncompleteType}, holding the incomplete {@code PyType}. This supports
         * {@link PyType#fromClass(Class)} in the case where a <code>PyType</code> has not already
         * been published. See there for caveats. If the making of a type (it will be a
         * <code>PyJavaType</code>) might require the processing of inner classes,
         * {@link PyType#fromClass(Class)} is called recursively for each.
         * <p>
         * The caller guarantees that this thread holds the lock on the registry.
         *
         * @param c for which a PyType is to be created
         * @throws IncompleteType to signal an incompletely constructed result
         */
        static PyType resolveType(Class<?> c) throws IncompleteType {

            // log("resolve", c);
            PyType type = classToNewType.get(c);

            if (type != null) {
                /*
                 * The type for c is still under construction, so we cannot return it normally,
                 * which would publish it immediately to other threads. The client must be our
                 * thread, calling re-entrantly, so we sneak it a reference in an exception, and
                 * remember we have done this.
                 */
                classToThrownType.put(c, type);
                // log("> ", type);
                throw new IncompleteType(type);

            } else if ((type = classToThrownType.remove(c)) != null) {
                /*
                 * The type for c has been fully constructed, but was ignored because an exception
                 * was raised between the call to computeValue() and the return. .
                 */

            } else if (depth > 0) {
                /*
                 * This is a nested call about a class c we haven't seen before. Create (or choose
                 * an existing) type for c.
                 */
                depth += 1;
                addFromClass(c);
                depth -= 1;
                type = classToNewType.remove(c);

            } else {
                /*
                 * This is a top-level call about a class c we haven't seen before. Create (or
                 * choose an existing) type for c. (In rare circumstances a thread that saw the
                 * cache miss will arrive here after some other thread populates classToNewType. The
                 * duplicate will be discarded, but the implementation must ensure this is harmless.
                 */
                assert needsInners.isEmpty();

                try {
                    // Signal to further invocations that they are nested.
                    depth = 1;

                    // Create (or choose an existing) type for c, accumulating classes in
                    // needsInners.
                    addFromClass(c);

                    // Process inner classes too, if necessary. (This invalidates needsInners.)
                    if (!needsInners.isEmpty()) {
                        processInners();
                    }
                } finally {
                    // Guarantee subsequent calls are top-level and needsInners is empty.
                    depth = 0;
                    needsInners.clear();
                }
                type = classToNewType.remove(c);
            }

            /*
             * Return the PyType we made for c, which is now complete. (We may still be part way
             * through making others.)
             */
            // log("+-->", type);
            return type;
        }

        /**
         * Make the <code>PyType</code> of each inner class of the classes in {@link #needsInners}
         * into an attribute of its Python representation (except where shadowed). This will
         * normally mean creating a <code>PyType</code> for each inner class.
         * <p>
         * Calling this method sets {@link #topLevel} <code> = true</code> and may replace the
         * contents of {@link #needsInners}.
         */
        private static void processInners() {

            // Take a copy for iteration since needsInners gets modified in the loop.
            PyJavaType[] ni = needsInners.toArray(new PyJavaType[needsInners.size()]);

            // Ensure calls to fromClass are top-level.
            depth = 0;
            needsInners.clear();

            for (PyJavaType javaType : ni) {

                Class<?> forClass = javaType.getProxyType();

                for (Class<?> inner : forClass.getClasses()) {
                    /*
                     * Only add the class if there isn't something else with that name and it came
                     * from this class.
                     */
                    if (inner.getDeclaringClass() == forClass
                            && javaType.dict.__finditem__(inner.getSimpleName()) == null) {
                        // This call is a top-level fromClass and destroys needsInners.
                        PyType innerType = fromClass(inner);
                        javaType.dict.__setitem__(inner.getSimpleName(), innerType);
                    }
                }
            }
        }

        /**
         * Add a new <code>PyType</code> or {@link PyJavaType} entry for the given class to
         * {@link #classToNewType}, respecting the {@link ExposeAsSuperclass} marker interface. It
         * is known at the point this method is called that the class is not already registered.
         * <p>
         * The caller guarantees that this thread holds the lock on the registry.
         *
         * @param c for which a PyType is to be created in {@link #classToNewType}
         */
        private static void addFromClass(Class<?> c) {
            if (ExposeAsSuperclass.class.isAssignableFrom(c)) {
                // Expose c with the Python type of its superclass, recursively as necessary.
                PyType exposedAs = fromClass(c.getSuperclass());
                classToNewType.put(c, exposedAs);
            } else {
                // Create and add a new Python type for this Java class
                createType(c);
            }
        }

        /**
         * Create a new <code>PyType</code> or {@link PyJavaType} for the given class in
         * {@link #classToNewType}. It is known at the point this method is called that the class is
         * not already registered.
         * <p>
         * The caller guarantees that this thread holds the lock on the registry.
         *
         * @param c for which a PyType is to be added to {@link #classToNewType}
         */
        private static void createType(Class<?> c) {

            PyType newtype;

            if (PyObject.class.isAssignableFrom(c)) {
                // c is a PyObject
                if (isExposed(c)) {
                    // c is an exposed type (therefore should be a PyObject of some kind).
                    if (c != PyType.class) {
                        newtype = new PyType(c);
                    } else {
                        // The one PyType that PyType(Class) can't make.
                        newtype = Constant.PYTYPE;
                    }
                } else {
                    // c is a non-exposed PyObject: expose reflectively.
                    newtype = new PyJavaType(c, true);
                }

            } else {
                // c is not a PyObject: expose reflectively via a proxy.
                if (c != Class.class) {
                    newtype = new PyJavaType(c, false);
                } else {
                    // The proxy that PyJavaType(Class, boolean) can't make.
                    newtype = PyJavaType.Constant.CLASS;
                }
            }

            // Enter the new PyType in the registry
            classToNewType.put(c, newtype);

            /*
             * This is new to the registry, and we must initialise it. Note that the if c is a
             * bootstrap type init() is called, but cut short, filling only the MRO. The work will
             * be completed later as a side effect of addBuilder(). Until bootstrap types are all
             * fully initialised, the type system demands care in use.
             */
            newtype.init(needsInners);
            newtype.invalidateMethodCache();
        }

        /**
         * Register the {@link TypeBuilder} for the given class. This only really makes sense for a
         * <code>PyObject</code>. Initialising a properly-formed PyObject will usually result in a
         * call to <code>addBuilder</code>, thanks to code inserted by the Jython type exposer.
         *
         * @param c class for which this is the builder
         * @param builder to register
         */
        static synchronized void addBuilder(Class<?> c, TypeBuilder builder) {
            /*
             * If c is not registered in the type system (e.g. c is being JVM-init'd by a "use" of
             * c), the next fromClass will create, register, and fully init() type(c), because we're
             * giving it a builder. In that case, we'll be done.
             */
            classToBuilder.put(c, builder);
            PyType type = fromClass(c);
            /*
             * The PyTypes of bootstrap classes may be registered "half-initialised", until a
             * builder is available. They end up on the deferred list.
             */
            if (deferredInit.remove(c)) {
                /*
                 * c was on the deferred list, so it was registered without a builder, and type(c)
                 * was "half-initialised". We need to complete the initialisation.
                 */
                type.init(null);
            }
        }

        /**
         * Force static initialisation of the given sub-class of <code>PyObject</code> in order to
         * supply a builder (via {@link #addBuilder(Class, TypeBuilder)}, if the class wants to. If
         * this is a class we haven't seen before, many reentrant calls to
         * {@link PyType#fromClass(Class)} are produced here, for the descriptor classes that expose
         * methods and attributes.
         *
         * @param c target class
         */
        static synchronized void staticJavaInit(Class<?> c) throws SecurityException {
            try {
                // Call Class.forName to force static initialisation.
                Class.forName(c.getName(), true, c.getClassLoader());
            } catch (ClassNotFoundException e) {
                // Well, this is certainly surprising.
                String msg = "Got ClassNotFound calling Class.forName on a class already found ";
                throw new RuntimeException(msg, e);
            } catch (ExceptionInInitializerError e) {
                throw Py.JavaError(e);
            }
        }

        /**
         * For each class listed as a bootstrap type, try to make sure it fully initialised a
         * <code>PyType</code>.
         *
         * @return classes that did not completely initialise
         */
        static synchronized Set<Class<?>> bootstrap() {
            Set<Class<?>> missing = new HashSet<>();
            for (Class<?> c : BOOTSTRAP_TYPES) {
                // Look-up should force at least first-stage initialisation (if not done before).
                PyType type = fromClass(c);
                if (deferredInit.contains(c)) {
                    // Only the first stage happened: encourage the rest to happen.
                    staticJavaInit(c);
                }
                // Check various symptoms
                if (type.name == null || deferredInit.contains(c)) {
                    missing.add(c);
                }
            }
            return missing;
        }

        // -------- Debugging for the registry --------
        private static void log(String where, Class<?> c) {
            String name = abbr(c.getName());
            System.err.printf("%s%s: %s %s thr=%s\n", pad(), where, name, names(classToNewType),
                    names(classToThrownType));
            // logger.log(Level.INFO, "{0}{1}: {2}", new Object[] {pad, where, name});
        }

        private static void log(String kind, PyType result) {
            String r = result.toString();
            System.err.printf("%s%s %s %s thr=%s\n", pad(), kind, r, names(classToNewType),
                    names(classToThrownType));
        }

        /** For logging formatting. */
        private static final String PAD = "                              ";

        private static String abbr(String name) {
            return name.replace("java.lang.", "j.l.").replace("org.python.core.", "");
        }

        private static String pad() {
            int d = Math.min(Math.max(2 * depth, 0), PAD.length());
            return PAD.substring(0, d);
        }

        private static List<String> names(Map<Class<?>, PyType> map) {
            ArrayList<String> names = new ArrayList<>(map.size());
            for (Class<?> k : map.keySet()) {
                names.add(abbr(k.getName()));
            }
            return names;
        }
    }

    /**
     * Create a "blank" <code>PyType</code> instance, for a Python subclass of <code>type</code>,
     * that is, for a Python metatype. The {@link #underlying_class} is <code>null</code> and
     * {@link #builtin} is <code>false</code>. This form is used by <code>PyTypeDerived</code>.
     *
     * @param subtype Python subclass of this object
     */
    protected PyType(PyType subtype) {
        super(subtype);
    }

    /**
     * Create a "blank" <code>PyType</code> instance. The {@link #underlying_class} is
     * <code>null</code> and it is a {@link #builtin} is <code>false</code>. This form is used in
     * {@link #newType(PyNewWrapper, PyType, String, PyTuple, PyObject)}, which takes responsibility
     * for filling in the other fields.
     */
    private PyType() {}

    /**
     * Create the <code>PyType</code> instance for <code>type</code> itself, or for a subclass of
     * it. Depending upon the argument, the {@link #underlying_class} is either this class, or
     * <code>null</code>, indicating a proxy. Either way, {@link #builtin} is <code>true</code>. In
     * practice this is used to create the <code>PyType</code>s for <code>PyType</code> itself and
     * for <code>java.lang.Class</code>.
     *
     * @param isProxy if true, this is a proxy
     */
    protected PyType(boolean isProxy) {
        super(false);
        builtin = true;
        underlying_class = isProxy ? null : this.getClass();
    }

    /**
     * As {@link #PyType(Class)}, but also specifying the sub-type of Python type for which it is
     * created.
     */
    protected PyType(PyType subtype, Class<?> c) {
        super(subtype);
        builtin = true;
        underlying_class = c;
    }

    /**
     * Create a built-in type for the given Java class. This is the class to be returned by
     * <code>__tojava__(Object.class)</code> (see {@link #__tojava__(Class)}), and by name in the
     * string representation of the type.
     *
     * @param c the underlying class or <code>null</code>.
     */
    protected PyType(Class<?> c) {
        // PyType.TYPE is the object type, but that may still be null so use the "advance copy".
        this(Constant.PYTYPE, c);
    }

    @ExposedNew
    static final PyObject type___new__(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        // Special case: type(x) should return x.getType()
        if (args.length == 1 && keywords.length == 0) {
            PyObject obj = args[0];
            PyType objType = obj.getType();

            // special case for PyStringMap so that it types as a dict
            PyType psmType = fromClass(PyStringMap.class);
            if (objType == psmType) {
                return PyDictionary.TYPE;
            }
            return objType;
        }
        /*
         * If that didn't trigger, we need 3 arguments. but ArgParser below may give a msg saying
         * type() needs exactly 3.
         */
        if (args.length + keywords.length != 3) {
            throw Py.TypeError("type() takes 1 or 3 arguments");
        }

        ArgParser ap = new ArgParser("type()", args, keywords, "name", "bases", "dict");
        String name = ap.getString(0);
        PyTuple bases = (PyTuple) ap.getPyObjectByType(1, PyTuple.TYPE);
        PyObject dict = ap.getPyObject(2);
        if (!(dict instanceof AbstractDict)) {
            throw Py.TypeError("type(): argument 3 must be dict, not " + dict.getType());
        }
        return newType(new_, subtype, name, bases, dict);
    }

    @ExposedMethod(doc = BuiltinDocs.type___init___doc)
    final void type___init__(PyObject[] args, String[] kwds) {
        if (kwds.length > 0) {
            throw Py.TypeError("type.__init__() takes no keyword arguments");
        }

        if (args.length != 1 && args.length != 3) {
            throw Py.TypeError("type.__init__() takes 1 or 3 arguments");
        }
        object___init__(Py.EmptyObjects, Py.NoKeywords);
    }

    public static PyObject newType(PyNewWrapper new_, PyType metatype, String name, PyTuple bases,
            PyObject dict) {
        PyObject[] tmpBases = bases.getArray();
        PyType winner = findMostDerivedMetatype(tmpBases, metatype);

        if (winner != metatype) {
            PyObject winnerNew = winner.lookup("__new__");
            if (winnerNew != null && winnerNew != new_) {
                return invokeNew(winnerNew, winner, false,
                        new PyObject[] {new PyString(name), bases, dict}, Py.NoKeywords);
            }
            metatype = winner;
        }
        /*
         * Use PyType as the metaclass for Python subclasses of Java classes rather than PyJavaType.
         * Using PyJavaType as metaclass exposes the java.lang.Object methods on the type, which
         * doesn't make sense for python subclasses.
         */
        if (metatype == PyJavaType.Constant.CLASS) {
            metatype = TYPE;
        }

        PyType type;
        if (new_.for_type == metatype) {
            // XXX: set metatype
            type = new PyType();
        } else {
            type = new PyTypeDerived(metatype);
        }

        dict = ((AbstractDict) dict).copy();
        type.name = name;
        type.bases = tmpBases.length == 0 ? new PyObject[] {PyObject.TYPE} : tmpBases;
        type.dict = dict;
        type.tp_flags = Py.TPFLAGS_HEAPTYPE | Py.TPFLAGS_BASETYPE;
        // Enable defining a custom __dict__ via a property, method, or other descriptor
        boolean defines_dict = dict.__finditem__("__dict__") != null;

        // immediately setup the javaProxy if applicable. may modify bases
        List<Class<?>> interfaces = Generic.list();
        Class<?> baseProxyClass = getJavaLayout(type.bases, interfaces);
        type.setupProxy(baseProxyClass, interfaces);

        PyType base = type.base = best_base(type.bases);
        if (!base.isBaseType) {
            throw Py.TypeError(
                    String.format("type '%.100s' is not an acceptable base type", base.name));
        }

        type.createAllSlots(!(base.needs_userdict || defines_dict), !base.needs_weakref);
        type.ensureAttributes();
        type.invalidateMethodCache();

        for (PyObject cur : type.bases) {
            if (cur instanceof PyType) {
                ((PyType) cur).attachSubclass(type);
            }
        }

        return type;
    }

    /**
     * Used internally by {@link #createAllSlots()}. Builds a naive pseudo mro used to collect all
     * slot names relevant for this type.
     *
     * @param tp type to be investigated
     * @param dest list collecting all ancestors
     * @param slotsMap map linking each type to its slots
     * @return position of first ancestor that is not equal to or ancestor of primary base
     */
    private static int findSlottedAncestors(PyType tp, List<PyType> dest,
            Map<PyType, PyObject> slotsMap) {
        int baseEnd = 0;
        if (tp.base != null && tp.base.numSlots > 0 && !slotsMap.containsKey(tp.base)) {
            findSlottedAncestors(tp.base, dest, slotsMap);
        }
        baseEnd = dest.size();
        PyObject slots = tp.dict.__finditem__("__slots__");
        if (slots != null) {
            dest.add(tp); // to keep track of order
            slotsMap.put(tp, slots);
        }
        if (tp.bases.length > 1) {
            for (PyObject base : tp.bases) {
                if (base == tp.base || !(base instanceof PyType) || ((PyType) base).numSlots == 0
                        || slotsMap.containsKey((PyType) base)) {
                    continue;
                }
                findSlottedAncestors((PyType) base, dest, slotsMap);
            }
        }
        return baseEnd;
    }

    /**
     * Used internally by {@link #createAllSlots()}. Adds all names in {@code slots} to
     * {@code dest}.
     *
     * @param slots names to be added as slots
     * @param dest set collecting all slots
     */
    private static void insertSlots(PyObject slots, Set<String> dest) {
        if (slots instanceof PyString) {
            slots = new PyTuple(slots);
        }
        // Check for valid slot names and create them.
        for (PyObject slot : slots.asIterable()) {
            String slotName = confirmIdentifier(slot);
            if (slotName.equals("__dict__") || slotName.equals("__weakref__")) {
                continue;
            }
            dest.add(slotName);
        }
    }

    /**
     * Create all slots and related descriptors.
     *
     * @param mayAddDict whether a __dict__ descriptor is allowed on this type
     * @param mayAddWeak whether a __weakref__ descriptor is allowed on this type
     */
    private void createAllSlots(boolean mayAddDict, boolean mayAddWeak) {
        List<PyType> slottedAncestors = Generic.list(base.mro.length + (bases.length - 1) * 3 + 1);
        Map<PyType, PyObject> slotsMap = Generic.identityHashMap(slottedAncestors.size());
        /*
         * Here we would need the mro to search for slots (also in secondary bases) properly, but
         * mro hasn't been set up yet. So we quickly (?) build a pseudo mro sufficient to find all
         * slots.
         */
        int baseEnd = findSlottedAncestors(this, slottedAncestors, slotsMap);
        // baseEnd is the first position of an ancestor not equal to or ancestor of primary base
        int slots_tmp = 0; // used for various purpose, first to accumulate maximal slot count
        for (PyType anc : slottedAncestors) {
            slots_tmp += anc.numSlots;
        }
        /*
         * In allSlots we collect slots of primary base first, then of this type, then of secondary
         * bases. At any time we prevent it from containing __dict__ or __weakref__. we know the
         * required capacity, so the set likely won't be resized.
         */
        Set<String> allSlots = Generic.linkedHashSet(2 * slots_tmp);
        if (baseEnd > 0) {
            for (int i = 0; i < baseEnd; ++i) {
                insertSlots(slotsMap.get(slottedAncestors.get(i)), allSlots);
            }
        }
        assert allSlots.size() == base.numSlots;

        boolean wantDict = false;
        boolean wantWeak = false;
        PyObject slots = dict.__finditem__("__slots__");
        ownSlots = 0; // to keep track of slots defined by this type itself for isSolidBase
        /*
         * from now on, slots_tmp stores position where other ancestors than primary base begin
         * (points to this type if it defines own slots)
         */
        if (slots == null) {
            wantDict = mayAddDict;
            wantWeak = mayAddWeak;
            slots_tmp = baseEnd;
        } else {
            if (slots instanceof PyString) {
                slots = new PyTuple(slots);
            }
            // Check for valid slot names and create them. Handle two special cases
            for (PyObject slot : slots.asIterable()) {
                String slotName = confirmIdentifier(slot);

                if (slotName.equals("__dict__")) {
                    if (!mayAddDict || wantDict) {
                        /*
                         * CPython is stricter here, but this seems arbitrary. To reproduce CPython
                         * behavior:
                         */
                        // if (base != PyObject.TYPE) {
                        // throw Py.TypeError("__dict__ slot disallowed: we already got one");
                        // }
                    } else {
                        wantDict = true;
                        continue;
                    }
                } else if (slotName.equals("__weakref__")) {
                    if ((!mayAddWeak || wantWeak) && base != PyObject.TYPE) {
                        /*
                         * CPython is stricter here, but this seems arbitrary. To reproduce CPython
                         * behavior:
                         */
                        // if (base != PyObject.TYPE) {
                        // throw Py.TypeError("__weakref__ slot disallowed: we already got one");
                        // }
                    } else {
                        wantWeak = true;
                        continue;
                    }
                }
                if (allSlots.add(slotName)) {
                    ++ownSlots;
                }
            }
            if (bases.length > 1 && ((mayAddDict && !wantDict) || (mayAddWeak && !wantWeak))) {
                // Secondary bases may provide weakrefs or dict
                for (PyObject base : bases) {
                    if (base == this.base) {
                        // Skip primary base
                        continue;
                    }

                    if (base instanceof PyClass) {
                        // Classic base class provides both
                        if (mayAddDict && !wantDict) {
                            wantDict = true;
                        }
                        if (mayAddWeak && !wantWeak) {
                            wantWeak = true;
                        }
                        break;
                    }

                    PyType baseType = (PyType) base;
                    if (mayAddDict && !wantDict && baseType.needs_userdict) {
                        wantDict = true;
                    }
                    if (mayAddWeak && !wantWeak && baseType.needs_weakref) {
                        wantWeak = true;
                    }
                    if ((!mayAddDict || wantDict) && (!mayAddWeak || wantWeak)) {
                        // Nothing more to check
                        break;
                    }
                }
            }
            slots_tmp = baseEnd + 1;
        }
        for (int i = slots_tmp; i < slottedAncestors.size(); ++i) {
            insertSlots(slotsMap.get(slottedAncestors.get(i)), allSlots);
        }
        numSlots = allSlots.size();
        int slotPos = 0;
        Iterator<String> slotIter = allSlots.iterator();
        // skip slot names belonging to primary base (i.e. first base.numSlots ones)
        for (; slotPos < base.numSlots; ++slotPos) {
            slotIter.next();
        }
        while (slotIter.hasNext()) {
            String slotName = slotIter.next();
            slotName = mangleName(name, slotName);
            if (dict.__finditem__(slotName) == null) {
                dict.__setitem__(slotName, new PySlot(this, slotName, slotPos++));
            } else {
                --numSlots;
            }
        }
        assert slotPos == numSlots;

        if (wantDict) {
            createDictSlot();
        }
        if (wantWeak) {
            createWeakrefSlot();
        }
        needs_finalizer = needsFinalizer();
    }

    /**
     * Create the __dict__ descriptor.
     */
    private void createDictSlot() {
        String doc = "dictionary for instance variables (if defined)";
        dict.__setitem__("__dict__", new PyDataDescr(this, "__dict__", PyObject.class, doc) {

            @Override
            public boolean implementsDescrGet() {
                return true;
            }

            @Override
            public Object invokeGet(PyObject obj) {
                return obj.getDict();
            }

            @Override
            public boolean implementsDescrSet() {
                return true;
            }

            @Override
            public void invokeSet(PyObject obj, Object value) {
                obj.setDict((PyObject) value);
            }

            @Override
            public boolean implementsDescrDelete() {
                return true;
            }

            @Override
            public void invokeDelete(PyObject obj) {
                obj.delDict();
            }
        });
        needs_userdict = true;
    }

    /**
     * Create the __weakref__ descriptor.
     */
    private void createWeakrefSlot() {
        String doc = "list of weak references to the object (if defined)";
        dict.__setitem__("__weakref__", new PyDataDescr(this, "__weakref__", PyObject.class, doc) {

            private static final String writeMsg = "attribute '%s' of '%s' objects is not writable";

            private void notWritable(PyObject obj) {
                throw Py.AttributeError(
                        String.format(writeMsg, "__weakref__", obj.getType().fastGetName()));
            }

            @Override
            public boolean implementsDescrGet() {
                return true;
            }

            @Override
            public Object invokeGet(PyObject obj) {
                PyList weakrefs = WeakrefModule.getweakrefs(obj);
                switch (weakrefs.size()) {
                    case 0:
                        return Py.None;
                    case 1:
                        return weakrefs.pyget(0);
                    default:
                        return weakrefs;

                }
            }

            @Override
            public boolean implementsDescrSet() {
                return true;
            }

            @Override
            public void invokeSet(PyObject obj, Object value) {
                // XXX: Maybe have PyDataDescr do notWritable() for us
                notWritable(obj);
            }

            @Override
            public boolean implementsDescrDelete() {
                return true;
            }

            @Override
            public void invokeDelete(PyObject obj) {
                notWritable(obj);
            }
        });
        needs_weakref = true;
    }

    /**
     * Setup this type's special attributes.
     */
    private void ensureAttributes() {
        inheritSpecial();

        // special case __new__, if function => static method
        PyObject new_ = dict.__finditem__("__new__");
        // XXX: java functions?
        if (new_ != null && new_ instanceof PyFunction) {
            dict.__setitem__("__new__", new PyStaticMethod(new_));
        }

        ensureDoc(dict);
        ensureModule(dict);

        // Calculate method resolution order
        mro_internal();
        cacheDescrBinds();
    }

    /**
     * Inherit special attributes from the dominant base.
     */
    private void inheritSpecial() {
        if (!needs_userdict && base.needs_userdict) {
            needs_userdict = true;
        }
        if (!needs_weakref && base.needs_weakref) {
            needs_weakref = true;
        }
    }

    /**
     * Ensure dict contains a __doc__.
     *
     * @param dict a PyObject mapping
     */
    public static void ensureDoc(PyObject dict) {
        if (dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", Py.None);
        }
    }

    /**
     * Ensure dict contains a __module__, retrieving it from the current frame if it doesn't exist.
     *
     * @param dict a PyObject mapping
     */
    public static void ensureModule(PyObject dict) {
        if (dict.__finditem__("__module__") != null) {
            return;
        }
        PyFrame frame = Py.getFrame();
        if (frame == null) {
            return;
        }
        PyObject name = frame.f_globals.__finditem__("__name__");
        if (name != null) {
            dict.__setitem__("__module__", name);
        }
    }

    private static PyObject invokeNew(PyObject new_, PyType type, boolean init, PyObject[] args,
            String[] keywords) {
        PyObject obj;
        if (new_ instanceof PyNewWrapper) {
            obj = ((PyNewWrapper) new_).new_impl(init, type, args, keywords);
        } else {
            int n = args.length;
            PyObject[] typePrepended = new PyObject[n + 1];
            System.arraycopy(args, 0, typePrepended, 1, n);
            typePrepended[0] = type;
            obj = new_.__get__(null, type).__call__(typePrepended, keywords);
        }
        return obj;
    }

    /**
     * Complete the initialisation of the <code>PyType</code> for an exposed <code>PyObject</code>.
     * If the type is one of the deferred types (types used in bootstrapping the type system,
     * predominantly), this will only fill in {@link #mro}, {@link #base} and {@link #bases}, and
     * only provisionally. In that case, a second call is made as soon as
     * {@link #addBuilder(Class, TypeBuilder)} is called by the initialisation of the type. In most
     * cases, and on the second visit for deferred types, this call will fill {@link #dict},
     * {@link #name} and all other descriptive state using the exposed characteristics.
     * <p>
     * The caller guarantees that this thread holds the lock on the registry.
     *
     * @param needsInners ignored in the base implementation (see {@link PyJavaType#init(Set)}
     */
    protected void init(Set<PyJavaType> needsInners) {

        Class<?> forClass = underlying_class;

        /*
         * We will have a builder already if the class has Java-initialised. We remove builder from
         * list as we don't need it anymore, and it holds a reference to the class c.
         */
        TypeBuilder builder = Registry.classToBuilder.remove(forClass);
        if (builder == null) {
            // Consider forcing static initialisation in order to get a builder.
            if (!Registry.deferredInit.contains(forClass)) {
                Registry.staticJavaInit(forClass);
                builder = Registry.classToBuilder.remove(forClass);
            }
        }

        if (builder == null) {
            /*
             * No builder has been supplied yet. Be content with partial initialisation of the
             * PyType. When we have a builder, we'll init this again.
             */
            Registry.deferredInit.add(forClass);
            computeLinearMro(PyObject.class);

        } else {
            /* We have a builder so we can go the whole way. Signal c is no longer deferred. */
            Registry.deferredInit.remove(forClass);
            Class<?> baseClass = builder.getBase();
            if (baseClass == Object.class) {
                // Base was not explicitly declared: default is Java super-class.
                baseClass = underlying_class.getSuperclass();
            }
            computeLinearMro(baseClass);

            // The builder supplies the name and collection of exposed methods and properties
            name = builder.getName();
            dict = builder.getDict(this);
            String doc = builder.getDoc();

            // Create a doc string if we don't have one already.
            if (dict.__finditem__("__doc__") == null) {
                PyObject docObj;
                if (doc != null) {
                    // Not PyString(doc) as PyString.TYPE may be null during bootstrapping.
                    docObj = new PyString(Constant.PYSTRING, doc);
                } else {
                    // Not Py.None to avoid load & init of Py module and all its constants.
                    docObj = PyNone.getInstance();
                }
                dict.__setitem__("__doc__", docObj);
            }

            setIsBaseType(builder.getIsBaseType());
            needs_userdict = dict.__finditem__("__dict__") != null;
            instantiable = dict.__finditem__("__new__") != null;
            cacheDescrBinds();
        }
    }

    /**
     * Fills the base and bases of this type with the type of baseClass as sets its mro to this type
     * followed by the mro of baseClass.
     */
    protected void computeLinearMro(Class<?> baseClass) {
        if (underlying_class == PyObject.class) {
            // Special case PyObject: there is no ancestor base: MRO is just {this}.
            mro = new PyType[1];
        } else {
            // MRO of base, with this PyType at the front.
            base = fromClass(baseClass);
            mro = new PyType[base.mro.length + 1];
            System.arraycopy(base.mro, 0, mro, 1, base.mro.length);
            bases = new PyObject[] {base};
        }
        mro[0] = this;
    }

    /**
     * Determine if this type is a descriptor, and if so what kind.
     */
    private void cacheDescrBinds() {
        hasGet = lookup_mro("__get__") != null;
        hasSet = lookup_mro("__set__") != null;
        hasDelete = lookup_mro("__delete__") != null;
    }

    public PyObject getStatic() {
        PyType cur = this;
        while (cur.underlying_class == null) {
            cur = cur.base;
        }
        return cur;
    }

    /**
     * Offers public read-only access to the protected field needs_finalizer.
     *
     * @return a boolean indicating whether the type implements __del__
     */
    public final boolean needsFinalizer() {
        /*
         * It might be sluggish to assume that if a finalizer was needed once, this would never
         * change. However since an expensive FinalizeTrigger was created anyway, it won't hurt to
         * keep it. Whether there actually is a __del__ in the dict, will be checked again when the
         * finalizer runs.
         */
        if (needs_finalizer) {
            return true;
        } else {
            needs_finalizer = lookup_mro("__del__") != null;
            return needs_finalizer;
        }
    }

    /**
     * Ensures that the physical layout between this type and <code>other</code> are compatible.
     * Raises a TypeError if not.
     */
    public void compatibleForAssignment(PyType other, String attribute) {
        if (!getLayout().equals(other.getLayout()) || needs_userdict != other.needs_userdict
                || needs_finalizer != other.needs_finalizer) {
            throw Py.TypeError(String.format("%s assignment: '%s' object layout differs from '%s'",
                    attribute, other.fastGetName(), fastGetName()));
        }
    }

    /**
     * Gets the most parent PyType that determines the layout of this type, ie it has slots or an
     * underlying_class. Can be this PyType.
     */
    private PyType getLayout() {
        if (underlying_class != null) {
            return this;
        } else if (numSlots != base.numSlots) {
            return this;
        }
        return base.getLayout();
    }

    /**
     * Get the most parent Java proxy Class from bases, tallying any encountered Java interfaces.
     *
     * @param bases array of base Jython classes
     * @param interfaces List for collecting interfaces to
     * @return base Java proxy Class
     * @raises Py.TypeError if multiple Java inheritance was attempted
     */
    private static Class<?> getJavaLayout(PyObject[] bases, List<Class<?>> interfaces) {
        Class<?> baseProxy = null;

        for (PyObject base : bases) {
            if (!(base instanceof PyType)) {
                continue;
            }
            Class<?> proxy = ((PyType) base).getProxyType();
            if (proxy == null) {
                continue;
            }
            if (proxy.isInterface()) {
                interfaces.add(proxy);
            } else {
                if (baseProxy != null) {
                    String msg = "no multiple inheritance for Java classes: %s and %s";
                    throw Py.TypeError(String.format(msg, proxy.getName(), baseProxy.getName()));
                }
                baseProxy = proxy;
            }
        }

        return baseProxy;
    }

    /**
     * Setup the javaProxy for this type.
     *
     * @param baseProxyClass this type's base proxyClass
     * @param interfaces a list of Java interfaces in bases
     */
    private void setupProxy(Class<?> baseProxyClass, List<Class<?>> interfaces) {
        if (baseProxyClass == null && interfaces.size() == 0) {
            // javaProxy not applicable
            return;
        }

        String proxyName = name;
        PyObject module = dict.__finditem__("__module__");
        if (module != null) {
            proxyName = module.toString() + "$" + proxyName;
        }
        Class<?> proxyClass =
                MakeProxies.makeProxy(baseProxyClass, interfaces, name, proxyName, dict);
        setProxyAttr(this, proxyClass);

        PyType proxyType = fromClass(proxyClass);
        List<PyObject> cleanedBases = Generic.list();
        boolean addedProxyType = false;
        for (PyObject base : bases) {
            if (!(base instanceof PyType)) {
                cleanedBases.add(base);
                continue;
            }
            Class<?> proxy = ((PyType) base).getProxyType();
            if (proxy == null) {
                // non-proxy types go straight into our lookup
                cleanedBases.add(base);
            } else {

                if (!(base instanceof PyJavaType)) {
                    /*
                     * python subclasses of proxy types need to be added as a base so their version
                     * of methods will show up.
                     */
                    cleanedBases.add(base);
                } else if (!addedProxyType) {
                    /*
                     * Only add a single Java type, since everything's going to go through the proxy
                     * type.
                     */
                    cleanedBases.add(proxyType);
                    addedProxyType = true;
                }
            }
        }
        bases = cleanedBases.toArray(new PyObject[cleanedBases.size()]);
    }

    protected PyObject richCompare(PyObject other, cmpopType op) {
        // Make sure the other object is a type
        if (!(other instanceof PyType) && other != this) {
            return null;
        }

        /*
         * If there is a __cmp__ method defined, let it be called instead of our dumb function
         * designed merely to warn. See CPython bug #7491.
         */
        if (__findattr__("__cmp__") != null || ((PyType) other).__findattr__("__cmp__") != null) {
            return null;
        }

        // Py3K warning if comparison isn't == or !=
        if (Options.py3k_warning && op != cmpopType.Eq && op != cmpopType.NotEq) {
            Py.warnPy3k("type inequality comparisons not supported in 3.x");
            return null;
        }

        // Compare hashes
        int hash1 = object___hash__();
        int hash2 = other.object___hash__();
        switch (op) {
            case Lt:
                return hash1 < hash2 ? Py.True : Py.False;
            case LtE:
                return hash1 <= hash2 ? Py.True : Py.False;
            case Eq:
                return hash1 == hash2 ? Py.True : Py.False;
            case NotEq:
                return hash1 != hash2 ? Py.True : Py.False;
            case Gt:
                return hash1 > hash2 ? Py.True : Py.False;
            case GtE:
                return hash1 >= hash2 ? Py.True : Py.False;
            default:
                return null;
        }
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.type___eq___doc)
    public PyObject type___eq__(PyObject other) {
        return richCompare(other, cmpopType.Eq);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.type___ne___doc)
    public PyObject type___ne__(PyObject other) {
        return richCompare(other, cmpopType.NotEq);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.type___le___doc)
    public PyObject type___le__(PyObject other) {
        return richCompare(other, cmpopType.LtE);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.type___lt___doc)
    public PyObject type___lt__(PyObject other) {
        return richCompare(other, cmpopType.Lt);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.type___ge___doc)
    public PyObject type___ge__(PyObject other) {
        return richCompare(other, cmpopType.GtE);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.type___gt___doc)
    public PyObject type___gt__(PyObject other) {
        return richCompare(other, cmpopType.Gt);
    }

    @ExposedGet(name = "__base__")
    public PyObject getBase() {
        if (base == null) {
            return Py.None;
        }
        return base;
    }

    @ExposedGet(name = "__bases__")
    public PyObject getBases() {
        return new PyTuple(bases);
    }

    @ExposedDelete(name = "__bases__")
    public void delBases() {
        throw Py.TypeError("Can't delete __bases__ attribute");
    }

    @ExposedSet(name = "__bases__")
    public void setBases(PyObject newBasesTuple) {
        if (!(newBasesTuple instanceof PyTuple)) {
            throw Py.TypeError("bases must be a tuple");
        }
        PyObject[] newBases = ((PyTuple) newBasesTuple).getArray();
        if (newBases.length == 0) {
            throw Py.TypeError(
                    "can only assign non-empty tuple to __bases__, not " + newBasesTuple);
        }
        for (int i = 0; i < newBases.length; i++) {
            if (!(newBases[i] instanceof PyType)) {
                if (!(newBases[i] instanceof PyClass)) {
                    throw Py.TypeError(name + ".__bases__ must be a tuple of old- or new-style "
                            + "classes, not " + newBases[i]);
                }
            } else {
                if (((PyType) newBases[i]).isSubType(this)) {
                    throw Py.TypeError("a __bases__ item causes an inheritance cycle");
                }
            }
        }
        PyType newBase = best_base(newBases);
        base.compatibleForAssignment(newBase, "__bases__");
        PyObject[] savedBases = bases;
        PyType savedBase = base;
        PyObject[] savedMro = mro;
        List<Object> savedSubMros = Generic.list();
        try {
            bases = newBases;
            base = newBase;
            mro_internal();
            mro_subclasses(savedSubMros);
            for (PyObject saved : savedBases) {
                if (saved instanceof PyType) {
                    ((PyType) saved).detachSubclass(this);
                }
            }
            for (PyObject newb : newBases) {
                if (newb instanceof PyType) {
                    ((PyType) newb).attachSubclass(this);
                }
            }
        } catch (PyException t) {
            for (Iterator<Object> it = savedSubMros.iterator(); it.hasNext();) {
                PyType subtype = (PyType) it.next();
                PyObject[] subtypeSavedMro = (PyObject[]) it.next();
                subtype.mro = subtypeSavedMro;
            }
            bases = savedBases;
            base = savedBase;
            mro = savedMro;
            throw t;
        }
        postSetattr("__getattribute__");
    }

    private void setIsBaseType(boolean isBaseType) {
        this.isBaseType = isBaseType;
        tp_flags = isBaseType ? tp_flags | Py.TPFLAGS_BASETYPE : tp_flags & ~Py.TPFLAGS_BASETYPE;
    }

    boolean isAbstract() {
        return (tp_flags & Py.TPFLAGS_IS_ABSTRACT) != 0;
    }

    /**
     * Set the {@link #mro} field from the Python {@code mro()} method which uses
     * ({@link #computeMro()} by default. We must repeat this whenever the bases of this type
     * change, which they may in general for classes defined in Python.
     */
    private void mro_internal() {

        if (getType() == TYPE) {
            mro = computeMro(); // Shortcut

        } else {
            // Use the mro() method, which may have been redefined to find the MRO as an array.
            PyObject mroDescr = getType().lookup("mro");
            if (mroDescr == null) {
                throw Py.AttributeError("mro");
            }
            PyObject[] result = Py.make_array(mroDescr.__get__(null, getType()).__call__(this));

            // Verify that Python types in the MRO have a "solid base" in common with this type.
            PyType solid = solid_base(this);

            for (PyObject cls : result) {
                if (cls instanceof PyClass) {
                    continue;
                } else if (cls instanceof PyType) {
                    PyType t = (PyType) cls;
                    if (!solid.isSubType(solid_base(t))) {
                        String fmt = "mro() returned base with unsuitable layout ('%.500s')";
                        throw Py.TypeError(String.format(fmt, t.fastGetName()));
                    }
                } else {
                    String fmt = "mro() returned a non-class ('%.500s')";
                    throw Py.TypeError(String.format(fmt, cls.getType().fastGetName()));
                }
            }
            mro = result;
        }
    }

    /**
     * Collects the subclasses and current mro of this type in mroCollector. If this type has
     * subclasses C and D, and D has a subclass E current mroCollector will equal [C, C.__mro__, D,
     * D.__mro__, E, E.__mro__] after this call.
     */
    private void mro_subclasses(List<Object> mroCollector) {
        for (WeakReference<PyType> ref : subclasses) {
            PyType subtype = ref.get();
            if (subtype == null) {
                continue;
            }
            mroCollector.add(subtype);
            mroCollector.add(subtype.mro);
            subtype.mro_internal();
            subtype.mro_subclasses(mroCollector);
        }
    }

    public PyObject instDict() {
        if (needs_userdict) {
            return new PyStringMap();
        }
        return null;
    }

    private void cleanup_subclasses() {
        Reference<?> ref;
        while ((ref = subclasses_refq.poll()) != null) {
            subclasses.remove(ref);
        }
    }

    @ExposedGet(name = "__mro__")
    public PyTuple getMro() {
        return mro == null ? Py.EmptyTuple : new PyTuple(mro);
    }

    @ExposedGet(name = "__flags__")
    public PyLong getFlags() {
        return new PyLong(tp_flags);
    }

    @ExposedMethod(doc = BuiltinDocs.type___subclasses___doc)
    public synchronized final PyObject type___subclasses__() {
        PyList result = new PyList();
        cleanup_subclasses();
        for (WeakReference<PyType> ref : subclasses) {
            PyType subtype = ref.get();
            if (subtype == null) {
                continue;
            }
            result.append(subtype);
        }
        return result;
    }

    @ExposedMethod(doc = BuiltinDocs.type___subclasscheck___doc)
    public synchronized final boolean type___subclasscheck__(PyObject inst) {
        /*
         * We cannot directly call Py.isSubClass(inst, this), because that would yield endless
         * recursion under some circumstances (e.g. in test_collections).
         */
        return Py.recursiveIsSubClass(inst, this);
    }

    @ExposedMethod(doc = BuiltinDocs.type___instancecheck___doc)
    public synchronized final boolean type___instancecheck__(PyObject inst) {
        /*
         * We cannot directly call Py.isInstance(inst, this), because that would yield endless
         * recursion. So we inline the essential parts from there, excluding checker-delegation and
         * PyTuple special case.
         */
        if (inst.getType() == this) {
            return true;
        }
        return Py.recursiveIsInstance(inst, this);
    }

    /**
     * Returns the Java Class that this type inherits from, or null if this type is Python-only.
     */
    public Class<?> getProxyType() {
        return (Class<?>) getProxyAttr(this);
    }

    private synchronized void attachSubclass(PyType subtype) {
        cleanup_subclasses();
        subclasses.add(new WeakReference<PyType>(subtype, subclasses_refq));
    }

    private synchronized void detachSubclass(PyType subtype) {
        cleanup_subclasses();
        for (WeakReference<PyType> ref : subclasses) {
            if (ref.get() == subtype) {
                subclasses.remove(ref);
                break;
            }
        }
    }

    private synchronized void traverse_hierarchy(boolean top, OnType behavior) {
        boolean stop = false;
        if (!top) {
            stop = behavior.onType(this);
        }
        if (stop) {
            return;
        }
        for (WeakReference<PyType> ref : subclasses) {
            PyType subtype = ref.get();
            if (subtype == null) {
                continue;
            }
            subtype.traverse_hierarchy(false, behavior);
        }
    }

    private static void fill_classic_mro(List<PyObject> acc, PyClass classic_cl) {
        if (!acc.contains(classic_cl)) {
            acc.add(classic_cl);
        }
        PyObject[] bases = classic_cl.__bases__.getArray();
        for (PyObject base : bases) {
            fill_classic_mro(acc, (PyClass) base);
        }
    }

    private static PyObject[] classic_mro(PyClass classic_cl) {
        List<PyObject> acc = Generic.list();
        fill_classic_mro(acc, classic_cl);
        return acc.toArray(new PyObject[acc.size()]);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.type_mro_doc)
    final PyList type_mro(PyObject X) {
        // This is either X.mro (where X is a type object) or type.mro(X)
        PyObject[] res = (X == null) ? computeMro() : ((PyType) X).computeMro();
        return new PyList(res);
    }

    /**
     * Examine the bases (which must contain no repetition) and the MROs of these bases and return
     * the MRO of this class.
     *
     * @return the MRO of this class
     */
    PyObject[] computeMro() {
        // First check that there are no duplicates amongst the bases of this class.
        for (int i = 0; i < bases.length; i++) {
            PyObject cur = bases[i];
            for (int j = i + 1; j < bases.length; j++) {
                if (bases[j] == cur) {
                    PyObject name = cur.__findattr__("__name__");
                    throw Py.TypeError(
                            "duplicate base class " + (name == null ? "?" : name.toString()));
                }
            }
        }

        // Build a table of the MROs of the bases as MROMergeState objects.
        MROMergeState[] toMerge = new MROMergeState[bases.length + 1];
        for (int i = 0; i < bases.length; i++) {
            toMerge[i] = new MROMergeState();
            if (bases[i] instanceof PyType) {
                toMerge[i].mro = ((PyType) bases[i]).mro;
            } else if (bases[i] instanceof PyClass) {
                toMerge[i].mro = classic_mro((PyClass) bases[i]);
            }
        }

        // Append to this table the list of bases of this class
        toMerge[bases.length] = new MROMergeState();
        toMerge[bases.length].mro = bases;

        // The head of the output MRO is the current class itself.
        List<PyObject> mro = Generic.list();
        mro.add(this);

        // Now execute the core of the MRO generation algorithm.
        return computeMro(toMerge, mro);
    }

    /**
     * Core algorithm for computing the MRO for "new-style" (although it's been a while) Python
     * classes.
     *
     * @param toMerge data structure representing the (partly processed) MROs of bases.
     * @param mro partial MRO (initially only this class)
     * @return the MRO of this class
     */
    PyObject[] computeMro(MROMergeState[] toMerge, List<PyObject> mro) {
        boolean addedProxy = false;
        Class<?> thisProxyAttr = this.getProxyType();
        PyType proxyAsType = thisProxyAttr == null ? null : fromClass(thisProxyAttr);

        scan : for (int i = 0; i < toMerge.length; i++) {
            if (toMerge[i].isMerged()) {
                continue scan;
            }

            PyObject candidate = toMerge[i].getCandidate();
            for (MROMergeState mergee : toMerge) {
                if (mergee.pastnextContains(candidate)) {
                    continue scan;
                }
            }

            if (!addedProxy && !(this instanceof PyJavaType) && candidate instanceof PyJavaType) {
                Class<?> candidateProxyAttr = ((PyJavaType) candidate).getProxyType();
                if (candidateProxyAttr != null && PyProxy.class.isAssignableFrom(candidateProxyAttr)
                        && candidateProxyAttr != thisProxyAttr) {
                    /*
                     * If this is a subclass of a Python class that subclasses a Java class, slip
                     * the proxy for this class in before the proxy class in the superclass' mro.
                     * This exposes the methods from the proxy generated for this class in addition
                     * to those generated for the superclass while allowing methods from the
                     * superclass to remain visible from the proxies.
                     */
                    mro.add(proxyAsType);
                    addedProxy = true;
                }
            }

            mro.add(candidate);
            // Was that our own proxy?
            addedProxy |= candidate == proxyAsType;
            for (MROMergeState element : toMerge) {
                element.noteMerged(candidate);
            }
            i = -1; // restart scan
        }

        for (MROMergeState mergee : toMerge) {
            if (!mergee.isMerged()) {
                handleMroError(toMerge, mro);
            }
        }

        return mro.toArray(new PyObject[mro.size()]);
    }

    /**
     * This method is called when the {@link #computeMro(MROMergeState[], List)} reaches an impasse
     * as far as its official algorithm is concerned, with the partial MRO and current state of the
     * working lists at the point the problem is detected. The base implementation raises a Python
     * {@code TypeError}, diagnosing the problem.
     *
     * @param toMerge partially processed algorithm state
     * @param mro output MRO (incomplete)
     */
    void handleMroError(MROMergeState[] toMerge, List<PyObject> mro) {
        StringBuilder msg = new StringBuilder(
                "Cannot create a consistent method resolution\norder (MRO) for bases ");
        Set<PyObject> set = Generic.set();
        for (MROMergeState mergee : toMerge) {
            if (!mergee.isMerged()) {
                set.add(mergee.mro[0]);
            }
        }
        boolean first = true;
        for (PyObject unmerged : set) {
            PyObject name = unmerged.__findattr__("__name__");
            if (first) {
                first = false;
            } else {
                msg.append(", ");
            }
            msg.append(
                    name == null ? "?" : name.toString() + new PyList(((PyType) unmerged).bases));
        }
        throw Py.TypeError(msg.toString());
    }

    /**
     * Finds the first super-type of the given {@code type} that is a "solid base" of the type, that
     * is, the returned type has an {@link #underlying_class}, or it defines {@code __slots__} and
     * no instance level dictionary ({@code __dict__} attribute).
     */
    private static PyType solid_base(PyType type) {
        do {
            if (isSolidBase(type)) {
                return type;
            }
            type = type.base;
        } while (type != null);
        return PyObject.TYPE;
    }

    /**
     * A "solid base" is a type that has an {@link #underlying_class}, or defines {@code __slots__}
     * and no instance level dictionary (no {@code __dict__} attribute).
     */
    private static boolean isSolidBase(PyType type) {
        return type.underlying_class != null || (type.ownSlots != 0 && !type.needs_userdict);
    }

    /**
     * Find the base selected from a {@link #bases} array that has the "most derived solid base".
     * This will become the {@link #base} attribute of a class under construction, or in which the
     * {@link #bases} tuple is being updated. On a successful return, the return value is one of the
     * elements of the argument {@code bases} and the solid base of that return is a sub-type of the
     * solid bases of all other (non-classic) elements of the argument.
     *
     * @throws Py.TypeError if the bases don't all derive from the same solid_base
     * @throws Py.TypeError if at least one of the bases isn't a new-style class
     */
    private static PyType best_base(PyObject[] bases) {
        PyType best = null;         // The best base found so far
        PyType bestSolid = null;    // The solid base of the best base so far
        for (PyObject b : bases) {
            if (b instanceof PyType) {
                PyType base = (PyType) b, solid = solid_base(base);
                if (bestSolid == null) {
                    // First (non-classic) base we find becomes the best base so far
                    best = base;
                    bestSolid = solid;
                } else if (bestSolid.isSubType(solid)) {
                    // Current best is still the best so far
                } else if (solid.isSubType(bestSolid)) {
                    // base is better than the previous best since its solid base is more derived.
                    best = base;
                    bestSolid = solid;
                } else {
                    throw Py.TypeError("multiple bases have instance lay-out conflict");
                }
            } else if (b instanceof PyClass) {
                // Skip over classic bases
                continue;
            } else {
                throw Py.TypeError("bases must be types");
            }
        }
        if (best == null) {
            throw Py.TypeError("a new-style class can't have only classic bases");
        }
        return best;
    }

    private static boolean isJavaRootClass(PyType type) {
        return type instanceof PyJavaType && type.fastGetName().equals("java.lang.Class");
    }

    /**
     * Finds the most derived subtype of initialMetatype in the types of bases, or initialMetatype
     * if it is already the most derived.
     *
     * @raises Py.TypeError if the all the metaclasses don't descend from the same base
     * @raises Py.TypeError if one of the bases is a PyJavaClass or a PyClass with no proxyClass
     */
    private static PyType findMostDerivedMetatype(PyObject[] bases_list, PyType initialMetatype) {
        PyType winner = initialMetatype;
        if (isJavaRootClass(winner)) {
            // consider this root class to be equivalent to type
            winner = PyType.TYPE;
        }

        for (PyObject base : bases_list) {
            if (base instanceof PyClass) {
                continue;
            }
            PyType curtype = base.getType();
            if (isJavaRootClass(curtype)) {
                curtype = PyType.TYPE;
            }

            if (winner.isSubType(curtype)) {
                continue;
            }
            if (curtype.isSubType(winner)) {
                winner = curtype;
                continue;
            }
            throw Py.TypeError("metaclass conflict: the metaclass of a derived class must be a "
                    + "(non-strict) subclass of the metaclasses of all its bases");
        }
        return winner;
    }

    public boolean isSubType(PyType supertype) {
        if (mro != null) {
            for (PyObject base : mro) {
                if (base == supertype) {
                    return true;
                }
            }
            return false;
        }

        // we're not completely initialized yet; follow tp_base
        PyType type = this;
        do {
            if (type == supertype) {
                return true;
            }
            type = type.base;
        } while (type != null);
        return supertype == PyObject.TYPE;
    }

    /**
     * Attribute lookup for name through mro objects' dicts. Lookups are cached.
     *
     * @param name attribute name (must be interned)
     * @return found object or null
     */
    public PyObject lookup(String name) {
        return lookup_where(name, null);
    }

    /**
     * Attribute lookup for name directly through mro objects' dicts. This isn't cached, and should
     * generally only be used during the bootstrapping of a type.
     *
     * @param name attribute name (must be interned)
     * @return found object or null
     */
    protected PyObject lookup_mro(String name) {
        return lookup_where_mro(name, null);
    }

    /**
     * Attribute lookup for name through mro objects' dicts. Lookups are cached.
     *
     * Returns where in the mro the attribute was found at where[0].
     *
     * @param name attribute name (must be interned)
     * @param where Where in the mro the attribute was found is written to index 0
     * @return found object or null
     */
    public PyObject lookup_where(String name, PyObject[] where) {
        return MethodCache.methodCache.lookup_where(this, name, where);
    }

    /**
     * Attribute lookup for name through mro objects' dicts. This isn't cached, and should generally
     * only be used during the bootstrapping of a type.
     *
     * Returns where in the mro the attribute was found at where[0].
     *
     * @param name attribute name (must be interned)
     * @param where Where in the mro the attribute was found is written to index 0
     * @return found object or null
     */
    protected PyObject lookup_where_mro(String name, PyObject[] where) {
        PyObject[] mro = this.mro;
        if (mro == null) {
            return null;
        }
        for (PyObject t : mro) {
            PyObject dict = t.fastGetDict();
            if (dict != null) {
                PyObject obj = dict.__finditem__(name);
                if (obj != null) {
                    if (where != null) {
                        where[0] = t;
                    }
                    return obj;
                }
            }
        }
        return null;
    }

    public PyObject super_lookup(PyType ref, String name) {
        String lookupName;  // the method name to lookup
        PyObject[] mro = this.mro;
        if (mro == null) {
            return null;
        }
        int i;
        for (i = 0; i < mro.length; i++) {
            if (mro[i] == ref) {
                break;
            }
        }
        i++;
        for (; i < mro.length; i++) {
            if (mro[i] instanceof PyJavaType) {
                /*
                 * The MRO contains this proxy for classes extending a Java class and/or interfaces,
                 * but the proxy points back to this starting Python class. So break out of this
                 * infinite loop by ignoring this entry for super purposes. The use of super__
                 * parallels the workaround seen in PyReflectedFunction Fixes
                 * http://bugs.jython.org/issue1540 Also ignore this if we're doing super during
                 * __init__ as we want it to behave Fixes http://bugs.jython.org/issue2375
                 */
                if (name != "__init__" && !name.startsWith("super__")) {
                    lookupName = "super__" + name;
                } else {
                    lookupName = name;
                }
            } else {
                lookupName = name;
            }
            PyObject dict = mro[i].fastGetDict();
            if (dict != null) {
                PyObject obj = dict.__finditem__(lookupName);
                if (obj != null) {
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Register the {@link TypeBuilder} for the given class. This only really makes sense for a
     * <code>PyObject</code>. Initialising a properly-formed PyObject will usually result in a call
     * to <code>addBuilder</code>, thanks to code inserted by the Jython type-exposer.
     *
     * @param c class for which this is the builder
     * @param builder to register
     */
    public static void addBuilder(Class<?> c, TypeBuilder builder) {
        Registry.addBuilder(c, builder);
    }

    /**
     * Attempt to ensure that the that the type system has fully constructed the types necessary to
     * build a fully-working, exposed, <code>PyObject</code> (the "bootstrap types"). Produce a
     * warning message if it does not seem to have worked. This is called at the end of the static
     * initialisation of <code>PyObject</code>.
     *
     * @return whether bootstrapping was successful
     */
    public static synchronized boolean ensureBootstrapped() {
        // Force bootstrap and collect any debris
        Set<Class<?>> missing = Registry.bootstrap();
        if (!missing.isEmpty()) {
            Py.writeWarning("init",
                    "Bootstrap types weren't encountered in bootstrapping: " + missing
                            + "\nThis may be caused by compiled core classes preceding "
                            + "their exposed equivalents on the class path.");
            return false;
        }
        return true;
    }

    /**
     * Equivalent to {@link #fromClass(Class)}, which is to be preferred.
     * <p>
     * The boolean argument is ignored. Previously it controlled whether the returned
     * <code>PyType</code> remained strongly-reachable through a reference the type registry would
     * keep. The returned object is now reachable as long as the class <code>c</code> remains
     * loaded.
     *
     * @param c for which the corresponding <code>PyType</code> is to be found
     * @param hardRef ignored
     * @return the <code>PyType</code> found or created
     */
    public static PyType fromClass(Class<?> c, boolean hardRef) {
        return fromClass(c);
    }

    /**
     * Look up (create if necessary) the <code>PyType</code> for the given target Java class. If the
     * target's <code>PyType</code> already exists, this is returned quickly. When a
     * <code>PyType</code> must be created, the method updates the registry of type information
     * internal to Jython, caching the answer for next time.
     * <p>
     * Creating the <code>PyType</code> also looks up or creates any <code>PyType</code>s that the
     * target depends upon, which results in re-entrant calls to <code>fromClass</code> for these
     * classes and (if <code>PyType</code>s are created for <code>PyObject</code>s) calls to
     * {@link PyType#addBuilder(Class, TypeBuilder)}.
     * <p>
     * Look-up of existing types is non-blocking in the majority of cases.
     *
     * @param c for which the corresponding <code>PyType</code> is to be found
     * @return the <code>PyType</code> found or created
     */
    public static PyType fromClass(Class<?> c) {
        try {
            // Look up or create a Python type for c in the registry.
            return Registry.classToType.get(c);
        } catch (Registry.IncompleteType it) {
            /*
             * This *only* happens when called recursively during type construction, and therefore
             * we can assume the caller is prepared to receive an incompletely constructed PyType as
             * the answer. *
             */
            return it.type;
        }
    }

    @ExposedMethod(doc = BuiltinDocs.type___getattribute___doc)
    final PyObject type___getattribute__(PyObject name) {
        String n = asName(name);
        PyObject ret = type___findattr_ex__(n);
        if (ret == null) {
            noAttributeError(n);
        }
        return ret;
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        return type___findattr_ex__(name);
    }

    // name must be interned
    final PyObject type___findattr_ex__(String name) {
        PyType metatype = getType();
        PyObject metaattr = metatype.lookup(name);
        boolean get = false;

        if (metaattr != null) {
            get = metaattr.implementsDescrGet();
            if (useMetatypeFirst(metaattr) && get && metaattr.isDataDescr()) {
                PyObject res = metaattr.__get__(this, metatype);
                if (res != null) {
                    return res;
                }
            }
        }

        PyObject attr = lookup(name);

        if (attr != null) {
            PyObject res = attr.__get__(null, this);
            if (res != null) {
                return res;
            }
        }

        if (get) {
            return metaattr.__get__(this, metatype);
        }

        if (metaattr != null) {
            return metaattr;
        }

        return null;
    }

    /**
     * Returns true if the given attribute retrieved from an object's metatype should be used before
     * looking for the object on the actual object.
     */
    protected boolean useMetatypeFirst(PyObject attr) {
        return true;
    }

    @ExposedMethod(doc = BuiltinDocs.type___setattr___doc)
    final void type___setattr__(PyObject name, PyObject value) {
        type___setattr__(asName(name), value);
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        type___setattr__(name, value);
    }

    /**
     * Adds the given method to this type's dict under its name in its descriptor. If there's an
     * existing item in the dict, it's replaced.
     */
    public void addMethod(PyBuiltinMethod meth) {
        PyMethodDescr pmd = meth.makeDescriptor(this);
        __setattr__(pmd.getName(), pmd);
    }

    /**
     * Removes the given method from this type's dict or raises a KeyError.
     */
    public void removeMethod(PyBuiltinMethod meth) {
        __delattr__(meth.info.getName());
    }

    void type___setattr__(String name, PyObject value) {
        if (builtin) {
            throw Py.TypeError(String.format(
                    "can't set attributes of built-in/extension type " + "'%s'", this.name));
        }
        super.__setattr__(name, value);
        postSetattr(name);
    }

    void postSetattr(String name) {
        invalidateMethodCache();
        if (name == "__get__") {
            if (!hasGet && lookup("__get__") != null) {
                traverse_hierarchy(false, new OnType() {

                    @Override
                    public boolean onType(PyType type) {
                        boolean old = type.hasGet;
                        type.hasGet = true;
                        return old;
                    }
                });
            }
        } else if (name == "__set__") {
            if (!hasSet && lookup("__set__") != null) {
                traverse_hierarchy(false, new OnType() {

                    @Override
                    public boolean onType(PyType type) {
                        boolean old = type.hasSet;
                        type.hasSet = true;
                        return old;
                    }
                });
            }
        } else if (name == "__delete__") {
            if (!hasDelete && lookup("__delete__") != null) {
                traverse_hierarchy(false, new OnType() {

                    @Override
                    public boolean onType(PyType type) {
                        boolean old = type.hasDelete;
                        type.hasDelete = true;
                        return old;
                    }
                });
            }
        } else if (name == "__getattribute__") {
            traverse_hierarchy(false, new OnType() {

                @Override
                public boolean onType(PyType type) {
                    return (type.usesObjectGetattribute = false);
                }

            });
        }
    }

    @Override
    public void __delattr__(String name) {
        type___delattr__(name);
    }

    @ExposedMethod(doc = BuiltinDocs.type___delattr___doc)
    final void type___delattr__(PyObject name) {
        type___delattr__(asName(name));
    }

    protected void checkDelattr() {}

    void type___delattr__(String name) {
        if (builtin) {
            throw Py.TypeError(String.format(
                    "can't set attributes of built-in/extension type " + "'%s'", this.name));
        }
        super.__delattr__(name);
        postDelattr(name);
    }

    void postDelattr(String name) {
        invalidateMethodCache();
        if (name == "__get__") {
            if (hasGet && lookup("__get__") == null) {
                traverse_hierarchy(false, new OnType() {

                    @Override
                    public boolean onType(PyType type) {
                        boolean absent = type.getDict().__finditem__("__get__") == null;
                        if (absent) {
                            type.hasGet = false;
                            return false;
                        }
                        return true;
                    }
                });
            }
        } else if (name == "__set__") {
            if (hasSet && lookup("__set__") == null) {
                traverse_hierarchy(false, new OnType() {

                    @Override
                    public boolean onType(PyType type) {
                        boolean absent = type.getDict().__finditem__("__set__") == null;
                        if (absent) {
                            type.hasSet = false;
                            return false;
                        }
                        return true;
                    }
                });
            }
        } else if (name == "__delete__") {
            if (hasDelete && lookup("__delete__") == null) {
                traverse_hierarchy(false, new OnType() {

                    @Override
                    public boolean onType(PyType type) {
                        boolean absent = type.getDict().__finditem__("__delete__") == null;
                        if (absent) {
                            type.hasDelete = false;
                            return false;
                        }
                        return true;
                    }
                });
            }
        } else if (name == "__getattribute__") {
            traverse_hierarchy(false, new OnType() {

                @Override
                public boolean onType(PyType type) {
                    return (type.usesObjectGetattribute = false);
                }

            });
        }
    }

    /**
     * Invalidate this type's MethodCache entries. *Must* be called after any modification to
     * __dict__ (or anything else affecting attribute lookups).
     */
    protected void invalidateMethodCache() {
        traverse_hierarchy(false, new OnType() {

            @Override
            public boolean onType(PyType type) {
                type.versionTag = new Object();
                return false;
            }
        });
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return type___call__(args, keywords);
    }

    @ExposedMethod(doc = BuiltinDocs.type___call___doc)
    final PyObject type___call__(PyObject[] args, String[] keywords) {
        PyObject new_ = lookup("__new__");
        if (!instantiable || new_ == null) {
            throw Py.TypeError(String.format("cannot create '%.100s' instances", name));
        }

        PyObject obj = invokeNew(new_, this, true, args, keywords);
        /*
         * When the call was type(something) or the returned object is not an instance of type, it
         * won't be initialized
         */
        if ((this == TYPE && args.length == 1 && keywords.length == 0)
                || !obj.getType().isSubType(this)) {
            return obj;
        }
        obj.dispatch__init__(args, keywords);
        return obj;
    }

    @Override
    protected void __rawdir__(PyDictionary accum) {
        mergeClassDict(accum, this);
    }

    public String fastGetName() {
        return name;
    }

    @ExposedGet(name = "__name__")
    public PyObject pyGetName() {
        return Py.newString(getName());
    }

    public String getName() {
        if (!builtin) {
            return name;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return name.substring(lastDot + 1);
        }
        return name;
    }

    @ExposedSet(name = "__name__")
    public void pySetName(PyObject name) {
        // guarded by __setattr__ to prevent modification of builtins
        if (!(name instanceof PyString)) {
            throw Py.TypeError(String.format("can only assign string to %s.__name__, not '%s'",
                    this.name, name.getType().fastGetName()));
        }
        String nameStr = name.toString();
        if (nameStr.indexOf((char) 0) > -1) {
            throw Py.ValueError("__name__ must not contain null bytes");
        }
        setName(nameStr);
        invalidateMethodCache();
    }

    public void setName(String name) {
        this.name = name;
    }

    @ExposedDelete(name = "__name__")
    public void pyDelName() {
        throw Py.TypeError(String.format("can't delete %s.__name__", name));
    }

    /**
     * Returns the actual dict underlying this type instance. Changes to Java types should go
     * through {@link #addMethod} and {@link #removeMethod}, or unexpected mro errors can occur.
     */
    @Override
    public PyObject fastGetDict() {
        return dict;
    }

    @ExposedGet(name = "__dict__")
    @Override
    public PyObject getDict() {
        return new PyDictProxy(dict);
    }

    @Override
    @ExposedSet(name = "__dict__")
    public void setDict(PyObject newDict) {
        // Analogous to CPython's descrobject:getset_set
        throw Py.AttributeError(
                String.format("attribute '__dict__' of '%s' objects is not " + "writable",
                        getType().fastGetName()));
    }

    @Override
    @ExposedDelete(name = "__dict__")
    public void delDict() {
        setDict(null);
    }

    /**
     * Equivalent of CPython's typeobject.c::type_get_doc; handles __doc__ descriptors.
     */
    @ExposedGet(name = "__doc__")
    public PyObject getDoc() {
        PyObject doc = dict.__finditem__("__doc__");
        if (doc == null) {
            return Py.None;
        }
        return doc.__get__(null, this);
    }

    boolean getUsesObjectGetattribute() {
        return usesObjectGetattribute;
    }

    void setUsesObjectGetattribute(boolean usesObjectGetattribute) {
        this.usesObjectGetattribute = usesObjectGetattribute;
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (underlying_class != null
                && (c == Object.class || c == Class.class || c == Serializable.class)) {
            return underlying_class;
        }
        return super.__tojava__(c);
    }

    @ExposedGet(name = "__module__")
    public PyObject getModule() {
        if (!builtin) {
            return dict.__finditem__("__module__");
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return new PyString(name.substring(0, lastDot));
        }
        return new PyString("__builtin__");
    }

    @ExposedDelete(name = "__module__")
    public void delModule() {
        throw Py.TypeError(String.format("can't delete %s.__module__", name));
    }

    @ExposedGet(name = "__abstractmethods__")
    public PyObject getAbstractmethods() {
        PyObject result = dict.__finditem__("__abstractmethods__");
        if (result == null || result instanceof PyDataDescr) {
            noAttributeError("__abstractmethods__");
        }
        return result;
    }

    @ExposedSet(name = "__abstractmethods__")
    public void setAbstractmethods(PyObject value) {
        /*
         * __abstractmethods__ should only be set once on a type, in abc.ABCMeta.__new__, so this
         * function doesn't do anything special to update subclasses
         */
        dict.__setitem__("__abstractmethods__", value);
        postSetattr("__abstractmethods__");
        tp_flags = value.__nonzero__() ? tp_flags | Py.TPFLAGS_IS_ABSTRACT
                : tp_flags & ~Py.TPFLAGS_IS_ABSTRACT;
    }

    public int getNumSlots() {
        return numSlots;
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.type___repr___doc)
    final String type_toString() {
        String kind = builtin ? "type" : "class";
        if (name == null || (!builtin && dict == null)) {
            // Type not sufficiently ready to show as module.name (useful in debugging)
            return String.format("<%s '%s'>", this.getClass().getSimpleName(), underlying_class);
        } else if (Registry.deferredInit.contains(PyString.class)) {
            // PyString not sufficiently ready to call this.getModule to show module.name
            return String.format("<%s ... '%s'>", kind, name);
        } else {
            // Normal path
            PyObject module = getModule();
            if (module instanceof PyString && !module.toString().equals("__builtin__")) {
                return String.format("<%s '%s.%s'>", kind, module.toString(), getName());
            }
            return String.format("<%s '%s'>", kind, getName());
        }
    }

    @Override
    public String toString() {
        return type_toString();
    }

    /**
     * Raises AttributeError on type objects. The message differs from PyObject#noAttributeError, to
     * mimic CPython behaviour.
     */
    @Override
    public void noAttributeError(String name) {
        throw Py.AttributeError(String.format("type object '%.50s' has no attribute '%.400s'",
                fastGetName(), name));
    }

    /*
     * XXX: consider pulling this out into a generally accessible place. I bet this is duplicated
     * more or less in other places.
     */
    private static String confirmIdentifier(PyObject obj) {
        String identifier;
        if (!(obj instanceof PyString)) {
            throw Py.TypeError(String.format("__slots__ items must be strings, not '%.200s'",
                    obj.getType().fastGetName()));
        } else if (obj instanceof PyUnicode) {
            identifier = ((PyUnicode) obj).encode();
        } else {
            identifier = obj.toString();
        }

        String msg = "__slots__ must be identifiers";
        if (identifier.length() == 0
                || (!Character.isLetter(identifier.charAt(0)) && identifier.charAt(0) != '_')) {
            throw Py.TypeError(msg);
        }
        for (char c : identifier.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                throw Py.TypeError(msg);
            }
        }
        return identifier;
    }

    /*
     * XXX: copied from CodeCompiler.java and changed variable names. Maybe this should go someplace
     * for all classes to use.
     */
    private static String mangleName(String classname, String methodname) {
        if (classname != null && methodname.startsWith("__") && !methodname.endsWith("__")) {
            // remove leading '_' from classname
            int i = 0;
            while (classname.charAt(i) == '_') {
                i++;
            }
            return ("_" + classname.substring(i) + methodname).intern();
        }
        return methodname;
    }

    /** Used when serializing this type. */
    protected Object writeReplace() {
        return new TypeResolver(underlying_class, getModule().toString(), getName());
    }

    private interface OnType {

        boolean onType(PyType type);
    }

    static class TypeResolver implements Serializable {

        private Class<?> underlying_class;

        String module;

        private String name;

        TypeResolver(Class<?> underlying_class, String module, String name) {
            /*
             * Don't store the underlying_class for PyProxies as the proxy type needs to fill in
             * based on the class, not be the class
             */
            if (underlying_class != null && !PyProxy.class.isAssignableFrom(underlying_class)) {
                this.underlying_class = underlying_class;
            }
            this.module = module;
            this.name = name;
        }

        private Object readResolve() {
            if (underlying_class != null) {
                return fromClass(underlying_class);
            }
            PyObject mod = imp.importName(module.intern(), false);
            PyObject pytyp = mod.__getattr__(name.intern());
            if (!(pytyp instanceof PyType)) {
                throw Py.TypeError(module + "." + name + " must be a type for deserialization");
            }
            return pytyp;
        }
    }

    /**
     * Tracks the status of merging a single base into a subclass' mro in computeMro.
     */
    static class MROMergeState {

        /** The mro of the base type we're representing. */
        public PyObject[] mro;

        /**
         * The index of the next item to be merged from mro, or mro.length if this base has been
         * completely merged.
         */
        public int next;

        public boolean isMerged() {
            return mro.length == next;
        }

        public PyObject getCandidate() {
            return mro[next];
        }

        /**
         * Marks candidate as merged for this base if it's the next item to be merged.
         */
        public void noteMerged(PyObject candidate) {
            if (!isMerged() && getCandidate() == candidate) {
                next++;
            }
        }

        /**
         * Returns true if candidate is in the items past this state's next item to be merged.
         */
        public boolean pastnextContains(PyObject candidate) {
            for (int i = next + 1; i < mro.length; i++) {
                if (mro[i] == candidate) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Removes the given item from this state's mro if it isn't already finished.
         */
        public void removeFromUnmerged(PyJavaType winner) {
            if (isMerged()) {
                return;
            }
            List<PyObject> newMro = Generic.list();
            for (PyObject mroEntry : mro) {
                if (mroEntry != winner) {
                    newMro.add(mroEntry);
                }
            }
            mro = newMro.toArray(new PyObject[newMro.size()]);
        }

        @Override
        public String toString() {
            List<String> names = Generic.list();
            for (int i = next; i < mro.length; i++) {
                PyObject t = mro[i];
                if (t instanceof PyType) {
                    names.add(((PyType) t).name);
                } else {
                    names.add(t.toString());
                }
            }
            return names.toString();
        }
    }

    /**
     * A thread safe, non-blocking version of Armin Rigo's mro cache.
     */
    static class MethodCache {

        /** Global mro cache. See {@link PyType#lookup_where(String, PyObject[])}. */
        private static final MethodCache methodCache = new MethodCache();

        /** The fixed size cache. */
        private final AtomicReferenceArray<MethodCacheEntry> table;

        /** Size of the cache exponent (2 ** SIZE_EXP). */
        private static final int SIZE_EXP = 12;

        public MethodCache() {
            table = new AtomicReferenceArray<MethodCacheEntry>(1 << SIZE_EXP);
            clear();
        }

        public void clear() {
            int length = table.length();
            for (int i = 0; i < length; i++) {
                table.set(i, MethodCacheEntry.EMPTY);
            }
        }

        public PyObject lookup_where(PyType type, String name, PyObject where[]) {
            Object versionTag = type.versionTag;
            int index = indexFor(versionTag, name);
            MethodCacheEntry entry = table.get(index);

            if (entry.isValid(versionTag, name)) {
                return entry.get(where);
            }

            // Always cache where
            if (where == null) {
                where = new PyObject[1];
            }
            PyObject value = type.lookup_where_mro(name, where);
            if (isCacheableName(name)) {
                /*
                 * CAS isn't totally necessary here but is possibly more correct. Cache by the
                 * original version before the lookup, if it's changed since then we'll cache a bad
                 * entry. Bad entries and CAS failures aren't a concern as subsequent lookups will
                 * sort themselves out.
                 */
                table.compareAndSet(index, entry,
                        new MethodCacheEntry(versionTag, name, where[0], value));
            }

            return value;
        }

        /**
         * Return the table index for type version/name.
         */
        private static int indexFor(Object version, String name) {
            return (version.hashCode() ^ name.hashCode()) & ((1 << SIZE_EXP) - 1);
        }

        /**
         * Determine if name is cacheable.
         *
         * Since the cache can keep references to names alive longer than usual, it avoids caching
         * unusually large strings.
         */
        private static boolean isCacheableName(String name) {
            return name.length() <= 100;
        }

        static class MethodCacheEntry extends WeakReference<PyObject> {

            /** Version of the entry, a PyType.versionTag. */
            private final Object version;

            /** The name of the attribute. */
            private final String name;

            /** Where in the mro the value was found. */
            private final WeakReference<PyObject> where;

            static final MethodCacheEntry EMPTY = new MethodCacheEntry();

            private MethodCacheEntry() {
                this(null, null, null, null);
            }

            public MethodCacheEntry(Object version, String name, PyObject where, PyObject value) {
                super(value);
                this.version = version;
                this.name = name;
                this.where = new WeakReference<PyObject>(where);
            }

            public boolean isValid(Object version, String name) {
                return this.version == version && this.name == name;
            }

            public PyObject get(PyObject[] where) {
                if (where != null) {
                    where[0] = this.where.get();
                }
                return get();
            }
        }
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (base != null) {
            retVal = visit.visit(base, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        // bases cannot be null
        for (PyObject ob : bases) {
            if (ob != null) {
                retVal = visit.visit(ob, arg);
                if (retVal != 0) {
                    return retVal;
                }
            }
        }
        if (dict != null) {
            retVal = visit.visit(dict, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (mro != null) {
            for (PyObject ob : mro) {
                retVal = visit.visit(ob, arg);
                if (retVal != 0) {
                    return retVal;
                }
            }
        }
        // Don't traverse subclasses since they are weak refs.
        // ReferenceQueue<PyType> subclasses_refq = new ReferenceQueue<PyType>();
        // Set<WeakReference<PyType>> subclasses = Generic.set();
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException {
        if (ob == null) {
            return false;
        }
        // bases cannot be null
        for (PyObject obj : bases) {
            if (obj == ob) {
                return true;
            }
        }
        if (mro != null) {
            for (PyObject obj : mro) {
                if (obj == ob) {
                    return true;
                }
            }
        }
        return ob == base || ob == dict;
    }
}
