package org.python.core;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.python.core.util.StringUtil;
import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;

/**
 * The Python Type object implementation.
 *
 */
@ExposedType(name = "type")
public class PyType extends PyObject implements Serializable {

    public static PyType TYPE = fromClass(PyType.class);

    /** The type's name. builtin types include their fully qualified
     * name, e.g.: time.struct_time. */
    private String name;

    /** __base__, the direct base type or null. */
    private PyType base;

    /** __bases__, the base classes. */
    private PyObject[] bases = new PyObject[0];

    /** The real, internal __dict__. */
    private PyObject dict;

    /** __mro__, the method resolution. order */
    private PyObject[] mro = new PyObject[0];

    /** __flags__, the type's options. */
    private long tp_flags;

    /** The underlying java class or null. */
    private Class underlying_class;

    /** Whether it's a builtin type. */
    boolean builtin = false;

    /** Whether new instances of this type can be instantiated */
    private boolean non_instantiable = false;

    /** Whether this type has set/delete descriptors */
    boolean has_set;
    boolean has_delete;

    /** Whether finalization is required for this type's instances
     * (implements __del__). */
    private boolean needs_finalizer;

    /** Whether this type's instances require a __dict__. */
    private boolean needs_userdict = true;

    /** The number of __slots__ defined. */
    private int numSlots;

    private ReferenceQueue<PyType> subclasses_refq = new ReferenceQueue<PyType>();
    private HashSet<WeakReference<PyType>> subclasses = new HashSet<WeakReference<PyType>>();

    private final static Class[] O = {PyObject.class};
    private final static Class[] OO = {PyObject.class, PyObject.class};

    /** Mapping of Java classes to their PyTypes. */
    private static HashMap<Class, PyType> class_to_type;

    /** Mapping of Java classes to their TypeBuilders. */
    private static HashMap<Class, TypeBuilder> classToBuilder;

    private PyType() {
    }

    private PyType(boolean dummy) {
        super(true);
    }

