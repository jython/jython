package org.python.core;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * first-class Python type.
 *
 */
public class PyType extends PyObject implements Serializable {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="type";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("__dict__",new PyGetSetDescr("__dict__",PyType.class,"getDict",null));
        dict.__setitem__("__name__",new PyGetSetDescr("__name__",PyType.class,"fastGetName",null));
        dict.__setitem__("__base__",new PyGetSetDescr("__base__",PyType.class,"getBase",null));
        dict.__setitem__("__bases__",new PyGetSetDescr("__bases__",PyType.class,"getBases",null));
        dict.__setitem__("__mro__",new PyGetSetDescr("__mro__",PyType.class,"getMro",null));
        dict.__setitem__("__flags__",new PyGetSetDescr("__flags__",PyType.class,"getFlags",null));
        class exposed_mro extends PyBuiltinFunctionNarrow {

            private PyType self;

            public PyObject getSelf() {
                return self;
            }

            exposed_mro(PyType self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_mro((PyType)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.type_mro(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyType self=(PyType)gself;
                return self.type_mro(arg0);
            }

            public PyObject __call__() {
                return self.type_mro();
            }

            public PyObject inst_call(PyObject gself) {
                PyType self=(PyType)gself;
                return self.type_mro();
            }

        }
        dict.__setitem__("mro",new PyClassMethod(new PyMethodDescr("mro",PyType.class,0,1,new exposed_mro(null,null))));
        class exposed___getattribute__ extends PyBuiltinFunctionNarrow {

            private PyType self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getattribute__(PyType self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getattribute__((PyType)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    String name=(arg0.asName(0));
                    PyObject ret=self.type___findattr__(name);
                    if (ret==null)
                        self.noAttributeError(name);
                    return ret;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyType self=(PyType)gself;
                try {
                    String name=(arg0.asName(0));
                    PyObject ret=self.type___findattr__(name);
                    if (ret==null)
                        self.noAttributeError(name);
                    return ret;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("__getattribute__",new PyMethodDescr("__getattribute__",PyType.class,1,1,new exposed___getattribute__(null,null)));
        class exposed___setattr__ extends PyBuiltinFunctionNarrow {

            private PyType self;

            public PyObject getSelf() {
                return self;
            }

            exposed___setattr__(PyType self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___setattr__((PyType)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    self.type___setattr__(arg0.asName(0),arg1);
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyType self=(PyType)gself;
                try {
                    self.type___setattr__(arg0.asName(0),arg1);
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("__setattr__",new PyMethodDescr("__setattr__",PyType.class,2,2,new exposed___setattr__(null,null)));
        class exposed___delattr__ extends PyBuiltinFunctionNarrow {

            private PyType self;

            public PyObject getSelf() {
                return self;
            }

            exposed___delattr__(PyType self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___delattr__((PyType)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    self.type___delattr__(arg0.asName(0));
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyType self=(PyType)gself;
                try {
                    self.type___delattr__(arg0.asName(0));
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("__delattr__",new PyMethodDescr("__delattr__",PyType.class,1,1,new exposed___delattr__(null,null)));
        class exposed___subclasses__ extends PyBuiltinFunctionNarrow {

            private PyType self;

            public PyObject getSelf() {
                return self;
            }

            exposed___subclasses__(PyType self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___subclasses__((PyType)self,info);
            }

            public PyObject __call__() {
                return self.type_getSubclasses();
            }

            public PyObject inst_call(PyObject gself) {
                PyType self=(PyType)gself;
                return self.type_getSubclasses();
            }

        }
        dict.__setitem__("__subclasses__",new PyMethodDescr("__subclasses__",PyType.class,0,0,new exposed___subclasses__(null,null)));
        class exposed___call__ extends PyBuiltinFunctionWide {

            private PyType self;

            public PyObject getSelf() {
                return self;
            }

            exposed___call__(PyType self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___call__((PyType)self,info);
            }

            public PyObject inst_call(PyObject self,PyObject[]args) {
                return inst_call(self,args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                return self.type___call__(args,keywords);
            }

            public PyObject inst_call(PyObject gself,PyObject[]args,String[]keywords) {
                PyType self=(PyType)gself;
                return self.type___call__(args,keywords);
            }

        }
        dict.__setitem__("__call__",new PyMethodDescr("__call__",PyType.class,-1,-1,new exposed___call__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyType.class,"__new__",-1,-1) {

                                                                                      public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                          return type_new(this,init,subtype,args,keywords);
                                                                                      }

                                                                                  });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

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
            return (PyType)pytyp;
        }
        
    }

    public PyObject getStatic() {
        PyType cur = this;
        while (cur.underlying_class == null) {
            cur = cur.base;
        }
        return cur;
    }

    public PyObject getBase() {
        if (base == null)
            return Py.None;
        return base;
    }

    public PyObject getBases() {
        if (bases == null)
            return new PyTuple();
        return new PyTuple(bases);
    }

    public PyObject instDict() {
        if (needs_userdict) {
            return new PyStringMap();
        }
        return null;
    }

    public List getSlotnames() {
        return slotnames;
    }


    private String name;
    private PyType base;
    private PyObject[] bases;
    private PyObject dict;
    private PyObject[] mro;
    private long tp_flags;
    private Class underlying_class;
    private List slotnames;

    private boolean non_instantiable = false;

    boolean has_set, has_delete, hide_dict;

    private boolean needs_finalizer;

    private int nuserslots;
    private boolean needs_userdict;

    private java.lang.ref.ReferenceQueue subclasses_refq = new java.lang.ref.ReferenceQueue();
    private java.util.HashSet subclasses = new java.util.HashSet();

    private void cleanup_subclasses() {
        java.lang.ref.Reference ref;
        while ((ref = subclasses_refq.poll()) != null) {
            subclasses.remove(ref);
        }
    }

    public PyTuple getMro() {
        return new PyTuple(mro);
    }

    public PyLong getFlags() {
        return new PyLong(tp_flags);
    }
    
    public synchronized final PyObject type_getSubclasses() {
        PyList result = new PyList();
        cleanup_subclasses();
        for (java.util.Iterator iter =subclasses.iterator(); iter.hasNext();) {
            java.lang.ref.WeakReference type_ref = (java.lang.ref.WeakReference)iter.next();
            PyType subtype = (PyType)type_ref.get();
            if (subtype == null)
                continue;
            result.append(subtype);
        }
        return result;
    }

    private synchronized void attachSubclass(PyType subtype) {
        cleanup_subclasses();
        subclasses.add(
            new java.lang.ref.WeakReference(subtype, subclasses_refq));
    }

    private interface OnType {
        boolean onType(PyType type);
    }

    private synchronized void traverse_hierarchy(boolean top, OnType behavior) {
        boolean stop = false;
        if (!top) {
            stop = behavior.onType(this);
        }
        if (stop)
            return;
        for (java.util.Iterator iter = subclasses.iterator();
            iter.hasNext();
            ) {
            java.lang.ref.WeakReference type_ref =
                (java.lang.ref.WeakReference) iter.next();
            PyType subtype = (PyType) type_ref.get();
            if (subtype == null)
                continue;
            subtype.traverse_hierarchy(false, behavior);
        }
    }

    private static void fill_classic_mro(ArrayList acc,PyClass classic_cl) {
        if(!acc.contains(classic_cl))
            acc.add(classic_cl);
        PyObject[] bases = classic_cl.__bases__.getArray();
        for (int i=0; i <bases.length; i++) {
            fill_classic_mro(acc,(PyClass)bases[i]);
        }
    }

    private static PyObject[] classic_mro(PyClass classic_cl) {
        ArrayList acc = new ArrayList();
        fill_classic_mro(acc, classic_cl);
        return (PyObject[])acc.toArray(new PyObject[0]);
    }

    private static boolean tail_contains(PyObject[] lst,int whence,PyObject o) {
        int n = lst.length;
        for (int i=whence+1; i < n; i++) {
            if (lst[i] == o)
                return true;
        }
        return false;
    }

    private static PyException mro_error(PyObject[][] to_merge,int[] remain) {
        StringBuffer msg = new StringBuffer("Cannot create a"+
            " consistent method resolution\norder (MRO) for bases ");
        PyDictionary set = new PyDictionary();
        for (int i=0; i < to_merge.length; i++) {
            PyObject[] lst = to_merge[i];
            if(remain[i] < lst.length)
                set.__setitem__(lst[remain[i]],Py.None);
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

      
    final PyTuple type_mro() {
        return getMro();
    }

    final PyTuple type_mro(PyObject o) {
	//FIXME: PyMethDescr should be gaurding against args that are not the
	//       correct type in the generated code, but that is not working.
	//       fix and delete this instanceof check.
	if (!(o instanceof PyType)) {
	    throw Py.TypeError(
            "descriptor 'mro' requires a 'type' object but received a '"
                + o.getType().fastGetName()
                + "'");
	}
	PyType type = (PyType)o;
	return type.type_mro();
    }

    final PyObject[] compute_mro() {
        PyObject[] bases = this.bases;
        int n = bases.length;
        for (int i=0; i < n; i++) {
            PyObject cur = bases[i];
            for (int j = i+1; j<n; j++) {
                if (bases[j] == cur) {
                    PyObject name = cur.__findattr__("__name__");
                    throw Py.TypeError("duplicate base class " +
                        (name==null?"?":name.toString()));
                }
            }
        }

        int nmerge = n+1;

        PyObject[][] to_merge = new PyObject[nmerge][];
        int[] remain = new int[nmerge];

        for (int i=0; i < n; i++) {
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

        ArrayList acc = new ArrayList();
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
            return (PyObject[])acc.toArray(bases);
        }
        throw mro_error(to_merge,remain);
    }

    private static PyType solid_base(PyType base) {
        PyObject[] mro = base.mro;
        for (int i=0; i<mro.length; i++) {
            PyObject parent = mro[i];
            if (parent instanceof PyType) {
                PyType parent_type =(PyType)parent;
                if (parent_type.underlying_class != null || parent_type.nuserslots != 0)
                    return parent_type;
            }
        }
        throw Py.TypeError("base without solid base");
    }

    private static PyType best_base(PyObject[] bases_list) {
        PyType winner=null;
        PyType candidate=null;
        PyType base=null;
        for (int i=0; i < bases_list.length;i++) {
            PyObject base_proto = bases_list[i];
            if (base_proto instanceof PyClass)
                continue;
            if (!(base_proto instanceof PyType))
                throw Py.TypeError("bases must be types");
            PyType base_i = (PyType)base_proto;
            candidate = solid_base(base_i);
            if (winner == null) {
                winner = candidate;
                base = base_i;
            } else if (winner.isSubType(candidate)) {
                ;
            } else if (candidate.isSubType(winner)) {
                winner = candidate;
                base = base_i;
            } else {
                throw Py.TypeError("multiple bases have instance lay-out conflict");
            }
        }
        if (base == null)
            throw Py.TypeError("a new-style class can't have only classic bases");
        return base;
    }

    public static PyObject newType(PyNewWrapper new_,PyType metatype,String name,PyTuple bases,PyObject dict) {
        PyType object_type = fromClass(PyObject.class);

        PyObject[] bases_list = bases.getArray();
        PyType winner = metatype;
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

        PyType base = best_base(bases_list);

        // xxx can be subclassed ?

        boolean needs_userdict = base.needs_userdict;
        if (!needs_userdict) {
            for (int i=0; i<bases_list.length;i++) {
                PyObject cur = bases_list[i];
                if (cur != base) {
                    if ((cur instanceof PyType && ((PyType)cur).needs_userdict) || cur instanceof PyClass) {
                        needs_userdict = true;
                        break;
                    }
                }
            }
        }

        int nuserslots = base.nuserslots;

        needs_userdict = true;

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
        List slotnames = null;
        boolean hide_dict = false;

        PyObject slots = dict.__finditem__("__slots__");
        if (slots != null) {
            hide_dict = true;
            if (base.nuserslots > 0) {
                nuserslots = base.nuserslots;
                slotnames = new ArrayList(base.getSlotnames());
            }
            else {
                slotnames = new ArrayList();
            }
            PyObject iter = slots.__iter__();
            PyObject slotname;
            for (; (slotname = iter.__iternext__())!= null; ) {
                confirmIdentifier(slotname);
                String slotstring = slotname.toString();
                if (slotstring.equals("__dict__")) {
                    hide_dict = false;
                }
                slotnames.add(mangleName(name, slotstring));
                nuserslots += 1;
            }
        }

        PyType newtype;
        if (new_.for_type == metatype) {
            newtype = new PyType(); // xxx set metatype
        } else {
            newtype = new PyTypeDerived(metatype);
        }

        newtype.name = name;
        newtype.base = base;
        newtype.bases = bases_list;

        /* initialize tp flags */
        newtype.tp_flags=Py.TPFLAGS_HEAPTYPE;

        newtype.needs_userdict = needs_userdict;
        newtype.nuserslots = nuserslots;
        newtype.hide_dict = hide_dict;

        newtype.dict = dict;

        newtype.slotnames = slotnames;

        // special case __new__, if function => static method
        PyObject tmp = dict.__finditem__("__new__");
        if (tmp != null && tmp instanceof PyFunction) { // xxx java functions?
            dict.__setitem__("__new__",new PyStaticMethod(tmp));
        }

        PyObject mro_meth = null;
        PyObject[] newmro;

        if (metatype.underlying_class != PyType.class)
            mro_meth = metatype.lookup("mro");

        if (mro_meth == null) {
            newmro = newtype.compute_mro();
        } else {
            newmro = Py.make_array(mro_meth.__get__(newtype,metatype).__call__());
        }

        newtype.mro = newmro;

        // __dict__ descriptor
        if (needs_userdict && newtype.lookup("__dict__")==null) {
            dict.__setitem__("__dict__",new PyGetSetDescr(newtype,"__dict__",PyObject.class,"getDict",null));
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
     * INTERNAL
     * lookup for name through mro objects' dicts
     *
     * @param name  attribute name (must be interned)
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
        super(true);
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

    private final static String[] EMPTY = new String[0];

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

    private static Method get_descr_method(
        Class c,
        String name,
        Class[] parmtypes) {
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

    private final static Class[] O = { PyObject.class };
    private final static Class[] OO = { PyObject.class, PyObject.class };

    private static void fillFromClass(
        PyType newtype,
        String name,
        Class c,
        Class base,
        boolean newstyle,
        Method setup,
        String[] exposed_methods) {

        if (base == null) {
            base = c.getSuperclass();
        }

        if (name == null) {
            name = c.getName();
        }

        if (name.startsWith("org.python.core.Py")) {
            name = name.substring("org.python.core.Py".length()).toLowerCase();
        } else {
            int lastdot = name.lastIndexOf('.');
            if (lastdot != -1) {
                name = name.substring(lastdot+1);
            }
        }

        newtype.name = name;

        newtype.underlying_class = c;

        boolean top = false;

        // basic mro, base, bases
        PyType[] mro = null;
        if (base == Object.class) {
            mro = new PyType[] { newtype };
            top = true;
        } else {
            PyType basetype = fromClass(base);
            mro = new PyType[basetype.mro.length + 1];
            System.arraycopy(basetype.mro, 0, mro, 1, basetype.mro.length);
            mro[0] = newtype;

            newtype.base = basetype;
            newtype.bases = new PyObject[] { basetype };
        }
        newtype.mro = mro;

        HashMap propnames = null;
        if (!newstyle)
            propnames = new HashMap();

        boolean only_exposed_methods = newstyle;

        PyObject dict = new PyStringMap();

        if (only_exposed_methods) {
            for (int i = 0; i < exposed_methods.length; i++) {
                String methname = exposed_methods[i];
                dict.__setitem__(
                    normalize_name(methname),
                    new PyReflectedFunction(methname));
            }
        }

        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method meth = methods[i];

            Class declaring = meth.getDeclaringClass();
            if (declaring != base
                && base.isAssignableFrom(declaring)
                && !ignore(meth)) {
                String methname = meth.getName();
                String nmethname = normalize_name(methname);
                PyReflectedFunction reflfunc =
                    (PyReflectedFunction) dict.__finditem__(nmethname);
                boolean added = false;
                if (reflfunc == null) {
                    if (!only_exposed_methods) {
                        dict.__setitem__(
                            nmethname,
                            new PyReflectedFunction(meth));
                        added = true;
                    }
                } else {
                    reflfunc.addMethod(meth);
                    added = true;
                }
                if (propnames != null
                    && added
                    && !Modifier.isStatic(meth.getModifiers())) {
                    // check for xxxX.*
                    int n = meth.getParameterTypes().length;
                    if (methname.startsWith("get") && n == 0) {
                        propnames.put(methname.substring(3), "getter");
                    } else if (
                        methname.startsWith("is")
                            && n == 0
                            && meth.getReturnType() == Boolean.TYPE) {
                        propnames.put(methname.substring(2), "getter");
                    } else if (methname.startsWith("set") && n == 1) {
                        propnames.put(methname.substring(3), meth);
                    }
                }

            }

        }

        boolean has_set = false, has_delete = false;
        if (!top) {
            if (get_descr_method(c, "__set__", OO) != null || /*backw comp*/
                get_descr_method(c, "_doset", OO) != null) {
                has_set = true;
            }

            if (get_descr_method(c, "__delete__", O) != null || /*backw comp*/
                get_descr_method(c, "_dodel", O) != null) {
                has_delete = true;
            }
        }

        for (int i = 0; i < methods.length; i++) {
            Method meth = methods[i];

            String nmethname = normalize_name(meth.getName());
            PyReflectedFunction reflfunc =
                (PyReflectedFunction) dict.__finditem__(nmethname);
            if (reflfunc != null) {
                reflfunc.addMethod(meth);
            }

        }

        if (!newstyle) { // backward compatibility
            Field[] fields = c.getFields();

            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                Class declaring = field.getDeclaringClass();
                if (declaring != base && base.isAssignableFrom(declaring)) {
                    String fldname = field.getName();
                    int fldmods = field.getModifiers();
                    Class fldtype = field.getType();
                    if (Modifier.isStatic(fldmods)) {
                        // ignore static PyClass __class__
                        if (fldname.equals("__class__")
                            && fldtype == PyClass.class) {
                            continue;
                        } else if (
                            fldname.startsWith("__doc__")
                                && fldname.length() > 7
                                && fldtype == PyString.class) {
                            String fname = fldname.substring(7).intern();
                            PyObject memb = dict.__finditem__(fname);
                            if (memb != null
                                && memb instanceof PyReflectedFunction) {
                                PyString doc = null;
                                try {
                                    doc = (PyString) field.get(null);
                                } catch (IllegalAccessException e) {
                                    throw error(e);
                                }
                                ((PyReflectedFunction) memb).__doc__ = doc;
                            }

                        }
                    }
                    dict.__setitem__(
                        normalize_name(fldname),
                        new PyReflectedField(field));
                }

            }

            for (Iterator iter = propnames.keySet().iterator();
                iter.hasNext();
                ) {
                String propname = (String) iter.next();
                String npropname = normalize_name(decapitalize(propname));
                PyObject prev = dict.__finditem__(npropname);
                if (prev != null && prev instanceof PyReflectedFunction) {
                    continue;
                }
                Method getter = null;
                Method setter = null;
                Class proptype = null;
                getter =
                    get_non_static_method(c, "get" + propname, new Class[] {
                });
                if (getter == null)
                    getter =
                        get_non_static_method(c, "is" + propname, new Class[] {
                });
                if (getter != null) {
                    proptype = getter.getReturnType();
                    setter =
                        get_non_static_method(
                            c,
                            "set" + propname,
                            new Class[] { proptype });
                } else {
                    Object o = propnames.get(propname);
                    if (o instanceof Method) {
                        setter = (Method) o;
                        proptype = setter.getParameterTypes()[0];
                    }
                }
                if (setter != null || getter != null) {
                    dict.__setitem__(
                        npropname,
                        new PyBeanProperty(
                            npropname,
                            proptype,
                            getter,
                            setter));

                } else {
                    // xxx error
                }
            }

            Constructor[] ctrs = c.getConstructors();
            if (ctrs.length != 0) {
                final PyReflectedConstructor reflctr =
                    new PyReflectedConstructor("_new_impl");
                for (int i = 0; i < ctrs.length; i++) {
                    reflctr.addConstructor(ctrs[i]);
                }
                PyObject new_ = new PyNewWrapper(c, "__new__", -1, -1) {

                    public PyObject new_impl(
                        boolean init,
                        PyType subtype,
                        PyObject[] args,
                        String[] keywords) {
                        return reflctr.make(args, keywords);
                    }
                };

                dict.__setitem__("__new__", new_);
            }

            if (ClassDictInit.class.isAssignableFrom(c)
                && c != ClassDictInit.class) {
                try {
                    Method m =
                        c.getMethod(
                            "classDictInit",
                            new Class[] { PyObject.class });
                    m.invoke(null, new Object[] { dict });
                } catch (Exception exc) {
                    throw error(exc);
                }
            }

        } else {
            if (setup != null) {
                try {
                    setup.invoke(null, new Object[] { dict, null });
                } catch (Exception e) {
                    throw error(e);
                }
            }
            newtype.non_instantiable = dict.__finditem__("__new__") == null;

        }

        newtype.has_set = has_set;
        newtype.has_delete = has_delete;
        newtype.dict = dict;
    }

    private static HashMap class_to_type;

    public static interface Newstyle {
    }

    private static PyType addFromClass(Class c) {
        Method setup = null;
        boolean newstyle = Newstyle.class.isAssignableFrom(c);
        Class base = null;
        String name = null;
        String[] exposed_methods = null;
        try {
            setup =
                c.getDeclaredMethod(
                    "typeSetup",
                    new Class[] { PyObject.class, Newstyle.class });
            newstyle = true;
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            throw error(e);
        }
        if (newstyle) { // newstyle
            base = (Class) exposed_decl_get_object(c, "base");
            name = (String) exposed_decl_get_object(c, "name");
            if (base == null) {
                Class cur = c;
                while (cur != PyObject.class) {
                    Class exposed_as =
                        (Class) exposed_decl_get_object(cur, "as");
                    if (exposed_as != null) {
                        PyType exposed_as_type = fromClass(exposed_as);
                        class_to_type.put(c, exposed_as_type);
                        return exposed_as_type;
                    }
                    cur = cur.getSuperclass();
                }
            }
            exposed_methods = (String[]) exposed_decl_get_object(c, "methods");
            if (exposed_methods == null)
                exposed_methods = EMPTY;
        }
        PyType newtype = c == PyType.class ? new PyType(true) : new PyType();
        class_to_type.put(c, newtype);
        fillFromClass(newtype, name, c, base, newstyle, setup, exposed_methods);
        return newtype;
    }

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
     *   Class exposed_as => instances are exposed as implementing
     *    just this superclass
     *
     *   (String[] exposed_methods)
     *
     */

    public static synchronized PyType fromClass(Class c) {
        if (class_to_type == null) {
            class_to_type = new HashMap();
            addFromClass(PyType.class);
        }
        PyType type = (PyType) class_to_type.get(c);
        if (type != null)
            return type;
        return addFromClass(c);
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


    public PyObject getDict() { // xxx return dict-proxy
        return dict;
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
        newobj.dispatch__init__(type,args,keywords);
        return newobj;
    }


    /**
     * @see org.python.core.PyObject#__call__(org.python.core.PyObject[], java.lang.String[])
     */
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return type___call__(args,keywords);
    }

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
            return "_"+classname.substring(i)+methodname;
        }
        return methodname;
    }

}
