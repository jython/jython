package org.python.core;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;
import org.python.util.Generic;

/**
 * The Python Type object implementation.
 */
@ExposedType(name = "type")
public class PyType extends PyObject implements Serializable {

    public static PyType TYPE = fromClass(PyType.class);

    /** The type's name. builtin types include their fully qualified name, e.g.: time.struct_time. */
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

    /** The underlying java class or null. */
    protected Class<?> underlying_class;

    /** Whether it's a builtin type. */
    protected boolean builtin;

    /** Whether new instances of this type can be instantiated */
    protected boolean instantiable = true;

    /** Whether this type has set/delete descriptors */
    boolean has_set;
    boolean has_delete;

    /** Whether this type allows subclassing. */
    private boolean isBaseType = true;

    /** Whether finalization is required for this type's instances (implements __del__). */
    private boolean needs_finalizer;

    /** Whether this type's instances require a __dict__. */
    protected boolean needs_userdict;

    /** The number of __slots__ defined. */
    private int numSlots;

    private ReferenceQueue<PyType> subclasses_refq = new ReferenceQueue<PyType>();
    private Set<WeakReference<PyType>> subclasses = Generic.set();

    /** Mapping of Java classes to their PyTypes. */
    private static Map<Class<?>, PyType> class_to_type;

    /** Mapping of Java classes to their TypeBuilders. */
    private static Map<Class<?>, TypeBuilder> classToBuilder;

    protected PyType(PyType subtype) {
        super(subtype);
    }


    private PyType() {}

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
        if (args.length == 1 && keywords.length == 0) {
            return args[0].getType();
        }
        if (args.length + keywords.length != 3) {
            throw Py.TypeError("type() takes exactly 1 or 3 arguments");
        }

        ArgParser ap = new ArgParser("type()", args, keywords, "name", "bases", "dict");
        String name = ap.getString(0);
        PyObject bases = ap.getPyObject(1);