    PyType(PyType subtype) {
        super(subtype);
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
        if (bases_list.length == 0) {
            bases_list = new PyObject[] {object_type};
        }

        // XXX can be subclassed ?
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

        PyType newtype;
        if (new_.for_type == metatype) {
            newtype = new PyType(); // XXX set metatype
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

        PyObject slots = dict.__finditem__("__slots__");
        if (slots != null) {
            newtype.needs_userdict = false;
            if (slots instanceof PyString) {
                addSlot(newtype, slots);
            } else {
                for (PyObject slotname : slots.asIterable()) {
                    addSlot(newtype, slotname);
                }
            }
        }
        if (!newtype.needs_userdict) {
            newtype.needs_userdict = necessitatesUserdict(bases_list);
        }

        newtype.tp_flags = Py.TPFLAGS_HEAPTYPE;

        // special case __new__, if function => static method
        PyObject tmp = dict.__finditem__("__new__");
        if (tmp != null && tmp instanceof PyFunction) { // XXX java functions?
            dict.__setitem__("__new__", new PyStaticMethod(tmp));
        }

        newtype.mro_internal();
        // __dict__ descriptor
        if (newtype.needs_userdict && newtype.lookup("__dict__") == null) {
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

        newtype.has_set = newtype.lookup("__set__") != null;
        newtype.has_delete = newtype.lookup("__delete__") != null;
        newtype.needs_finalizer = newtype.lookup("__del__") != null;

        for (int i = 0; i < bases_list.length; i++) {
            PyObject cur = bases_list[i];
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

    private static void fillFromClass(PyType newtype, String name, Class c, Class base,
                                      TypeBuilder tb) {
        if (base == null) {
            base = c.getSuperclass();
        }
        if (name == null) {
            name = c.getName();
            // Strip the java fully qualified class name (specifically
            // remove org.python.core.Py or fallback to stripping to
            // the last dot)
            if (name.startsWith("org.python.core.Py")) {
                name = name.substring("org.python.core.Py".length()).toLowerCase();
            } else {
                int lastDot = name.lastIndexOf('.');
                if (lastDot != -1) {
                    name = name.substring(lastDot + 1);
                }
            }
        }
        newtype.name = name;
        newtype.underlying_class = c;
        newtype.builtin = true;
        // basic mro, base, bases
        fillInMRO(newtype, base);
        PyObject dict;
        if (tb != null) {
            dict = tb.getDict(newtype);
            newtype.non_instantiable = dict.__finditem__("__new__") == null;
        } else {
            dict = new PyStringMap();
            fillInClassic(c, base, dict);
        }
        if (base != Object.class) {
            if (get_descr_method(c, "__set__", OO) != null || /* backw comp */
            get_descr_method(c, "_doset", OO) != null) {
                newtype.has_set = true;
            }
            if (get_descr_method(c, "__delete__", O) != null || /* backw comp */
            get_descr_method(c, "_dodel", O) != null) {
                newtype.has_delete = true;
            }
        }
        newtype.dict = dict;
    }

    private static void fillInClassic(Class c, Class<?> base, PyObject dict) {
        if (Py.BOOTSTRAP_TYPES.contains(c)) {
            // BOOTSTRAP_TYPES will be filled in by addBuilder later
            return;
        }
        HashMap<String, Object> propnames = new HashMap<String, Object>();
        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method meth = methods[i];
            Class declaring = meth.getDeclaringClass();
            if (declaring != base && base.isAssignableFrom(declaring) && !ignore(meth)) {
                String methname = meth.getName();
                String nmethname = normalize_name(methname);
                PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
                boolean added = false;
                if (reflfunc == null) {
                    dict.__setitem__(nmethname, new PyReflectedFunction(meth));
                    added = true;
                } else {
                    reflfunc.addMethod(meth);
                    added = true;
                }
                if (added && !Modifier.isStatic(meth.getModifiers())) {
                    // check for xxxX.*
                    int n = meth.getParameterTypes().length;
                    if (methname.startsWith("get") && n == 0) {
                        propnames.put(methname.substring(3), "getter");
                    } else if (methname.startsWith("is") && n == 0
                            && meth.getReturnType() == Boolean.TYPE) {
                        propnames.put(methname.substring(2), "getter");
                    } else if (methname.startsWith("set") && n == 1) {
                        propnames.put(methname.substring(3), meth);
                    }
                }
            }
        }
        for (int i = 0; i < methods.length; i++) {
            Method meth = methods[i];
            String nmethname = normalize_name(meth.getName());
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if (reflfunc != null) {
                reflfunc.addMethod(meth);
            }
        }
        Field[] fields = c.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Class declaring = field.getDeclaringClass();
            if (declaring != base && base.isAssignableFrom(declaring)) {
                String fldname = field.getName();
                int fldmods = field.getModifiers();
                Class fldtype = field.getType();
                if (Modifier.isStatic(fldmods)) {
                    if (fldname.startsWith("__doc__") && fldname.length() > 7
                            && fldtype == PyString.class) {
                        String fname = fldname.substring(7).intern();
                        PyObject memb = dict.__finditem__(fname);
                        if (memb != null && memb instanceof PyReflectedFunction) {
                            PyString doc = null;
                            try {
                                doc = (PyString)field.get(null);
                            } catch (IllegalAccessException e) {
                                throw error(e);
                            }
                            ((PyReflectedFunction)memb).__doc__ = doc;
                        }
                    }
                }
                dict.__setitem__(normalize_name(fldname), new PyReflectedField(field));
            }
        }
        for (Iterator iter = propnames.keySet().iterator(); iter.hasNext();) {
            String propname = (String)iter.next();
            String npropname = normalize_name(StringUtil.decapitalize(propname));
            PyObject prev = dict.__finditem__(npropname);
            if (prev != null && prev instanceof PyReflectedFunction) {
                continue;
            }
            Method getter = null;
            Method setter = null;
            Class proptype = null;
            getter = get_non_static_method(c, "get" + propname, new Class[] {});
            if (getter == null)
                getter = get_non_static_method(c, "is" + propname, new Class[] {});
            if (getter != null) {
                proptype = getter.getReturnType();
                setter = get_non_static_method(c, "set" + propname, new Class[] {proptype});
            } else {
                Object o = propnames.get(propname);
                if (o instanceof Method) {
                    setter = (Method)o;
                    proptype = setter.getParameterTypes()[0];
                }
            }
            if (setter != null || getter != null) {
                dict.__setitem__(npropname, new PyBeanProperty(npropname, proptype, getter,
                                                               setter));
            } else {
                // XXX error
            }
        }
        Constructor[] ctrs = c.getConstructors();
        if (ctrs.length != 0) {
            final PyReflectedConstructor reflctr = new PyReflectedConstructor("_new_impl");
            for (int i = 0; i < ctrs.length; i++) {
                reflctr.addConstructor(ctrs[i]);
            }
            PyObject new_ = new PyNewWrapper(c, "__new__", -1, -1) {
                public PyObject new_impl(boolean init, PyType subtype, PyObject[] args,
                                         String[] keywords) {
                    return reflctr.make(args, keywords);
                }
            };
            dict.__setitem__("__new__", new_);
        }
        if (ClassDictInit.class.isAssignableFrom(c) && c != ClassDictInit.class) {
            try {
                @SuppressWarnings("unchecked")
                Method m = c.getMethod("classDictInit", PyObject.class);
                m.invoke(null, new Object[] {dict});
            } catch (Exception exc) {
                throw error(exc);
            }
        }
    }

    private static void fillInMRO(PyType type, Class base) {
        PyType[] mro;
        if (base == Object.class) {
            mro = new PyType[] {type};
        } else {
            PyType baseType = fromClass(base);
            mro = new PyType[baseType.mro.length + 1];
            System.arraycopy(baseType.mro, 0, mro, 1, baseType.mro.length);
            mro[0] = type;
            type.base = baseType;
            type.bases = new PyObject[] {baseType};
        }
        type.mro = mro;
    }

    public PyObject getStatic() {
        PyType cur = this;
        while (cur.underlying_class == null) {
            cur = cur.base;
        }
        return cur;
    }

    /**
     * Ensures that the physical layout between this type and
     * <code>other</code> are compatible. Raises a TypeError if not.
     */
    public void compatibleForAssignment(PyType other, String attribute) {
        if (!getLayout().equals(other.getLayout()) || needs_userdict != other.needs_userdict
            || needs_finalizer != other.needs_finalizer) {
            throw Py.TypeError(String.format("%s assignment: '%s' object layout differs from '%s'",
                                             attribute, other.fastGetName(), fastGetName()));
        }
    }

    /**
     * Gets the most parent PyType that determines the layout of this type ie
     * has slots or an underlying_class.  Can by this PyType.
     */
    private PyType getLayout() {
        if (underlying_class != null) {
            return this;
        } else if (numSlots != base.numSlots) {
            return this;
        }
        return base.getLayout();
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
        List<Object> savedSubMros = new ArrayList<Object>();
        try {
            bases = newBases;
            base = newBase;
            mro_internal();
            mro_subclasses(savedSubMros);
            for (int i = 0; i < savedBases.length; i++) {
                if (savedBases[i] instanceof PyType) {
                    ((PyType)savedBases[i]).detachSubclass(this);
                }
            }
            for (int i = 0; i < newBases.length; i++) {
                if (newBases[i] instanceof PyType) {
                    ((PyType)newBases[i]).attachSubclass(this);
                }
            }
        } catch (PyException t) {
            for (Iterator it = savedSubMros.iterator(); it.hasNext();) {
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

    private void mro_internal() {
        if (getType().underlying_class != PyType.class && getType().lookup("mro") != null) {
            mro = Py.make_array(getType().lookup("mro").__get__(null, getType()).__call__(this));
        } else {
            mro = compute_mro();
        }
    }

    /**
     * Collects the subclasses and current mro of this type in mroCollector. If
     * this type has subclasses C and D, and D has a subclass E current
     * mroCollector will equal [C, C.__mro__, D, D.__mro__, E, E.__mro__] after
     * this call.
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
        Reference ref;
        while ((ref = subclasses_refq.poll()) != null) {
            subclasses.remove(ref);
        }
    }

    @ExposedGet(name = "__mro__")
    public PyTuple getMro() {
        return new PyTuple(mro);
    }

    @ExposedGet(name = "__flags__")
    public PyLong getFlags() {
        return new PyLong(tp_flags);
    }

    @ExposedMethod
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

    private static void fill_classic_mro(ArrayList<PyObject> acc, PyClass classic_cl) {
        if (!acc.contains(classic_cl)) {
            acc.add(classic_cl);
        }
        PyObject[] bases = classic_cl.__bases__.getArray();
        for (int i = 0; i < bases.length; i++) {
            fill_classic_mro(acc,(PyClass)bases[i]);
        }
    }

    private static PyObject[] classic_mro(PyClass classic_cl) {
        ArrayList<PyObject> acc = new ArrayList<PyObject>();
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
        StringBuffer msg = new StringBuffer("Cannot create a consistent method resolution\n"
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

    @ExposedMethod(defaults = "null")
    final PyList type_mro(PyObject o) {
        if (o == null) {
            return new PyList(compute_mro());
        }
        return new PyList(((PyType)o).compute_mro());
    }

    final PyObject[] compute_mro() {
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

        ArrayList<PyObject> acc = new ArrayList<PyObject>();
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
     * Finds the parent of base with an underlying_class or with slots
     *
     * @raises Py.TypeError if there is no solid base for base
     */
    private static PyType solid_base(PyType base) {
        PyObject[] mro = base.mro;
        for (int i = 0; i < mro.length; i++) {
            PyObject parent = mro[i];
            if (parent instanceof PyType) {
                PyType parent_type = (PyType)parent;
                if (isSolidBase(parent_type)) {
                    return parent_type;
                }
            }
        }
        throw Py.TypeError("base without solid base");
    }

    private static boolean isSolidBase(PyType type) {
        return type.underlying_class != null || type.numSlots != 0;
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
        for (int i = 0; i < bases.length; i++) {
            PyObject base_proto = bases[i];
            if (base_proto instanceof PyClass) {
                continue;
            }
            if (!(base_proto instanceof PyType)) {
                throw Py.TypeError("bases must be types");
            }
            PyType base = (PyType)base_proto;
            candidate = solid_base(base);
            if (winner == null) {
                winner = candidate;
                best = base;
            } else if (winner.isSubType(candidate)) {
                ;
            } else if (candidate.isSubType(winner)) {
                winner = candidate;
                best = base;
            } else {
                throw Py.TypeError("multiple bases have instance lay-out conflict");
            }
        }
        if (best == null) {
            throw Py.TypeError("a new-style class can't have only classic bases");
        }
        return best;
    }

    private static boolean necessitatesUserdict(PyObject[] bases_list) {
        for (int i = 0; i < bases_list.length; i++) {
            PyObject cur = bases_list[i];
            if ((cur instanceof PyType && ((PyType)cur).needs_userdict)
                || cur instanceof PyClass) {
               return true;
            }
        }
        return false;
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
        for (int i = 0; i < bases_list.length; i++) {
            PyObject bases_i = bases_list[i];
            if (bases_i instanceof PyJavaClass) {
                throw Py.TypeError("can't mix new-style and java classes");
            }
            if (bases_i instanceof PyClass) {
                if (((PyClass)bases_i).proxyClass != null) {
                    throw Py.TypeError("can't mix new-style and java classes");
                }
                continue;
            }
            PyType curtype = bases_i.getType();
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
            newtype.needs_userdict = true;
        } else if (newtype.dict.__finditem__(slotstring) == null) {
            newtype.dict.__setitem__(slotstring, new PySlot(newtype, slotstring,
                                                            newtype.numSlots++));
        }
    }

    public boolean isSubType(PyType supertype) {
        PyObject[] mro = this.mro;
        for (int i = 0; i < mro.length; i++) {
            if (mro[i] == supertype)
                return true;
        }
        return false;
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
        for (int i = 0; i < mro.length; i++) {
            PyObject dict = mro[i].fastGetDict();
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
        for (int i = 0; i < mro.length; i++) {
            PyObject t = mro[i];
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

    public PyObject super_lookup(PyType ref, String name) {
        PyObject[] mro = this.mro;
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

    private static String normalize_name(String name) {
        if (name.endsWith("$")) {
            name = name.substring(0, name.length() - 1);
        }
        return name.intern();
    }

    private static PyException error(Exception e) {
        return Py.JavaError(e);
    }

    private static Method get_non_static_method(Class<?> c, String name, Class[] parmtypes) {
        try {
            Method meth = c.getMethod(name, parmtypes);
            if (!Modifier.isStatic(meth.getModifiers())) {
                return meth;
            }
        } catch (NoSuchMethodException e) {
            // ok
        }
        return null;
    }

    private static Method get_descr_method(Class c, String name, Class[] parmtypes) {
        Method meth = get_non_static_method(c, name, parmtypes);
        if (meth != null && meth.getDeclaringClass() != PyObject.class) {
            return meth;
        }
        return null;
    }

    private static boolean ignore(Method meth) {
        Class[] exceptions = meth.getExceptionTypes();
        for (int j = 0; j < exceptions.length; j++) {
            if (exceptions[j] == PyIgnoreMethodTag.class) {
                return true;
            }
        }
        return false;
    }

    public static void addBuilder(Class forClass, TypeBuilder builder) {
        if (classToBuilder == null) {
            classToBuilder = new HashMap<Class, TypeBuilder>();
        }
        classToBuilder.put(forClass, builder);

        if (class_to_type.containsKey(forClass)) {
            if (!Py.BOOTSTRAP_TYPES.remove(forClass)) {
                Py.writeWarning("init", "Bootstrapping class not in Py.BOOTSTRAP_TYPES[class="
                                + forClass + "]");
            }
            // The types in Py.BOOTSTRAP_TYPES are initialized before their builders are assigned,
            // so do the work of addFromClass & fillFromClass after the fact
            PyType objType = fromClass(builder.getTypeClass());
            objType.name = builder.getName();
            objType.dict = builder.getDict(objType);
            Class base = builder.getBase();
            if (base == Object.class) {
                base = forClass.getSuperclass();
            }
            fillInMRO(objType, base);
            objType.non_instantiable = objType.dict.__finditem__("__new__") == null;
        }
    }

    private static PyType addFromClass(Class c) {
        if (ExposeAsSuperclass.class.isAssignableFrom(c)) {
            PyType exposedAs = fromClass(c.getSuperclass());
            class_to_type.put(c, exposedAs);
            return exposedAs;
        }
        Class base = null;
        String name = null;
        TypeBuilder tb = classToBuilder == null ? null : classToBuilder.get(c);
        if (tb != null) {
            name = tb.getName();
            if (!tb.getBase().equals(Object.class)) {
                base = tb.getBase();
            }
        }
        PyType newtype = class_to_type.get(c);
        if (newtype == null) {
            newtype = c == PyType.class ? new PyType(true) : new PyType();
            class_to_type.put(c, newtype);
            fillFromClass(newtype, name, c, base, tb);
        }
        return newtype;
    }

    public static synchronized PyType fromClass(Class c) {
        if (class_to_type == null) {
            class_to_type = new HashMap<Class, PyType>();
            addFromClass(PyType.class);
        }
        PyType type = class_to_type.get(c);
        if (type != null) {
            return type;
        }
        return addFromClass(c);
    }

    @ExposedMethod
    final PyObject type___getattribute__(PyObject name) {
        return type___findattr__(asName(name));
    }

    public PyObject __findattr__(String name) {
        return type___findattr__(name);
    }

    // name must be interned
    final PyObject type___findattr__(String name) {
        PyType metatype = getType();

        PyObject metaattr = metatype.lookup(name);
        PyObject res = null;

        if (metaattr != null) {
            if (metaattr.isDataDescr()) {
                res = metaattr.__get__(this, metatype);
                if (res != null)
                    return res;
            }
        }

        PyObject attr = lookup(name);

        if (attr != null) {
            res = attr.__get__(null, this);
            if (res != null) {
                return res;
            }
        }

        if (metaattr != null) {
            return metaattr.__get__(this, metatype);
        }

        return null;
    }

    @ExposedMethod
    final void type___setattr__(PyObject name, PyObject value) {
        type___setattr__(asName(name), value);
    }

    public void __setattr__(String name, PyObject value) {
         type___setattr__(name, value);
    }

    final void type___setattr__(String name, PyObject value) {
        if (builtin) {
            throw Py.TypeError(String.format("can't set attributes of built-in/extension type "
                                             + "'%s'", this.name));
        }
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

    @ExposedMethod
    final void type___delattr__(PyObject name) {
        type___delattr__(asName(name));
    }

    final void type___delattr__(String name) {
        if (builtin) {
            throw Py.TypeError(String.format("can't set attributes of built-in/extension type "
                                             + "'%s'", this.name));
        }
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

    @ExposedMethod
    final PyObject type___call__(PyObject[] args, String[] keywords) {
        PyObject new_ = lookup("__new__");
        if (non_instantiable || new_ == null) {
            throw Py.TypeError("cannot create '" + name + "' instances");
        }
        return invoke_new_(new_, this, true, args, keywords);
    }

    protected void __rawdir__(PyDictionary accum) {
        PyObject[] mro = this.mro;
        for (int i = 0; i < mro.length; i++) {
            mro[i].addKeys(accum, "__dict__");
        }
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

    public Object __tojava__(Class c) {
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

    public String toString() {
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

    private Object writeReplace() {
        // XXX: needed?
        return new TypeResolver(underlying_class, getModule().toString(), name);
    }

    public static interface Newstyle {
    }

    private interface OnType {
        boolean onType(PyType type);
    }

    static class TypeResolver implements Serializable {
        private Class underlying_class;
        private String module;
        private String name;

        TypeResolver(Class underlying_class, String module, String name) {
            this.underlying_class = underlying_class;
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
