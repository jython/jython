package org.python.core;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;

/**
 * first-class Python type.
 *
 */
public class PyType extends PyObject {

    public static void typeSetup(PyObject dict, Newstyle marker) {
        dict.__setitem__(
            "__dict__",
            new PyGetSetDescr("__dict__", PyType.class, "getDict", null));
        dict.__setitem__(
            "__name__",
            new PyGetSetDescr("__name__", PyType.class, "fastGetName", null));
        dict.__setitem__(
            "__new__",
            new PyNewWrapper(PyType.class,"__new__",-1,-1) {
                public PyObject new_impl(boolean init,PyType subtype,PyObject[] args,String[] keywords) {
                    if (args.length==1 && keywords.length == 0) {
                        return args[0].getType();
                    }
                    // xxx 3args : type instantiation logic
                    throw Py.TypeError("type() takes exactly 1 (or...) arguments");
                }
            });

    }

    private String name;
    private PyObject dict;
    private PyObject[] mro;
    private Class underlying_class;
    private boolean non_instantiable = false;

    boolean has_set, has_delete;

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
            PyObject dict = mro[i].getDict();
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
                name = name.substring(lastdot);
            }
        }

        newtype.name = name;

        newtype.underlying_class = c;

        boolean top = false;

        // basic mro
        PyType[] mro = null;
        if (base == Object.class) {
            mro = new PyType[] { newtype };
            top = true;
        } else {
            PyType basetype = fromClass(base);
            mro = new PyType[basetype.mro.length + 1];
            System.arraycopy(basetype.mro, 0, mro, 1, basetype.mro.length);
            mro[0] = newtype;
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
    final PyObject type__findattr__(String name) {
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

    /**
     * @see org.python.core.PyObject#getDict()
     */
    public PyObject getDict() {
        return dict;
    }

    public Object __tojava__(Class c) {
        if (c == Object.class || c == Class.class || c == Serializable.class) {
            return underlying_class;
        }
        return super.__tojava__(c);
    }

    public String toString() {
        return "<type '" + name + "'>"; // xxx use fullname
    }

    /**
     * @see org.python.core.PyObject#__findattr__(java.lang.String)
     */
    public PyObject __findattr__(String name) {
        return type__findattr__(name);
    }

    /**
     * @see org.python.core.PyObject#safeRepr()
     */
    public String safeRepr() throws PyIgnoreMethodTag {
        return "type object '" + name + "'"; // xxx use fullname
    }

    /**
     * @see org.python.core.PyObject#__call__(org.python.core.PyObject[], java.lang.String[])
     */
    public PyObject __call__(PyObject[] args, String[] keywords) {
        PyObject new_ = lookup("__new__");
        if (non_instantiable || new_ == null) {
            throw Py.TypeError("cannot create '" + name + "' instances");
            // xxx fullname
        }
        if (new_ instanceof PyNewWrapper) {
            return ((PyNewWrapper) new_).new_impl(true, this, args, keywords);
        }
        int n = args.length;
        PyObject[] type_prepended = new PyObject[n + 1];
        System.arraycopy(args, 0, type_prepended, 1, n);
        type_prepended[0] = this;
        return new_.__get__(null, this).__call__(type_prepended, keywords);
        // xxx __init__ invocation
    }

    // xxx other __call__ shortcuts

}