        if (!(bases instanceof PyTuple)) {
            throw Py.TypeError("type(): bases must be tuple");
        }
        PyObject dict = ap.getPyObject(2);
        if (!(dict instanceof PyDictionary || dict instanceof PyStringMap)) {
            throw Py.TypeError("type(): dict must be dict");
        }
        return newType(new_, subtype, name, (PyTuple)bases, dict);
    }

    public static PyObject newType(PyNewWrapper new_, PyType metatype, String name, PyTuple bases,
                                   PyObject dict) {
        PyType object_type = fromClass(PyObject.class);

        PyObject[] bases_list = bases.getArray();
        PyType winner = findMostDerivedMetatype(bases_list, metatype);
        if (winner != metatype) {
            PyObject winner_new_ = winner.lookup("__new__");
            if (winner_new_ != null && winner_new_ != new_) {
                return invoke_new_(new_, winner, false,
                                   new PyObject[] {new PyString(name), bases, dict},
                                   Py.NoKeywords);
            }
            metatype = winner;
        }
        // Use PyType as the metaclass for Python subclasses of Java classes rather than PyJavaType.
        // Using PyJavaType as metaclass exposes the java.lang.Object methods on the type, which
        // doesn't make sense for python subclasses.
        if (metatype == PyType.fromClass(Class.class)) {
            metatype = TYPE;
        }
        if (bases_list.length == 0) {
            bases_list = new PyObject[] {object_type};
        }
        List<Class<?>> interfaces = Generic.list();
        Class<?> baseClass = null;
        for (PyObject base : bases_list) {
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
                if (baseClass != null) {
                    throw Py.TypeError("no multiple inheritance for Java classes: "
                            + proxy.getName() + " and " + baseClass.getName());
                }
                baseClass = proxy;
            }
        }

        if (dict.__finditem__("__module__") == null) {
           PyFrame frame = Py.getFrame();
           if (frame != null) {
               PyObject globals = frame.f_globals;
               PyObject modname;
               if ((modname = globals.__finditem__("__name__")) != null) {
                   dict.__setitem__("__module__", modname);
               }
           }
        }
        // XXX also __doc__ __module__


        Class<?> proxyClass = null;
        if (baseClass != null || interfaces.size() != 0) {
            String proxyName = name;
            PyObject module = dict.__finditem__("__module__");
            if (module != null) {
                proxyName = module.toString() + "$" + proxyName;
            }
            proxyClass = MakeProxies.makeProxy(baseClass, interfaces, name, proxyName, dict);
            PyType proxyType = PyType.fromClass(proxyClass);
            List<PyObject> cleanedBases = Generic.list();
            boolean addedProxyType = false;
            for (PyObject base : bases_list) {
                if (base instanceof PyJavaType) {
                    if (!addedProxyType) {
                        cleanedBases.add(proxyType);
                        addedProxyType = true;
                    }
                } else {
                    cleanedBases.add(base);
                }
            }
            bases_list = cleanedBases.toArray(new PyObject[cleanedBases.size()]);
        }
        PyType newtype;
        if (new_.for_type == metatype || metatype == PyType.fromClass(Class.class)) {
            newtype = new PyType(); // XXX set metatype
            if(proxyClass != null) {
                newtype.underlying_class = proxyClass;
            }
        } else {
            newtype = new PyTypeDerived(metatype);
        }
        if (dict instanceof PyStringMap) {
            dict = ((PyStringMap)dict).copy();
        } else {
            dict = ((PyDictionary)dict).copy();
        }
        newtype.dict = dict;
        newtype.name = name;
        newtype.base = best_base(bases_list);
        newtype.numSlots = newtype.base.numSlots;
        newtype.bases = bases_list;

        if (!newtype.base.isBaseType) {
            throw Py.TypeError(String.format("type '%.100s' is not an acceptable base type",
                                             newtype.base.name));
        }

        PyObject slots = dict.__finditem__("__slots__");
        boolean needsDictDescr = false;
        if (slots == null) {
            newtype.needs_userdict = true;
            // a dict descriptor is required if base doesn't already provide a dict
            needsDictDescr = !newtype.base.needs_userdict;
        } else {
            // have slots, but may inherit a dict
            newtype.needs_userdict = newtype.base.needs_userdict;

            if (slots instanceof PyString) {
                addSlot(newtype, slots);
            } else {
                for (PyObject slotname : slots.asIterable()) {
                    addSlot(newtype, slotname);
                }
            }

            if (!newtype.base.needs_userdict && newtype.needs_userdict) {
                // base doesn't provide dict but addSlot found the __dict__ slot
                needsDictDescr = true;
            } else if (bases_list.length > 0 && !newtype.needs_userdict) {
                // secondary bases may provide dict
                for (PyObject base : bases_list) {
                    if (base == newtype.base) {
                        // Skip primary base
                        continue;
                    }
                    if (base instanceof PyClass) {
                        // Classic base class provides dict
                        newtype.needs_userdict = true;
                        needsDictDescr = true;
                        break;
                    }
                    PyType tmpType = (PyType)base;
                    if (tmpType.needs_userdict) {
                        newtype.needs_userdict = true;
                        needsDictDescr = true;
                        // Nothing more to check
                        break;
                    }
                }
            }
        }

        newtype.tp_flags = Py.TPFLAGS_HEAPTYPE | Py.TPFLAGS_BASETYPE;

        // special case __new__, if function => static method
        PyObject tmp = dict.__finditem__("__new__");
        if (tmp != null && tmp instanceof PyFunction) { // XXX java functions?
            dict.__setitem__("__new__", new PyStaticMethod(tmp));
        }

        newtype.mro_internal();
        // __dict__ descriptor
        if (needsDictDescr && dict.__finditem__("__dict__") == null) {
            dict.__setitem__("__dict__", new PyDataDescr(newtype, "__dict__", PyObject.class) {

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
        }

        newtype.fillHasSetAndDelete();
        newtype.needs_finalizer = newtype.lookup("__del__") != null;

        for (PyObject cur : bases_list) {
            if (cur instanceof PyType)
                ((PyType)cur).attachSubclass(newtype);
        }
        return newtype;
    }

    private static PyObject invoke_new_(PyObject new_, PyType type, boolean init, PyObject[] args,
                                        String[] keywords) {
        PyObject newobj;
        if (new_ instanceof PyNewWrapper) {
            newobj = ((PyNewWrapper)new_).new_impl(init, type, args, keywords);
        } else {
            int n = args.length;
            PyObject[] type_prepended = new PyObject[n + 1];
            System.arraycopy(args, 0, type_prepended, 1, n);
            type_prepended[0] = type;
            newobj = new_.__get__(null, type).__call__(type_prepended, keywords);
        }
        /* special case type(x) */
        if (type == TYPE && args.length == 1 && keywords.length == 0) {
            return newobj;
        }
        newobj.dispatch__init__(type, args, keywords);
        return newobj;
    }

    /**
     * Called on builtin types after underlying_class has been set on them. Should fill in dict,
     * name, mro, base and bases from the class.
     */
    protected void init() {
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
        setIsBaseType(builder.getIsBaseType());
        instantiable = dict.__finditem__("__new__") != null;
        fillHasSetAndDelete();
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

    private void fillHasSetAndDelete() {
        has_set = lookup("__set__") != null;
        has_delete = lookup("__delete__") != null;
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

    //XXX: needs __doc__
    @ExposedGet(name = "__base__")
    public PyObject getBase() {
        if (base == null)
            return Py.None;
        return base;
    }

    //XXX: needs __doc__
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
    }

    private void setIsBaseType(boolean isBaseType) {
        this.isBaseType = isBaseType;
        tp_flags = isBaseType ? tp_flags | Py.TPFLAGS_BASETYPE : tp_flags & ~Py.TPFLAGS_BASETYPE;
    }

    private void mro_internal() {
        if (getType() == TYPE) {
            mro = compute_mro();
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
        for (PyObject base : bases) {
            if (base instanceof PyType) {
                Class<?> javaType = ((PyType)base).getProxyType();
                if (javaType != null) {
                    return javaType;
                }
            }
        }
        return null;
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

    private static boolean tail_contains(PyObject[] lst, int whence, PyObject o) {
        int n = lst.length;
        for (int i = whence + 1; i < n; i++) {
            if (lst[i] == o) {
                return true;
            }
        }
        return false;
    }

    private static PyException mro_error(PyObject[][] to_merge, int[] remain) {
        StringBuilder msg = new StringBuilder("Cannot create a consistent method resolution\n"
                                            + "order (MRO) for bases ");
        PyDictionary set = new PyDictionary();
        for (int i = 0; i < to_merge.length; i++) {
            PyObject[] lst = to_merge[i];
            if (remain[i] < lst.length) {
                set.__setitem__(lst[remain[i]], Py.None);
            }
        }
        PyObject iter = set.__iter__();
        PyObject cur;
        boolean subq = false;
        while ((cur = iter.__iternext__()) != null) {
            PyObject name = cur.__findattr__("__name__");
            if (!subq) {
                subq = true;
            } else {
                msg.append(", ");
            }
            msg.append(name == null ? "?" : name.toString());
        }
        return Py.TypeError(msg.toString());
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.type_mro_doc)
    final PyList type_mro(PyObject o) {
        if (o == null) {
            return new PyList(compute_mro());
        }
        return new PyList(((PyType)o).compute_mro());
    }

    PyObject[] compute_mro() {
        PyObject[] bases = this.bases;
        int n = bases.length;
        for (int i = 0; i < n; i++) {
            PyObject cur = bases[i];
            for (int j = i + 1; j < n; j++) {
                if (bases[j] == cur) {
                    PyObject name = cur.__findattr__("__name__");
                    throw Py.TypeError("duplicate base class " +
                                       (name == null ? "?" : name.toString()));
                }
            }
        }

        int nmerge = n + 1;
        PyObject[][] to_merge = new PyObject[nmerge][];
        int[] remain = new int[nmerge];

        for (int i = 0; i < n; i++) {
            PyObject cur = bases[i];
            remain[i] = 0;
            if (cur instanceof PyType) {
                to_merge[i] = ((PyType)cur).mro;
            } else if (cur instanceof PyClass) {
                to_merge[i] = classic_mro((PyClass)cur);
            }
        }

        to_merge[n] = bases;
        remain[n] = 0;

        List<PyObject> acc = Generic.list();
        acc.add(this);

        int empty_cnt = 0;

        scan : for (int i = 0; i < nmerge; i++) {
            PyObject candidate;
            PyObject[] cur = to_merge[i];
            if (remain[i] >= cur.length) {
                empty_cnt++;
                continue scan;
            }

            candidate = cur[remain[i]];
            for (int j = 0; j < nmerge; j++)
                if (tail_contains(to_merge[j], remain[j], candidate)) {
                    continue scan;
                }
            acc.add(candidate);
            for (int j = 0; j < nmerge; j++) {
                if (remain[j] < to_merge[j].length && to_merge[j][remain[j]] == candidate) {
                    remain[j]++;
                }
            }
            // restart scan
            i = -1;
            empty_cnt = 0;
        }
        if (empty_cnt == nmerge) {
            return acc.toArray(bases);
        }
        throw mro_error(to_merge, remain);
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

    private static void addSlot(PyType newtype, PyObject slotname) {
        confirmIdentifier(slotname);
        String slotstring = mangleName(newtype.name, slotname.toString());
        if (slotstring.equals("__dict__")) {
            if (newtype.base.needs_userdict || newtype.needs_userdict) {
                throw Py.TypeError("__dict__ slot disallowed: we already got one");
            }
            newtype.needs_userdict = true;
        } else if (newtype.dict.__finditem__(slotstring) == null) {
            newtype.dict.__setitem__(slotstring, new PySlot(newtype, slotstring,
                                                            newtype.numSlots++));
        }
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
     * INTERNAL lookup for name through mro objects' dicts
     *
     * @param name
     *            attribute name (must be interned)
     * @return found object or null
     */
    public PyObject lookup(String name) {
        PyObject[] mro = this.mro;
        if (mro == null) {
            return null;
        }
        for (PyObject element : mro) {
            PyObject dict = element.fastGetDict();
            if (dict != null) {
                PyObject obj = dict.__finditem__(name);
                if (obj != null)
                    return obj;
            }
        }
        return null;
    }

    public PyObject lookup_where(String name, PyObject[] where) {
        PyObject[] mro = this.mro;
        if (mro == null) {
            return null;
        }
        for (PyObject t : mro) {
            PyObject dict = t.fastGetDict();
            if (dict != null) {
                PyObject obj = dict.__finditem__(name);
                if (obj != null) {
                    where[0] = t;
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Like lookup but also provides (in where[0]) the index of the type in the reversed
     * mro -- that is, how many subtypes away from the base object the type is.
     *
     * @param name attribute name (must be interned)
     * @param where an int[] with a length of at least 1
     * @return found PyObject or null
     */
    public PyObject lookup_where_index(String name, int[] where) {
        PyObject[] mro = this.mro;
        if (mro == null) {
            return null;
        }
        int i = mro.length;
        for (PyObject t : mro) {
            i--;
            PyObject dict = t.fastGetDict();
            if (dict != null) {
                PyObject obj = dict.__finditem__(name);
                if (obj != null) {
                    where[0] = i;
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
            fromClass(builder.getTypeClass()).init();
        }
    }

    private static PyType addFromClass(Class<?> c) {
        if (ExposeAsSuperclass.class.isAssignableFrom(c)) {
            PyType exposedAs = fromClass(c.getSuperclass());
            class_to_type.put(c, exposedAs);
            return exposedAs;
        }
        return createType(c);
    }

    private static TypeBuilder getBuilder(Class<?> c) {
        return classToBuilder == null ? null : classToBuilder.get(c);
    }

    private static PyType createType(Class<?> c) {
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
        newtype.underlying_class = c;
        newtype.builtin = true;
        newtype.init();
        return newtype;
    }

    public static synchronized PyType fromClass(Class<?> c) {
        if (class_to_type == null) {
            class_to_type = Generic.map();
            addFromClass(PyType.class);
        }
        PyType type = class_to_type.get(c);
        if (type != null) {
            return type;
        }
        return addFromClass(c);
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

        if (metaattr != null && useMetatypeFirst(metaattr)) {
            if (metaattr.isDataDescr()) {
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

        if (metaattr != null) {
            return metaattr.__get__(this, metatype);
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

    protected void checkSetattr() {
        if (builtin) {
            throw Py.TypeError(String.format("can't set attributes of built-in/extension type "
                    + "'%s'", this.name));
        }
    }

    final void type___setattr__(String name, PyObject value) {
        checkSetattr();
        super.__setattr__(name, value);
        if (name == "__set__") {
            if (!has_set && lookup("__set__") != null) {
                traverse_hierarchy(false, new OnType() {
                    public boolean onType(PyType type) {
                        boolean old = type.has_set;
                        type.has_set = true;
                        return old;
                    }
                });
            }
        } else if (name == "__delete__") {
            if (!has_delete && lookup("__delete__") != null) {
                traverse_hierarchy(false, new OnType() {
                    public boolean onType(PyType type) {
                        boolean old = type.has_delete;
                        type.has_delete = true;
                        return old;
                    }
                });
            }
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
        if (builtin) {
            throw Py.TypeError(String.format("can't set attributes of built-in/extension type "
                    + "'%s'", this.name));
        }
    }

    final void type___delattr__(String name) {
        checkDelattr();
        super.__delattr__(name);
        if (name == "__set__") {
            if (has_set && lookup("__set__") == null) {
                traverse_hierarchy(false, new OnType() {
                    public boolean onType(PyType type) {
                        boolean absent = type.getDict().__finditem__("__set__") == null;
                        if (absent) {
                            type.has_set = false;
                            return false;
                        }
                        return true;
                    }
                });
            }
        } else if (name == "__delete__") {
            if (has_set && lookup("__delete__") == null) {
                traverse_hierarchy(false, new OnType() {
                    public boolean onType(PyType type) {
                        boolean absent = type.getDict().__finditem__("__delete__") == null;
                        if (absent) {
                            type.has_delete = false;
                            return false;
                        }
                        return true;
                    }
                });
            }
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        return type___call__(args, keywords);
    }

    @ExposedMethod(doc = BuiltinDocs.type___call___doc)
    final PyObject type___call__(PyObject[] args, String[] keywords) {
        PyObject new_ = lookup("__new__");
        if (!instantiable || new_ == null) {
            throw Py.TypeError("cannot create '" + name + "' instances");
        }
        return invoke_new_(new_, this, true, args, keywords);
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
    }

    public void setName(String name) {
        this.name = name;
    }

    @ExposedDelete(name = "__name__")
    public void pyDelName() {
        throw Py.TypeError(String.format("can't delete %s.__name__", name));
    }

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
     * Equivalent of CPython's typeobject type_get_doc; handles __doc__ descriptors.
     */
    public PyObject getDoc() {
        PyObject doc = super.getDoc();
        if (!builtin && doc != null && doc.getType().lookup("__get__") != null) {
            return doc.__get__(null, this);
        }
        return doc;
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

    @ExposedMethod(names = {"__repr__", "__str__"}, doc = BuiltinDocs.type___str___doc)
    public String type_toString() {
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
    private static void confirmIdentifier(PyObject o) {
        String msg = "__slots__ must be identifiers";
        if (o == Py.None) {
            throw Py.TypeError(msg);
        }
        String identifier = o.toString();
        if (identifier == null || identifier.length() < 1
            || (!Character.isLetter(identifier.charAt(0)) && identifier.charAt(0) != '_')) {
            throw Py.TypeError(msg);
        }
        char[] chars = identifier.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isLetterOrDigit(chars[i]) && chars[i] != '_') {
                throw Py.TypeError(msg);
            }
        }
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
        return new TypeResolver(underlying_class, getModule().toString(), name);
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
}
