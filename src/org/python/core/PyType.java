/* Copyright (c) Jython Developers */
package org.python.core;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;
import org.python.modules._weakref.WeakrefModule;
import org.python.util.Generic;

/**
 * The Python Type object implementation.
 */
@ExposedType(name = "type", doc = BuiltinDocs.type_doc)
public class PyType extends PyObject implements Serializable {

    public static PyType TYPE = fromClass(PyType.class);

    /**
     * The type's name. builtin types include their fully qualified name, e.g.:
     * time.struct_time.
     */
    protected String name;

    /** __base__, the direct base type or null. */
    protected PyType base;

    /** __bases__, the base classes. */
    protected PyObject[] bases = new PyObject[0];

    /** The real, internal __dict__. */
    protected PyObject dict;

    /** __mro__, the method resolution. order */
    protected PyObject[] mro;

    /** __flags__, the type's options. */
    private long tp_flags;

    /**
     * The Java Class instances of this type will be represented as, or null if it's
     * determined by a base type.
     */
    protected Class<?> underlying_class;

    /** Whether it's a builtin type. */
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
    private boolean needs_finalizer;

    /** Whether this type's __getattribute__ is object.__getattribute__. */
    private volatile boolean usesObjectGetattribute;

    /** MethodCacheEntry version tag. */
    private volatile Object versionTag = new Object();

    /** The number of __slots__ defined. */
    private int numSlots;

    private ReferenceQueue<PyType> subclasses_refq = new ReferenceQueue<PyType>();
    private Set<WeakReference<PyType>> subclasses = Generic.set();

    /** Global mro cache. */
    private static final MethodCache methodCache = new MethodCache();

    /** Mapping of Java classes to their PyTypes. */
    private static Map<Class<?>, PyType> class_to_type;

    /** Mapping of Java classes to their TypeBuilders. */
    private static Map<Class<?>, TypeBuilder> classToBuilder;

    protected PyType(PyType subtype) {
        super(subtype);
    }

    private PyType() {
    }

    /**
     * Creates the PyType instance for type itself. The argument just exists to make the constructor
     * distinct.
     */
    private PyType(boolean ignored) {
        super(ignored);
    }

