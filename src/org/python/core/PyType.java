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

import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;

/**
 * first-class Python type.
 *
 */
@ExposedType(name = "type")
public class PyType extends PyObject implements Serializable {

    @ExposedNew
    public static PyObject type_new(PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        if (args.length == 1 && keywords.length == 0) {
            return args[0].getType();
        }
        if (args.length + keywords.length != 3)
            throw Py.TypeError("type() takes exactly 1 or 3 arguments");
        ArgParser ap = new ArgParser("type()", args, keywords, "name", "bases",
                "dict");
        String name = ap.getString(0);
        PyObject bases = ap.getPyObject(1);
        if (!(bases instanceof PyTuple))
            throw Py.TypeError("type(): bases must be tuple");
        PyObject dict = ap.getPyObject(2);
        if (!(dict instanceof PyDictionary || dict instanceof PyStringMap))
            throw Py.TypeError("type(): dict must be dict");
        return newType(new_, subtype, name, (PyTuple) bases, dict);
    }

    private Object writeReplace() {
        //System.err.println("replace type");
        return new TypeResolver(underlying_class, getModule().toString(), name);
    }
    
    static class TypeResolver implements Serializable   {
        private Class underlying_class;
        private String module;
        private String name;
        
        TypeResolver(Class underlying_class, String module, String name) {
            this.underlying_class = underlying_class;
            this.module = module;
            this.name = name;
        }
        
        private Object readResolve() {
            //System.err.println("resolve: "+module+"."+name);
            if(underlying_class!=null)
                return PyType.fromClass(underlying_class);
            PyObject mod = imp.importName(module.intern(), false);
            PyObject pytyp = mod.__getattr__(name.intern());
            if (!(pytyp instanceof PyType)) {
                throw Py.TypeError(module+"."+name+" must be a type for deserialization");
            }
            return pytyp;
        }
        
    }

    public PyObject getStatic() {
        PyType cur = this;
        while (cur.underlying_class == null) {
            cur = cur.base;
        }
        return cur;
    }
    
    /**
     * Checks that the physical layout between this type and <code>other</code>
     * are compatible.
     */
    public boolean layoutAligns(PyType other) {
        return getLayout().equals(other.getLayout())
                && needs_userdict == other.needs_userdict
                && needs_finalizer == other.needs_finalizer;
    }
    