    @ExposedNew
    public static PyObject type___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                        PyObject[] args, String[] keywords) {
        // Special case: type(x) should return x.getType()
        if (args.length == 1 && keywords.length == 0) {
            return args[0].getType();
        }
        // If that didn't trigger, we need 3 arguments. but ArgParser below may give a msg
        // saying type() needs exactly 3.
        if (args.length + keywords.length != 3) {
            throw Py.TypeError("type() takes 1 or 3 arguments");
        }

        ArgParser ap = new ArgParser("type()", args, keywords, "name", "bases", "dict");
        String name = ap.getString(0);
        PyTuple bases = (PyTuple)ap.getPyObjectByType(1, PyTuple.TYPE);
        PyObject dict = ap.getPyObject(2);
        if (!(dict instanceof PyDictionary || dict instanceof PyStringMap)) {
            throw Py.TypeError("type(): argument 3 must be dict, not " + dict.getType());
        }
        return newType(new_, subtype, name, bases, dict);
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

        // Use PyType as the metaclass for Python subclasses of Java classes rather than
        // PyJavaType.  Using PyJavaType as metaclass exposes the java.lang.Object methods
        // on the type, which doesn't make sense for python subclasses.
        if (metatype == PyType.fromClass(Class.class)) {
            metatype = TYPE;
        }

        PyType type;
        if (new_.for_type == metatype) {
            // XXX: set metatype
            type = new PyType();
        } else {
            type = new PyTypeDerived(metatype);
        }

        if (dict instanceof PyStringMap) {
            dict = ((PyStringMap)dict).copy();
        } else {
            dict = ((PyDictionary)dict).copy();
        }

        type.name = name;
        type.bases = tmpBases.length == 0 ? new PyObject[] {PyObject.TYPE} : tmpBases;
        type.dict = dict;
        type.tp_flags = Py.TPFLAGS_HEAPTYPE | Py.TPFLAGS_BASETYPE;

        // immediately setup the javaProxy if applicable. may modify bases
        List<Class<?>> interfaces = Generic.list();
        Class<?> baseProxyClass = getJavaLayout(type.bases, interfaces);
        type.setupProxy(baseProxyClass, interfaces);

        PyType base = type.base = best_base(type.bases);
        if (!base.isBaseType) {
            throw Py.TypeError(String.format("type '%.100s' is not an acceptable base type",
                                             base.name));
        }

        type.createAllSlots(!base.needs_userdict, !base.needs_weakref);
        type.ensureAttributes();
        type.invalidateMethodCache();

        for (PyObject cur : type.bases) {
            if (cur instanceof PyType)
                ((PyType)cur).attachSubclass(type);
        }

        return type;
    }

    /**
     * Create all slots and related descriptors.
     *
     * @param mayAddDict whether a __dict__ descriptor is allowed on this type
     * @param mayAddWeak whether a __weakref__ descriptor is allowed on this type
     */
    private void createAllSlots(boolean mayAddDict, boolean mayAddWeak) {
        numSlots = base.numSlots;
        boolean wantDict = false;
        boolean wantWeak = false;
        PyObject slots = dict.__finditem__("__slots__");

        if (slots == null) {
            wantDict = mayAddDict;
            wantWeak = mayAddWeak;
        } else {
            if (slots instanceof PyString) {
                slots = new PyTuple(slots);
            }

            // Check for valid slot names and create them. Handle two special cases
            for (PyObject slot : slots.asIterable()) {
                String slotName = confirmIdentifier(slot);

                if (slotName.equals("__dict__")) {
                    if (!mayAddDict || wantDict) {
                        throw Py.TypeError("__dict__ slot disallowed: we already got one");
                    }
                    wantDict = true;
                } else if (slotName.equals("__weakref__")) {
                    if (!mayAddWeak || wantWeak) {
                        throw Py.TypeError("__weakref__ slot disallowed: we already got one");
                    }
                    wantWeak = true;
                } else {
                    slotName = mangleName(name, slotName);
                    if (dict.__finditem__(slotName) == null) {
                        dict.__setitem__(slotName, new PySlot(this, slotName, numSlots++));
                    }
                }
            }

            // Secondary bases may provide weakrefs or dict
            if (bases.length > 1
                && ((mayAddDict && !wantDict) || (mayAddWeak && !wantWeak))) {
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

                    PyType baseType = (PyType)base;
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
        }

        if (wantDict) {
            createDictSlot();
        }
        if (wantWeak) {
            createWeakrefSlot();
        }
        needs_finalizer = lookup_mro("__del__") != null;
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
                    obj.setDict((PyObject)value);
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
                private static final String writeMsg =
                        "attribute '%s' of '%s' objects is not writable";

                private void notWritable(PyObject obj) {
                    throw Py.AttributeError(String.format(writeMsg, "__weakref__",
                                                          obj.getType().fastGetName()));
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
     * Ensure dict contains a __module__, retrieving it from the current frame if it
     * doesn't exist.
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
            obj = ((PyNewWrapper)new_).new_impl(init, type, args, keywords);
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
     * Called on builtin types for a particular class. Should fill in dict, name, mro, base and
     * bases from the class.
     */
    protected void init(Class<?> forClass, Set<PyJavaType> needsInners) {
        underlying_class = forClass;
        if (underlying_class == PyObject.class) {
            mro = new PyType[] {this};
        } else {
            Class<?> baseClass;
            if (!Py.BOOTSTRAP_TYPES.contains(underlying_class)) {
                baseClass = classToBuilder.get(underlying_class).getBase();
            } else {
                baseClass = PyObject.class;
            }
            if (baseClass == Object.class) {
                baseClass = underlying_class.getSuperclass();
            }
            computeLinearMro(baseClass);
        }
        if (Py.BOOTSTRAP_TYPES.contains(underlying_class)) {
            // init will be called again from addBuilder which also removes underlying_class from
            // BOOTSTRAP_TYPES
            return;
        }
        TypeBuilder builder = classToBuilder.get(underlying_class);
        name = builder.getName();
        dict = builder.getDict(this);
        String doc = builder.getDoc();
        // XXX: Can't create a __doc__ str until the PyBaseString/PyString types are
        // created
        if (dict.__finditem__("__doc__") == null && forClass != PyBaseString.class
            && forClass != PyString.class) {
            PyObject docObj;
            if (doc != null) {
                docObj = new PyString(doc);
            } else {
                // XXX: Hack: Py.None may be null during bootstrapping. Most types
                // encountered then should have docstrings anyway
                docObj = Py.None == null ? new PyString() : Py.None;
            }
            dict.__setitem__("__doc__", docObj);
        }
        setIsBaseType(builder.getIsBaseType());
        needs_userdict = dict.__finditem__("__dict__") != null;
        instantiable = dict.__finditem__("__new__") != null;
        cacheDescrBinds();
    }

    /**
     * Fills the base and bases of this type with the type of baseClass as sets its mro to this type
     * followed by the mro of baseClass.
     */
    protected void computeLinearMro(Class<?> baseClass) {
        base = PyType.fromClass(baseClass);
        mro = new PyType[base.mro.length + 1];
        System.arraycopy(base.mro, 0, mro, 1, base.mro.length);
        mro[0] = this;
        bases = new PyObject[] {base};
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
     * Get the most parent Java proxy Class from bases, tallying any encountered Java
     * interfaces.
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
            Class<?> proxy = ((PyType)base).getProxyType();
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
        Class<?> proxyClass = MakeProxies.makeProxy(baseProxyClass, interfaces, name, proxyName,
                                                    dict);
        javaProxy = proxyClass;

        PyType proxyType = PyType.fromClass(proxyClass);
        List<PyObject> cleanedBases = Generic.list();
        boolean addedProxyType = false;
        for (PyObject base : bases) {
            if (!(base instanceof PyType)) {
                cleanedBases.add(base);
                continue;
            }
            Class<?> proxy = ((PyType)base).getProxyType();
            if (proxy == null) {
                // non-proxy types go straight into our lookup
                cleanedBases.add(base);
            } else {

                if (!(base instanceof PyJavaType)) {
                    // python subclasses of proxy types need to be added as a base so their
                    // version of methods will show up
                    cleanedBases.add(base);
                } else if (!addedProxyType) {
                    // Only add a single Java type, since everything's going to go through the
                    // proxy type
                    cleanedBases.add(proxyType);
                    addedProxyType = true;
                }
            }
        }
        bases = cleanedBases.toArray(new PyObject[cleanedBases.size()]);
    }

    @ExposedGet(name = "__base__")
    public PyObject getBase() {
        if (base == null)
            return Py.None;
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
        PyObject[] newBases = ((PyTuple)newBasesTuple).getArray();
        if (newBases.length == 0) {
            throw Py.TypeError("can only assign non-empty tuple to __bases__, not "
                               + newBasesTuple);
        }
        for (int i = 0; i < newBases.length; i++) {
            if (!(newBases[i] instanceof PyType)) {
                if (!(newBases[i] instanceof PyClass)) {
                    throw Py.TypeError(name + ".__bases__ must be a tuple of old- or new-style "
                                       + "classes, not " + newBases[i]);
                }
            } else {
                if (((PyType)newBases[i]).isSubType(this)) {
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
                    ((PyType)saved).detachSubclass(this);
                }
            }
            for (PyObject newb : newBases) {
                if (newb instanceof PyType) {
                    ((PyType)newb).attachSubclass(this);
                }
            }
        } catch (PyException t) {
            for (Iterator<Object> it = savedSubMros.iterator(); it.hasNext();) {
                PyType subtype = (PyType)it.next();
                PyObject[] subtypeSavedMro = (PyObject[])it.next();
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

    private void mro_internal() {
        if (getType() == TYPE) {
            mro = computeMro();
        } else {
            PyObject mroDescr = getType().lookup("mro");
            if (mroDescr == null) {
                throw Py.AttributeError("mro");
            }
            PyObject[] result = Py.make_array(mroDescr.__get__(null, getType()).__call__(this));

            PyType solid = solid_base(this);
            for (PyObject cls : result) {
                if (cls instanceof PyClass) {
                    continue;
                }
                if (!(cls instanceof PyType)) {
                    throw Py.TypeError(String.format("mro() returned a non-class ('%.500s')",
                                                     cls.getType().fastGetName()));
                }
                PyType t = (PyType)cls;
                if (!solid.isSubType(solid_base(t))) {
                    throw Py.TypeError(String.format("mro() returned base with unsuitable layout "
                                                     + "('%.500s')", t.fastGetName()));
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
            if (subtype == null)
                continue;
            result.append(subtype);
        }
        return result;
    }

    /**
     * Returns the Java Class that this type inherits from, or null if this type is Python-only.
     */
    public Class<?> getProxyType() {
        return (Class<?>)javaProxy;
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
            fill_classic_mro(acc,(PyClass)base);
        }
    }

    private static PyObject[] classic_mro(PyClass classic_cl) {
        List<PyObject> acc = Generic.list();
        fill_classic_mro(acc, classic_cl);
        return acc.toArray(new PyObject[acc.size()]);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.type_mro_doc)
    final PyList type_mro(PyObject o) {
        if (o == null) {
            return new PyList(computeMro());
        }
        return new PyList(((PyType)o).computeMro());
    }

    PyObject[] computeMro() {
        for (int i = 0; i < bases.length; i++) {
            PyObject cur = bases[i];
            for (int j = i + 1; j < bases.length; j++) {
                if (bases[j] == cur) {
                    PyObject name = cur.__findattr__("__name__");
                    throw Py.TypeError("duplicate base class " +
                                       (name == null ? "?" : name.toString()));
                }
            }
        }

        MROMergeState[] toMerge = new MROMergeState[bases.length + 1];
        for (int i = 0; i < bases.length; i++) {
            toMerge[i] = new MROMergeState();
            if (bases[i] instanceof PyType) {
                toMerge[i].mro = ((PyType)bases[i]).mro;
            } else if (bases[i] instanceof PyClass) {
                toMerge[i].mro = classic_mro((PyClass)bases[i]);
            }
        }
        toMerge[bases.length] = new MROMergeState();
        toMerge[bases.length].mro = bases;

        List<PyObject> mro = Generic.list();
        mro.add(this);
        return computeMro(toMerge, mro);
    }

    PyObject[] computeMro(MROMergeState[] toMerge, List<PyObject> mro) {
        boolean addedProxy = false;
        PyType proxyAsType = javaProxy == null ? null : PyType.fromClass(((Class<?>)javaProxy));
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
            if (!addedProxy && !(this instanceof PyJavaType) && candidate instanceof PyJavaType
                    && candidate.javaProxy != null
                    && PyProxy.class.isAssignableFrom(((Class<?>)candidate.javaProxy))
                    && candidate.javaProxy != javaProxy) {
                // If this is a subclass of a Python class that subclasses a Java class, slip the
                // proxy for this class in before the proxy class in the superclass' mro.
                // This exposes the methods from the proxy generated for this class in addition to
                // those generated for the superclass while allowing methods from the superclass to
                // remain visible from the proxies.
                mro.add(proxyAsType);
                addedProxy = true;
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
     * Must either throw an exception, or bring the merges in <code>toMerge</code> to completion by
     * finishing filling in <code>mro</code>.
     */
    void handleMroError(MROMergeState[] toMerge, List<PyObject> mro) {
        StringBuilder msg = new StringBuilder("Cannot create a consistent method resolution\n"
                + "order (MRO) for bases ");
        Set<PyObject> set = Generic.set();
        for (MROMergeState mergee : toMerge) {
            if(!mergee.isMerged()) {
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
            msg.append(name == null ? "?" : name.toString() + new PyList(((PyType)unmerged).bases));
        }
        throw Py.TypeError(msg.toString());
    }

    /**
     * Finds the parent of type with an underlying_class or with slots sans a __dict__
     * slot.
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

    private static boolean isSolidBase(PyType type) {
        return type.underlying_class != null || (type.numSlots != 0 && !type.needs_userdict);
    }

    /**
     * Finds the base in bases with the most derived solid_base, ie the most base type
     *
     * @throws Py.TypeError if the bases don't all derive from the same solid_base
     * @throws Py.TypeError if at least one of the bases isn't a new-style class
     */
    private static PyType best_base(PyObject[] bases) {
        PyType winner = null;
        PyType candidate = null;
        PyType best = null;
        for (PyObject base : bases) {
            if (base instanceof PyClass) {
                continue;
            }
            if (!(base instanceof PyType)) {
                throw Py.TypeError("bases must be types");
            }
            candidate = solid_base((PyType)base);
            if (winner == null) {
                winner = candidate;
                best = (PyType)base;
            } else if (winner.isSubType(candidate)) {
                ;
            } else if (candidate.isSubType(winner)) {
                winner = candidate;
                best = (PyType)base;
            } else {
                throw Py.TypeError("multiple bases have instance lay-out conflict");
            }
        }
        if (best == null) {
            throw Py.TypeError("a new-style class can't have only classic bases");
        }
        return best;
    }

    /**
     * Finds the most derived subtype of initialMetatype in the types
     * of bases, or initialMetatype if it is already the most derived.
     *
     * @raises Py.TypeError if the all the metaclasses don't descend
     * from the same base
     * @raises Py.TypeError if one of the bases is a PyJavaClass or a
     * PyClass with no proxyClass
     */
    private static PyType findMostDerivedMetatype(PyObject[] bases_list, PyType initialMetatype) {
        PyType winner = initialMetatype;
        for (PyObject base : bases_list) {
            if (base instanceof PyClass) {
                continue;
            }
            PyType curtype = base.getType();
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
     * Attribute lookup for name directly through mro objects' dicts. This isn't cached,
     * and should generally only be used during the bootstrapping of a type.
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
        return methodCache.lookup_where(this, name, where);
    }

    /**
     * Attribute lookup for name through mro objects' dicts. This isn't cached, and should
     * generally only be used during the bootstrapping of a type.
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
        PyObject[] mro = this.mro;
        if (mro == null) {
            return null;
        }
        int i;
        for (i = 0; i < mro.length; i++) {
            if (mro[i] == ref)
                break;
        }
        i++;
        for (; i < mro.length; i++) {
            PyObject dict = mro[i].fastGetDict();
            if (dict != null) {
                PyObject obj = dict.__finditem__(name);
                if (obj != null)
                    return obj;
            }
        }
        return null;
    }

    public static void addBuilder(Class<?> forClass, TypeBuilder builder) {
        if (classToBuilder == null) {
            classToBuilder = Generic.map();
        }
        classToBuilder.put(forClass, builder);

        if (class_to_type.containsKey(forClass)) {
            if (!Py.BOOTSTRAP_TYPES.remove(forClass)) {
                Py.writeWarning("init", "Bootstrapping class not in Py.BOOTSTRAP_TYPES[class="
                                + forClass + "]");
            }
            // The types in Py.BOOTSTRAP_TYPES are initialized before their builders are assigned,
            // so do the work of addFromClass & fillFromClass after the fact
            fromClass(builder.getTypeClass()).init(builder.getTypeClass(), null);
        }
    }

    private static PyType addFromClass(Class<?> c, Set<PyJavaType> needsInners) {
        if (ExposeAsSuperclass.class.isAssignableFrom(c)) {
            PyType exposedAs = fromClass(c.getSuperclass());
            class_to_type.put(c, exposedAs);
            return exposedAs;
        }
        return createType(c, needsInners);
    }

    private static TypeBuilder getBuilder(Class<?> c) {
        if (classToBuilder == null) {
            // PyType itself has yet to be initialized.  This should be a bootstrap type, so it'll
            // go through the builder process in a second
            return null;
        }
        if (c.isPrimitive() || !PyObject.class.isAssignableFrom(c)) {
            // If this isn't a PyObject, don't bother forcing it to be initialized to load its
            // builder
            return null;
        }

        // This is a PyObject, call forName to force static initialization on the class so if it has
        // a builder, it'll be filled in
        SecurityException exc = null;
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (ClassNotFoundException e) {
            // Well, this is certainly surprising.
            throw new RuntimeException("Got ClassNotFound calling Class.forName on an already "
                + " found class.", e);
        } catch (ExceptionInInitializerError e) {
            throw Py.JavaError(e);
        } catch (SecurityException e) {
            exc = e;
        }
        TypeBuilder builder = classToBuilder.get(c);
        if (builder == null && exc != null) {
            Py.writeComment("type",
                    "Unable to initialize " + c.getName() + ", a PyObject subclass, due to a " +
                    "security exception, and no type builder could be found for it. If it's an " +
                    "exposed type, it may not work properly.  Security exception: " +
                    exc.getMessage());
        }
        return builder;
    }

    private static PyType createType(Class<?> c, Set<PyJavaType> needsInners) {
        PyType newtype;
        if (c == PyType.class) {
            newtype = new PyType(false);
        } else if (Py.BOOTSTRAP_TYPES.contains(c) || getBuilder(c) != null) {
            newtype = new PyType();
        } else {
            newtype = new PyJavaType();
        }


        // If filling in the type above filled the type under creation, use that one
        PyType type = class_to_type.get(c);
        if (type != null) {
            return type;
        }

        class_to_type.put(c, newtype);
        newtype.builtin = true;
        newtype.init(c, needsInners);
        newtype.invalidateMethodCache();
        return newtype;
    }

    // XXX what's the proper scope of this synchronization? given module import lock, might be
    // ok to omit this sync here...
    public static synchronized PyType fromClass(Class<?> c) {
        if (class_to_type == null) {
            class_to_type = Generic.synchronizedWeakHashMap();
            addFromClass(PyType.class, null);
        }
        PyType type = class_to_type.get(c);
        if (type != null) {
            return type;
        }
        // We haven't seen this class before, so it's type needs to be created. If it's being
        // exposed as a Java class, defer processing its inner types until it's completely
        // created in case the inner class references a class that references this class.
        Set<PyJavaType> needsInners = Generic.set();
        PyType result = addFromClass(c, needsInners);
        for (PyJavaType javaType : needsInners) {
            Class<?> forClass = javaType.getProxyType();
            if (forClass == null) {
                continue;
            }
            for (Class<?> inner : forClass.getClasses()) {
                // Only add the class if there isn't something else with that name and it came from this
                // class
                if (inner.getDeclaringClass() == forClass &&
                        javaType.dict.__finditem__(inner.getSimpleName()) == null) {
                    // If this class is currently being loaded, any exposed types it contains won't have
                    // set their builder in PyType yet, so add them to BOOTSTRAP_TYPES so they're
                    // created as PyType instead of PyJavaType
                    if (inner.getAnnotation(ExposedType.class) != null
                            || ExposeAsSuperclass.class.isAssignableFrom(inner)) {
                        Py.BOOTSTRAP_TYPES.add(inner);
                    }
                    javaType.dict.__setitem__(inner.getSimpleName(), PyType.fromClass(inner));
                }
            }
        }
        return result;
    }

    static PyType fromClassSkippingInners(Class<?> c, Set<PyJavaType> needsInners) {
        PyType type = class_to_type.get(c);
        if (type != null) {
            return type;
        }
        return addFromClass(c, needsInners);
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
                if (res != null)
                    return res;
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
            throw Py.TypeError(String.format("can't set attributes of built-in/extension type "
                    + "'%s'", this.name));
        }
        super.__setattr__(name, value);
        postSetattr(name);
    }

    void postSetattr(String name) {
        invalidateMethodCache();
        if (name == "__get__") {
            if (!hasGet && lookup("__get__") != null) {
                traverse_hierarchy(false, new OnType() {
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
                    public boolean onType(PyType type) {
                        boolean old = type.hasDelete;
                        type.hasDelete = true;
                        return old;
                    }
                });
            }
        } else if (name == "__getattribute__") {
            traverse_hierarchy(false, new OnType() {
                    public boolean onType(PyType type) {
                        return (type.usesObjectGetattribute = false);
                    }
                });
        }
    }

    public void __delattr__(String name) {
        type___delattr__(name);
    }

    @ExposedMethod(doc = BuiltinDocs.type___delattr___doc)
    final void type___delattr__(PyObject name) {
        type___delattr__(asName(name));
    }

    protected void checkDelattr() {
    }

    void type___delattr__(String name) {
        if (builtin) {
            throw Py.TypeError(String.format("can't set attributes of built-in/extension type "
                    + "'%s'", this.name));
        }
        super.__delattr__(name);
        postDelattr(name);
    }

    void postDelattr(String name) {
        invalidateMethodCache();
        if (name == "__get__") {
            if (hasGet && lookup("__get__") == null) {
                traverse_hierarchy(false, new OnType() {
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
                    public boolean onType(PyType type) {
                        return (type.usesObjectGetattribute = false);
                    }
                });
        }
    }

    /**
     * Invalidate this type's MethodCache entries. *Must* be called after any modification
     * to __dict__ (or anything else affecting attribute lookups).
     */
    protected void invalidateMethodCache() {
        traverse_hierarchy(false, new OnType() {
                public boolean onType(PyType type) {
                    type.versionTag = new Object();
                    return false;
                }
            });
    }

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
        // When the call was type(something) or the returned object is not an instance of
        // type, it won't be initialized
        if ((this == TYPE && args.length == 1 && keywords.length == 0)
            || !obj.getType().isSubType(this)) {
            return obj;
        }
        obj.dispatch__init__(args, keywords);
        return obj;
    }

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
        if (nameStr.indexOf((char)0) > -1) {
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
    public PyObject fastGetDict() {
        return dict;
    }

    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        return new PyDictProxy(dict);
    }

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject newDict) {
        // Analogous to CPython's descrobject:getset_set
        throw Py.AttributeError(String.format("attribute '__dict__' of '%s' objects is not "
                                              + "writable", getType().fastGetName()));
    }

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

    public Object __tojava__(Class<?> c) {
        if (underlying_class != null && (c == Object.class || c == Class.class ||
                                         c == Serializable.class)) {
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
            return Py.newString(name.substring(0, lastDot));
        }
        return Py.newString("__builtin__");
    }

    @ExposedDelete(name = "__module__")
    public void delModule() {
        throw Py.TypeError(String.format("can't delete %s.__module__", name));
    }

    public int getNumSlots() {
        return numSlots;
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.type___repr___doc)
    final String type_toString() {
        String kind;
        if (!builtin) {
            kind = "class";
        } else {
            kind = "type";
        }
        PyObject module = getModule();
        if (module instanceof PyString && !module.toString().equals("__builtin__")) {
            return String.format("<%s '%s.%s'>", kind, module.toString(), getName());
        }
        return String.format("<%s '%s'>", kind, getName());
    }

    public String toString() {
        return type_toString();
    }

    /**
     * Raises AttributeError on type objects. The message differs from
     * PyObject#noAttributeError, to mimic CPython behaviour.
     */
    public void noAttributeError(String name) {
        throw Py.AttributeError(String.format("type object '%.50s' has no attribute '%.400s'",
                                              fastGetName(), name));
    }

    //XXX: consider pulling this out into a generally accessible place
    //     I bet this is duplicated more or less in other places.
    private static String confirmIdentifier(PyObject obj) {
        String identifier;
        if (!(obj instanceof PyString)) {
            throw Py.TypeError(String.format("__slots__ items must be strings, not '%.200s'",
                                             obj.getType().fastGetName()));
        } else if (obj instanceof PyUnicode) {
            identifier = ((PyUnicode)obj).encode();
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

    //XXX: copied from CodeCompiler.java and changed variable names.
    //       Maybe this should go someplace for all classes to use.
    private static String mangleName(String classname, String methodname) {
        if (classname != null && methodname.startsWith("__") && !methodname.endsWith("__")) {
            //remove leading '_' from classname
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
            // Don't store the underlying_class for PyProxies as the proxy type needs to fill in
            // based on the class, not be the class
            if (underlying_class != null && !PyProxy.class.isAssignableFrom(underlying_class)) {
                this.underlying_class = underlying_class;
            }
            this.module = module;
            this.name = name;
        }

        private Object readResolve() {
            if (underlying_class != null) {
                return PyType.fromClass(underlying_class);
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
    }

    /**
     * A thead safe, non-blocking version of Armin Rigo's mro cache.
     */
    static class MethodCache {

        /** The fixed size cache. */
        private final AtomicReferenceArray<MethodCacheEntry> table;

        /** Size of the cache exponent (2 ** SIZE_EXP). */
        public static final int SIZE_EXP = 11;

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
                // CAS isn't totally necessary here but is possibly more correct. Cache by
                // the original version before the lookup, if it's changed since then
                // we'll cache a bad entry. Bad entries and CAS failures aren't a concern
                // as subsequent lookups will sort themselves out
                table.compareAndSet(index, entry, new MethodCacheEntry(versionTag, name, where[0],
                                                                       value));
            }
            return value;
        }

        /**
         * Return the table index for type version/name.
         */
        private static int indexFor(Object version, String name) {
            return (version.hashCode() * name.hashCode()) >>> (Integer.SIZE - SIZE_EXP);
        }

        /**
         * Determine if name is cacheable.
         *
         * Since the cache can keep references to names alive longer than usual, it avoids
         * caching unusually large strings.
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
}