    /**
     * Gets the most parent PyType that determines the layout of this type ie
     * has slots or an underlying_class.  Can by this PyType.
     */
    private PyType getLayout(){
        if(underlying_class != null){
            return this;
        }else if(numSlots != base.numSlots){
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
        if (bases == null)
            return new PyTuple();
        return new PyTuple(bases);
    }

    @ExposedDelete(name = "__bases__")
    public void delBases() {
        throw Py.TypeError("Can't delete __bases__ attribute");
    }

    @ExposedSet(name = "__bases__")
    public void setBases(PyObject newBasesTuple) {
        if(!(newBasesTuple instanceof PyTuple)){
            throw Py.TypeError("bases must be a tuple");
        }
        PyObject[] newBases = ((PyTuple)newBasesTuple).getArray();
        if (newBases.length == 0) {
            throw Py.TypeError("can only assign non-empty tuple to __bases__, not " + newBasesTuple);
        }
        for(int i = 0; i < newBases.length; i++) {
            if(!(newBases[i] instanceof PyType)){
                if(!(newBases[i] instanceof PyClass)){
                    throw Py.TypeError(name + ".__bases__ must be  a tuple of old- or new-style classes, not " + newBases[i]);
                }
            }else{
                if(((PyType)newBases[i]).isSubType(this)){
                    throw Py.TypeError("a __bases__ item causes an inheritance cycle");
                }
            }
        }
        PyType newBase = best_base(newBases);
        if(!newBase.layoutAligns(base)) {
            throw Py.TypeError("'" + base + "' layout differs from '" + newBase + "'");
        }
        PyObject[] savedBases = bases;
        PyType savedBase = base;
        PyObject[] savedMro = mro;
        List<Object> savedSubMros = new ArrayList<Object>();
        try {
            bases = newBases;
            base = newBase;
            mro_internal();
            mro_subclasses(savedSubMros);
            for(int i = 0; i < savedBases.length; i++) {
                if(savedBases[i] instanceof PyType) {
                    ((PyType)savedBases[i]).detachSubclass(this);
                }
            }
            for(int i = 0; i < newBases.length; i++) {
                if(newBases[i] instanceof PyType) {
                    ((PyType)newBases[i]).attachSubclass(this);
                }
            }
        } catch(PyException t) {
            for(Iterator it = savedSubMros.iterator(); it.hasNext(); ){
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
        if(getType().underlying_class != PyType.class && getType().lookup("mro") != null) {
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
    private void mro_subclasses(List<Object> mroCollector){
        for(WeakReference<PyType> ref : subclasses) {
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

    private String name;
    private PyType base;
    private PyObject[] bases;
    private PyObject dict;
    private PyObject[] mro = new PyObject[0];
    private long tp_flags;
    private Class underlying_class;
    
    boolean builtin = false;

    private boolean non_instantiable = false;

    boolean has_set, has_delete;

    private boolean needs_finalizer;
    private int numSlots;
    private boolean needs_userdict = true;

    private ReferenceQueue<PyType> subclasses_refq = new ReferenceQueue<PyType>();
    private HashSet<WeakReference<PyType>> subclasses = new HashSet<WeakReference<PyType>>();

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
        for(WeakReference<PyType> ref : subclasses) {
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
        for(WeakReference<PyType> ref : subclasses) {
            if(ref.get() == subtype){
                subclasses.remove(ref);
                break;
            }
        }
    }
    
    private interface OnType {
        boolean onType(PyType type);
    }

    private synchronized void traverse_hierarchy(boolean top, OnType behavior) {
        boolean stop = false;
        if (!top) {
            stop = behavior.onType(this);
        }
        if (stop) {
            return;
        }
        for(WeakReference<PyType> ref : subclasses) {
            PyType subtype = ref.get();
            if (subtype == null) {
                continue;
            }
            subtype.traverse_hierarchy(false, behavior);
        }
    }

    private static void fill_classic_mro(ArrayList<PyObject> acc,PyClass classic_cl) {
        if(!acc.contains(classic_cl)) {
            acc.add(classic_cl);
        }
        PyObject[] bases = classic_cl.__bases__.getArray();
        for (int i=0; i <bases.length; i++) {
            fill_classic_mro(acc,(PyClass)bases[i]);
        }
    }

    private static PyObject[] classic_mro(PyClass classic_cl) {
        ArrayList<PyObject> acc = new ArrayList<PyObject>();
        fill_classic_mro(acc, classic_cl);
        return acc.toArray(new PyObject[acc.size()]);
    }

    private static boolean tail_contains(PyObject[] lst,int whence,PyObject o) {
        int n = lst.length;
        for (int i=whence+1; i < n; i++) {
            if (lst[i] == o) {
                return true;
            }
        }
        return false;
    }

    private static PyException mro_error(PyObject[][] to_merge,int[] remain) {
        StringBuffer msg = new StringBuffer("Cannot create a consistent method resolution\n" +
                                            "order (MRO) for bases ");
        PyDictionary set = new PyDictionary();
        for (int i=0; i < to_merge.length; i++) {
            PyObject[] lst = to_merge[i];
            if(remain[i] < lst.length) {
                set.__setitem__(lst[remain[i]],Py.None);
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
            msg.append(name==null?"?":name.toString());
        }
        return Py.TypeError(msg.toString());
    }

    private static void debug(PyObject[] objs) {
        System.out.println(new PyList(objs).toString());
    }
      
    @ExposedMethod(defaults = "null")
    final PyList type_mro(PyObject o) {
        if(o == null) {
            return new PyList(compute_mro());
        }
        return new PyList(((PyType)o).compute_mro());
    }

    final PyObject[] compute_mro() {
        PyObject[] bases = this.bases;
        int n = bases.length;
        for(int i = 0; i < n; i++) {
            PyObject cur = bases[i];
            for(int j = i + 1; j < n; j++) {
                if(bases[j] == cur) {
                    PyObject name = cur.__findattr__("__name__");
                    throw Py.TypeError("duplicate base class "
                            + (name == null ? "?" : name.toString()));
                }
            }
        }

        int nmerge = n+1;
        PyObject[][] to_merge = new PyObject[nmerge][];
        int[] remain = new int[nmerge];

        for(int i = 0; i < n; i++) {
            PyObject cur = bases[i];
            remain[i] = 0;
            if(cur instanceof PyType) {
                to_merge[i] = ((PyType)cur).mro;
            } else if(cur instanceof PyClass) {
                to_merge[i] = classic_mro((PyClass)cur);
            }
        }

        to_merge[n] = bases;
        remain[n] = 0;

        ArrayList<PyObject> acc = new ArrayList<PyObject>();
        acc.add(this);

        int empty_cnt=0;

        scan : for (int i = 0; i < nmerge; i++) {
            PyObject candidate;
            PyObject[] cur = to_merge[i];
            if (remain[i] >= cur.length) {
                empty_cnt++;
                continue scan;
            }

            candidate = cur[remain[i]];
            for (int j = 0; j < nmerge; j++)
                if (tail_contains(to_merge[j],remain[j],candidate))
                    continue scan;
            acc.add(candidate);
            for (int j = 0; j < nmerge; j++) {
                if (remain[j]<to_merge[j].length &&
                    to_merge[j][remain[j]]==candidate)
                    remain[j]++;
            }
            // restart scan
            i = -1;
            empty_cnt = 0;
        }
        if (empty_cnt == nmerge) {
            return acc.toArray(bases);
        }
        throw mro_error(to_merge,remain);
    }

    /**
     * Finds the parent of base with an underlying_class or with slots
     * 
     * @raises Py.TypeError if there is no solid base for base
     */
    private static PyType solid_base(PyType base) {
        PyObject[] mro = base.mro;
        for (int i=0; i<mro.length; i++) {
            PyObject parent = mro[i];
            if (parent instanceof PyType) {
                PyType parent_type =(PyType)parent;
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
        for (int i=0; i < bases.length;i++) {
            PyObject base_proto = bases[i];
            if (base_proto instanceof PyClass)
                continue;
            if (!(base_proto instanceof PyType))
                throw Py.TypeError("bases must be types");
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
        if (best == null)
            throw Py.TypeError("a new-style class can't have only classic bases");
        return best;
    }

    public static PyObject newType(PyNewWrapper new_,PyType metatype,String name,PyTuple bases,PyObject dict) {
        PyType object_type = fromClass(PyObject.class);

        PyObject[] bases_list = bases.getArray();
        PyType winner = findMostDerivedMetatype(bases_list, metatype);
        if (winner != metatype) {
            PyObject winner_new_ = winner.lookup("__new__");
            if (winner_new_ !=null && winner_new_ != new_) {
                return invoke_new_(new_,winner,false,new PyObject[] {new PyString(name),bases,dict},Py.NoKeywords);
            }
            metatype = winner;
        }
        if (bases_list.length == 0) {
            bases_list = new PyObject[] {object_type};
        }

        // xxx can be subclassed ?
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
        // xxx also __doc__ __module__

        PyType newtype;
        if (new_.for_type == metatype) {
            newtype = new PyType(); // xxx set metatype
        } else {
            newtype = new PyTypeDerived(metatype);
        }
        newtype.dict = dict;
        newtype.name = name;
        newtype.base = best_base(bases_list);
        newtype.numSlots = newtype.base.numSlots;
        newtype.bases = bases_list;
        
        PyObject slots = dict.__finditem__("__slots__");
        if(slots != null) {
            newtype.needs_userdict = false;
            if(slots instanceof PyString) {
                addSlot(newtype, slots);
            } else {
                for (PyObject slotname : slots.asIterable()) {
                    addSlot(newtype, slotname);
                }
            }
        }
        if(!newtype.needs_userdict) {
            newtype.needs_userdict = necessitatesUserdict(bases_list);
        }

        newtype.tp_flags=Py.TPFLAGS_HEAPTYPE;

        // special case __new__, if function => static method
        PyObject tmp = dict.__finditem__("__new__");
        if (tmp != null && tmp instanceof PyFunction) { // xxx java functions?
            dict.__setitem__("__new__",new PyStaticMethod(tmp));
        }
        
        newtype.mro_internal();
        // __dict__ descriptor
        if (newtype.needs_userdict && newtype.lookup("__dict__")==null) {
            dict.__setitem__("__dict__",new PyGetSetDescr(newtype,"__dict__",PyObject.class,"getDict","setDict","delDict"));
        }

        newtype.has_set = newtype.lookup("__set__") != null;
        newtype.has_delete = newtype.lookup("__delete__") != null;
        newtype.needs_finalizer = newtype.lookup("__del__") != null;

        for (int i=0; i<bases_list.length;i++) {
            PyObject cur = bases_list[i];
            if (cur instanceof PyType)
                ((PyType)cur).attachSubclass(newtype);
        }
        return newtype;
    }

    private static boolean necessitatesUserdict(PyObject[] bases_list) {
        for(int i = 0; i < bases_list.length; i++) {
            PyObject cur = bases_list[i];
            if((cur instanceof PyType && ((PyType)cur).needs_userdict && ((PyType)cur).numSlots > 0)
                    || cur instanceof PyClass) {
               return true;
            }
        }
        return false;
    }

    /**
     * Finds the most derived subtype of initialMetatype in the types of bases, or initialMetatype if 
     * it is already the most derived.
     * 
     * @raises Py.TypeError if the all the metaclasses don't descend from the same base
     * @raises Py.TypeError if one of the bases is a PyJavaClass or a PyClass with no proxyClass 
     */
    private static PyType findMostDerivedMetatype(PyObject[] bases_list, PyType initialMetatype) {
        PyType winner = initialMetatype;
        for (int i=0; i<bases_list.length; i++) {
            PyObject bases_i = bases_list[i];
            if (bases_i instanceof PyJavaClass)
                throw Py.TypeError("can't mix new-style and java classes");
            if (bases_i instanceof PyClass) {
                if (((PyClass)bases_i).proxyClass != null)
                    throw Py.TypeError("can't mix new-style and java classes");
                continue;
            }
            PyType curtype = bases_i.getType();
            if (winner.isSubType(curtype))
                continue;
            if (curtype.isSubType(winner)) {
                winner = curtype;
                continue;
            }
            throw Py.TypeError("metaclass conflict: "+
                "the metaclass of a derived class "+
                "must be a (non-strict) subclass "+
                "of the metaclasses of all its bases");
        }
        return winner;
    }

    private static void addSlot(PyType newtype, PyObject slotname) {
        confirmIdentifier(slotname);
        String slotstring = mangleName(newtype.name, slotname.toString());
        if(slotstring.equals("__dict__")) {
            newtype.needs_userdict = true;
        } else {
            newtype.dict.__setitem__(slotstring, new PySlot(newtype,
                                                    slotstring,
                                                    newtype.numSlots++));
        }
    }


    @ExposedGet(name = "__name__")
    public String fastGetName() {
        return name;
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
        for(int i = 0; i < mro.length; i++) {
            PyObject dict = mro[i].fastGetDict();
            if(dict != null) {
                PyObject obj = dict.__finditem__(name);
                if(obj != null)
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

    public PyObject super_lookup(PyType ref,String name) {
        PyObject[] mro = this.mro;
        int i;
        for (i=0; i < mro.length; i++) {
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

    private PyType(boolean dummy) {
        super(true);
    }

    private PyType() {
    }

    PyType(PyType subtype) {
        super(subtype);
    }

    private static String decapitalize(String s) {
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

    private static String normalize_name(String name) {
        if (name.endsWith("$"))
            name = name.substring(0, name.length() - 1);
        return name.intern();
    }

    private static Object exposed_decl_get_object(Class c, String name) {
        try {
            return c.getDeclaredField("exposed_" + name).get(null);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (Exception e) {
            throw error(e);
        }
    }

    private static PyException error(Exception e) {
        return Py.JavaError(e);
    }

    private static Method get_non_static_method(
        Class c,
        String name,
        Class[] parmtypes) {
        try {
            Method meth = c.getMethod(name, parmtypes);
            if (!Modifier.isStatic(meth.getModifiers()))
                return meth;
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private static Method get_descr_method(Class c, String name, Class[] parmtypes) {
        Method meth = get_non_static_method(c, name, parmtypes);
        if(meth != null && meth.getDeclaringClass() != PyObject.class) {
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

    private final static Class[] O = { PyObject.class };
    private final static Class[] OO = { PyObject.class, PyObject.class };

    private static void fillFromClass(PyType newtype,
                                      String name,
                                      Class c,
                                      Class base,
                                      boolean newstyle,
                                      Method setup,
                                      TypeBuilder tb) {

        if(base == null) {
            base = c.getSuperclass();
        }
        if(name == null) {
            name = c.getName();
        }
        if(name.startsWith("org.python.core.Py")) {
            name = name.substring("org.python.core.Py".length()).toLowerCase();
        } else {
            int lastdot = name.lastIndexOf('.');
            if(lastdot != -1) {
                name = name.substring(lastdot + 1);
            }
        }
        newtype.name = name;
        newtype.underlying_class = c;
        newtype.builtin = true;
        boolean top = false;
        // basic mro, base, bases
        PyType[] mro;
        if(base == Object.class) {
            mro = new PyType[] {newtype};
            top = true;
        } else {
            PyType basetype = fromClass(base);
            mro = new PyType[basetype.mro.length + 1];
            System.arraycopy(basetype.mro, 0, mro, 1, basetype.mro.length);
            mro[0] = newtype;
            newtype.base = basetype;
            newtype.bases = new PyObject[] {basetype};
        }
        newtype.mro = mro;
        PyObject dict;
        if(tb != null) {
            dict = tb.getDict(newtype);
        } else {
            dict = new PyStringMap();
            if(newstyle) {
                fillInNewstyle(newtype, setup, dict);
            } else {
                fillInClassic(c, base, dict);
            }
        }
        if(newstyle) {
            newtype.non_instantiable = dict.__finditem__("__new__") == null;
        }
        if(!top) {
            if(get_descr_method(c, "__set__", OO) != null || /* backw comp */
            get_descr_method(c, "_doset", OO) != null) {
                newtype.has_set = true;
            }
            if(get_descr_method(c, "__delete__", O) != null || /* backw comp */
            get_descr_method(c, "_dodel", O) != null) {
                newtype.has_delete = true;
            }
        }
        newtype.dict = dict;
    }

    private static void fillInClassic(Class c, Class base, PyObject dict) {
        HashMap propnames = new HashMap();
        Method[] methods = c.getMethods();
        for(int i = 0; i < methods.length; i++) {
            Method meth = methods[i];
            Class declaring = meth.getDeclaringClass();
            if(declaring != base && base.isAssignableFrom(declaring)
                    && !ignore(meth)) {
                String methname = meth.getName();
                String nmethname = normalize_name(methname);
                PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
                boolean added = false;
                if(reflfunc == null) {
                    dict.__setitem__(nmethname, new PyReflectedFunction(meth));
                    added = true;
                } else {
                    reflfunc.addMethod(meth);
                    added = true;
                }
                if(added && !Modifier.isStatic(meth.getModifiers())) {
                    // check for xxxX.*
                    int n = meth.getParameterTypes().length;
                    if(methname.startsWith("get") && n == 0) {
                        propnames.put(methname.substring(3), "getter");
                    } else if(methname.startsWith("is") && n == 0
                            && meth.getReturnType() == Boolean.TYPE) {
                        propnames.put(methname.substring(2), "getter");
                    } else if(methname.startsWith("set") && n == 1) {
                        propnames.put(methname.substring(3), meth);
                    }
                }
            }
        }
        for(int i = 0; i < methods.length; i++) {
            Method meth = methods[i];
            String nmethname = normalize_name(meth.getName());
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if(reflfunc != null) {
                reflfunc.addMethod(meth);
            }
        }
        Field[] fields = c.getFields();
        for(int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Class declaring = field.getDeclaringClass();
            if(declaring != base && base.isAssignableFrom(declaring)) {
                String fldname = field.getName();
                int fldmods = field.getModifiers();
                Class fldtype = field.getType();
                if(Modifier.isStatic(fldmods)) {
                    // ignore static PyClass __class__
                    if(fldname.equals("__class__") && fldtype == PyClass.class) {
                        continue;
                    } else if(fldname.startsWith("__doc__")
                            && fldname.length() > 7
                            && fldtype == PyString.class) {
                        String fname = fldname.substring(7).intern();
                        PyObject memb = dict.__finditem__(fname);
                        if(memb != null && memb instanceof PyReflectedFunction) {
                            PyString doc = null;
                            try {
                                doc = (PyString)field.get(null);
                            } catch(IllegalAccessException e) {
                                throw error(e);
                            }
                            ((PyReflectedFunction)memb).__doc__ = doc;
                        }
                    }
                }
                dict.__setitem__(normalize_name(fldname),
                                 new PyReflectedField(field));
            }
        }
        for(Iterator iter = propnames.keySet().iterator(); iter.hasNext();) {
            String propname = (String)iter.next();
            String npropname = normalize_name(decapitalize(propname));
            PyObject prev = dict.__finditem__(npropname);
            if(prev != null && prev instanceof PyReflectedFunction) {
                continue;
            }
            Method getter = null;
            Method setter = null;
            Class proptype = null;
            getter = get_non_static_method(c, "get" + propname, new Class[] {});
            if(getter == null)
                getter = get_non_static_method(c,
                                               "is" + propname,
                                               new Class[] {});
            if(getter != null) {
                proptype = getter.getReturnType();
                setter = get_non_static_method(c,
                                               "set" + propname,
                                               new Class[] {proptype});
            } else {
                Object o = propnames.get(propname);
                if(o instanceof Method) {
                    setter = (Method)o;
                    proptype = setter.getParameterTypes()[0];
                }
            }
            if(setter != null || getter != null) {
                dict.__setitem__(npropname, new PyBeanProperty(npropname,
                                                               proptype,
                                                               getter,
                                                               setter));
            } else {
                // xxx error
            }
        }
        Constructor[] ctrs = c.getConstructors();
        if(ctrs.length != 0) {
            final PyReflectedConstructor reflctr = new PyReflectedConstructor("_new_impl");
            for(int i = 0; i < ctrs.length; i++) {
                reflctr.addConstructor(ctrs[i]);
            }
            PyObject new_ = new PyNewWrapper(c, "__new__", -1, -1) {

                public PyObject new_impl(boolean init,
                                         PyType subtype,
                                         PyObject[] args,
                                         String[] keywords) {
                    return reflctr.make(args, keywords);
                }
            };
            dict.__setitem__("__new__", new_);
        }
        if(ClassDictInit.class.isAssignableFrom(c) && c != ClassDictInit.class) {
            try {
                Method m = c.getMethod("classDictInit",
                                       new Class[] {PyObject.class});
                m.invoke(null, new Object[] {dict});
            } catch(Exception exc) {
                throw error(exc);
            }
        }
    }

    private static void fillInNewstyle(PyType newtype,
                                       Method setup,
                                       PyObject dict) {
        if(setup != null) {
            try {
                setup.invoke(null, new Object[] {dict, null});
            } catch(Exception e) {
                e.printStackTrace();
                throw error(e);
            }
        }
    }

    private static HashMap<Class, PyType> class_to_type;

    private static HashMap<Class, TypeBuilder> classToBuilder;

    public static interface Newstyle {
    }

    public static void addBuilder(Class forClass, TypeBuilder builder) {
        if(classToBuilder == null) {
            classToBuilder = new HashMap<Class, TypeBuilder>();
        }
        classToBuilder.put(forClass, builder);
        
        if(class_to_type.containsKey(forClass)) {
            // PyObject and PyType are loaded as part of creating their
            // builders, so they need to be bootstrapped
            PyType objType = fromClass(builder.getTypeClass());
            objType.name = builder.getName();
            objType.dict = builder.getDict(objType);
        }
    }

    private static PyType addFromClass(Class c) {
        if(ExposeAsSuperclass.class.isAssignableFrom(c)) {
            PyType exposedAs = fromClass(c.getSuperclass());
            class_to_type.put(c, exposedAs);
            return exposedAs;
        }
        Method setup = null;
        boolean newstyle = Newstyle.class.isAssignableFrom(c);
        Class base = null;
        String name = null;
        TypeBuilder tb = classToBuilder == null ? null : classToBuilder.get(c);
        if(tb != null) {
            name = tb.getName();
            if(!tb.getBase().equals(Object.class)) {
                base = tb.getBase(); 
            }
            newstyle = true;
        } else {
            try {
                setup = c.getDeclaredMethod("typeSetup", new Class[] {PyObject.class,
                                                                      Newstyle.class});
                newstyle = true;
            } catch(NoSuchMethodException e) {} catch(Exception e) {
                throw error(e);
            }
            if(newstyle) { // newstyle
                base = (Class)exposed_decl_get_object(c, "base");
                name = (String)exposed_decl_get_object(c, "name");
            }
        }
        PyType newtype = class_to_type.get(c);
        if (newtype == null) {
            newtype = c == PyType.class ? new PyType(true) : new PyType();
            class_to_type.put(c, newtype);
            fillFromClass(newtype, name, c, base, newstyle, setup, tb);
        }
        return newtype;
    }

    public static PyType TYPE = fromClass(PyType.class);

    /*
     * considers:
     *   if c implements Newstyle => c and all subclasses
     *    are considered newstyle
     *
     *   if c has static typeSetup(PyObject dict, Newstyle marker)
     *   => c is considired newstyle, subclasses are not automatically;
     *    typeSetup is invoked to populate dict which will become
     *    type's __dict__
     *
     *   Class exposed_base
     *   String exposed_name
     *
     */

    public static synchronized PyType fromClass(Class c) {
        if (class_to_type == null) {
            class_to_type = new HashMap<Class, PyType>();
            addFromClass(PyType.class);
        }
        PyType type = class_to_type.get(c);
        if (type != null)
            return type;
        return addFromClass(c);
    }

    @ExposedMethod
    final PyObject type___getattribute__(PyObject name){
        return type___findattr__(asName(name));
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
            if (res != null)
                return res;
        }

        if (metaattr != null) {
            return metaattr.__get__(this, metatype);
        }

        return null;
    }
    
    @ExposedMethod
    final void type___setattr__(PyObject name, PyObject value){
        type___setattr__(asName(name), value);
    }

    final void type___setattr__(String name, PyObject value) {
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

    @ExposedMethod
    final void type___delattr__(PyObject name) {
        type___delattr__(asName(name));
    }
    
    final void type___delattr__(String name) {
        super.__delattr__(name);
        if (name == "__set__") {
            if (has_set && lookup("__set__") == null) {
                traverse_hierarchy(false, new OnType() {
                    public boolean onType(PyType type) {
                        boolean absent =
                            type.getDict().__finditem__("__set__") == null;
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
                        boolean absent =
                            type.getDict().__finditem__("__delete__") == null;
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

    protected void __rawdir__(PyDictionary accum) {
        PyObject[] mro = this.mro;
        for (int i = 0; i < mro.length; i++) {
            mro[i].addKeys(accum, "__dict__");
        }
    }

    /**
     * @see org.python.core.PyObject#fastGetDict()
     */
    public PyObject fastGetDict() {
        return dict;
    }


    @ExposedGet(name = "__dict__")
    public PyObject getDict() { // xxx return dict-proxy
        return dict;
    }
    
    @ExposedSet(name = "__dict__")
    public void setDict(PyObject newDict) {
    	throw Py.TypeError("can't set attribute '__dict__' of type '" + name + "'");
    }

    @ExposedDelete(name = "__dict__")
    public void delDict() {
    	throw Py.TypeError("can't delete attribute '__dict__' of type '" + name + "'");
    }

    public Object __tojava__(Class c) {
        if (underlying_class!= null &&(
            c == Object.class || c == Class.class || c == Serializable.class)) {
            return underlying_class;
        }
        return super.__tojava__(c);
    }

    public PyObject getModule() {
        if (underlying_class != null)
            return new PyString("__builtin__");
        return dict.__finditem__("__module__");
    }
    
    public int getNumSlots(){
        return numSlots;
    }

    public String getFullName () {
        if (underlying_class != null)
            return name;
        PyObject mod = getModule();
        if (mod != null)
            return mod.__str__()+"."+name;
        return name;
    }

    public String toString() {
        if (underlying_class != null)
            return "<type '" + name + "'>";
        return "<class '" + getFullName() + "'>";
    }

    /**
     * @see org.python.core.PyObject#__findattr__(java.lang.String)
     */
    public PyObject __findattr__(String name) {
        return type___findattr__(name);
    }

    /**
     * @see org.python.core.PyObject#__delattr__(java.lang.String)
     */
    public void __delattr__(String name) {
        type___delattr__(name);
    }

    /**
     * @see org.python.core.PyObject#__setattr__(java.lang.String, org.python.core.PyObject)
     */
    public void __setattr__(String name, PyObject value) {
         type___setattr__(name, value);
    }

    /**
     * @see org.python.core.PyObject#safeRepr()
     */
    public String safeRepr() throws PyIgnoreMethodTag {
        return "type object '" + name + "'"; // xxx use fullname
    }

    private static PyObject invoke_new_(PyObject new_,PyType type,boolean init,PyObject[] args,String[] keywords) {
        PyObject newobj;
        if (new_ instanceof PyNewWrapper) {
            newobj = ((PyNewWrapper) new_).new_impl(init, type, args, keywords);
        } else {
            int n = args.length;
            PyObject[] type_prepended = new PyObject[n + 1];
            System.arraycopy(args, 0, type_prepended, 1, n);
            type_prepended[0] = type;
            newobj = new_.__get__(null, type).__call__(type_prepended, keywords);
        }
        /* special case type(x) */
        if (type == TYPE && args.length==1 && keywords.length==0) {
            return newobj;
        }
        newobj.dispatch__init__(type,args,keywords);
        return newobj;
    }


    /**
     * @see org.python.core.PyObject#__call__(org.python.core.PyObject[], java.lang.String[])
     */
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return type___call__(args,keywords);
    }

    @ExposedMethod
    final PyObject type___call__(PyObject[] args, String[] keywords) {
        PyObject new_ = lookup("__new__");
        if (non_instantiable || new_ == null) {
            throw Py.TypeError("cannot create '" + name + "' instances");
            // xxx fullname
        }

        return invoke_new_(new_,this,true,args,keywords);
    }
    //XXX: consider pulling this out into a generally accessible place
    //     I bet this is duplicated more or less in other places.
    private static void confirmIdentifier(PyObject o) {
        String msg = "__slots__ must be identifiers";
        if (o == Py.None) {
            throw Py.TypeError(msg);
        }
        String identifier = o.toString();
        if (identifier == null ||
            identifier.length() < 1 ||
            (!Character.isLetter(identifier.charAt(0)) && identifier.charAt(0) != '_')
        ) {
            throw Py.TypeError(msg);
        }
        char[] chars = identifier.toCharArray();
        for(int i = 0; i < chars.length; i++) {
            if (!Character.isLetterOrDigit(chars[i]) && chars[i] != '_') {
                throw Py.TypeError(msg);
            }
        }
    }

    //XXX: copied from CodeCompiler.java and changed variable names.
    //       Maybe this should go someplace for all classes to use.
    private static String mangleName(String classname, String methodname) {
        if (classname != null && methodname.startsWith("__") &&
            !methodname.endsWith("__"))
        {
            //remove leading '_' from classname
            int i = 0;
            while (classname.charAt(i) == '_')
                i++;
            return ("_"+classname.substring(i)+methodname).intern();
        }
        return methodname;
    }

}
