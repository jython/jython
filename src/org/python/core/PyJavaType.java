package org.python.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.core.util.StringUtil;
import org.python.expose.ExposeAsSuperclass;
import org.python.util.Generic;

public class PyJavaType extends PyType implements ExposeAsSuperclass {

    private final static Class<?>[] OO = {PyObject.class, PyObject.class};

    public static PyObject wrapJavaObject(Object o) {
        PyObject obj = new PyObjectDerived(PyType.fromClass(o.getClass()));
        obj.javaProxy = o;
        return obj;
    }

    public PyJavaType() {
        super(TYPE == null ? fromClass(PyType.class) : TYPE);
    }

    @Override
    public Class<?> getProxyType() {
        return PyObject.class.isAssignableFrom(underlying_class) ? null : underlying_class;
    }

    @Override
    protected void fillDict() {
        dict = new PyStringMap();
        Map<String, Object> propnames = new HashMap<String, Object>();
        Class<?> base = underlying_class.getSuperclass();
        Method[] methods = underlying_class.getMethods();
        for (Method meth : methods) {
            Class<?> declaring = meth.getDeclaringClass();
            if (base == null ||
                    (declaring != base && base.isAssignableFrom(declaring) && !ignore(meth))) {
                String methname = meth.getName();
                String nmethname = normalize_name(methname);
                PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
                if (reflfunc == null) {
                    dict.__setitem__(nmethname, new PyReflectedFunction(meth));
                } else {
                    reflfunc.addMethod(meth);
                }
                if (!Modifier.isStatic(meth.getModifiers())) {
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
        for (Method meth : methods) {
            String nmethname = normalize_name(meth.getName());
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if (reflfunc != null) {
                reflfunc.addMethod(meth);
            }
        }
        Field[] fields = underlying_class.getFields();
        for (Field field : fields) {
            Class<?> declaring = field.getDeclaringClass();
            if (base == null || (declaring != base && base.isAssignableFrom(declaring))) {
                String fldname = field.getName();
                int fldmods = field.getModifiers();
                Class<?> fldtype = field.getType();
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
                if (dict.__finditem__(normalize_name(fldname)) == null) {
                    dict.__setitem__(normalize_name(fldname), new PyReflectedField(field));
                }
            }
        }
        for (String propname : propnames.keySet()) {
            if (propname.equals("")) {
                continue;
            }
            String npropname = normalize_name(StringUtil.decapitalize(propname));
            PyObject prev = dict.__finditem__(npropname);
            if (prev != null && (!(prev instanceof PyReflectedField) ||
                    !Modifier.isStatic(((PyReflectedField)prev).field.getModifiers()))) {
                continue;
            }
            Method getter = null;
            Method setter = null;
            Class<?> proptype = null;
            getter = get_non_static_method(underlying_class, "get" + propname);
            if (getter == null)
                getter = get_non_static_method(underlying_class, "is" + propname);
            if (getter != null) {
                proptype = getter.getReturnType();
                setter = get_non_static_method(underlying_class, "set" + propname, proptype);
            } else {
                Object o = propnames.get(propname);
                if (o instanceof Method) {
                    setter = (Method)o;
                    proptype = setter.getParameterTypes()[0];
                }
            }
            if (setter != null || getter != null) {
                PyBeanProperty prop = new PyBeanProperty(npropname, proptype, getter, setter);
                if (prev != null) {
                    prop.field = ((PyReflectedField)prev).field;
                }
                dict.__setitem__(npropname, prop);
            } else {
                // XXX error
            }
        }
        Constructor<?>[] ctrs = underlying_class.getConstructors();
        if (ctrs.length != 0) {
            final PyReflectedConstructor reflctr = new PyReflectedConstructor("_new_impl");
            for (Constructor<?> ctr : ctrs) {
                reflctr.addConstructor(ctr);
            }
            if (PyObject.class.isAssignableFrom(underlying_class)) {
                PyObject new_ = new PyNewWrapper(underlying_class, "__new__", -1, -1) {

                    public PyObject new_impl(boolean init,
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
        }
        for (Class<?> inner : underlying_class.getClasses()) {
            dict.__setitem__(inner.getSimpleName(), PyType.fromClass(inner));
        }
        for (Map.Entry<Class<?>, PyBuiltinMethod[]> entry : _collectionProxies.entrySet()) {
            if (entry.getKey().isAssignableFrom(underlying_class)) {
                for (PyBuiltinMethod meth : entry.getValue()) {
                    dict.__setitem__(meth.info.getName(), new PyMethodDescr(this, meth));
                }
            }
        }
        dict.__setitem__("__eq__", new PyMethodDescr(this, EQUALS_PROXY));
        if (ClassDictInit.class.isAssignableFrom(underlying_class)
                && underlying_class != ClassDictInit.class) {
            try {
                Method m = underlying_class.getMethod("classDictInit", PyObject.class);
                m.invoke(null, dict);
            } catch (Exception exc) {
                throw error(exc);
            }
        }
        if (base != Object.class) {
            has_set = get_descr_method(underlying_class, "__set__", OO) != null
                    || get_descr_method(underlying_class, "_doset", OO) != null;
            has_delete = get_descr_method(underlying_class, "__delete__", PyObject.class) != null
                    || get_descr_method(underlying_class, "_dodel", PyObject.class) != null;
        }
    }

    private static String normalize_name(String name) {
        if (name.endsWith("$")) {
            name = name.substring(0, name.length() - 1);
        }
        return name.intern();
    }

    private static Method get_non_static_method(Class<?> c, String name, Class<?>... parmtypes) {
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

    private static Method get_descr_method(Class<?> c, String name, Class<?>... parmtypes) {
        Method meth = get_non_static_method(c, name, parmtypes);
        if (meth != null && meth.getDeclaringClass() != PyObject.class) {
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

    private static PyException error(Exception e) {
        return Py.JavaError(e);
    }

    protected static  final PyBuiltinMethodNarrow LEN_PROXY =
        new PyBuiltinMethodNarrow("__len__", 0, 0) {
        @Override
        public PyObject __call__() {
            return Py.newInteger(((Collection<?>)self.javaProxy).size());
        }
    };

    protected static final PyBuiltinMethodNarrow MAP_GET_PROXY =
        new PyBuiltinMethodNarrow("__getitem__", 1, 1) {
        @Override
        public PyObject __call__(PyObject key) {
            return Py.java2py(((Map<?, ?>)self.javaProxy).get(Py.tojava(key, Object.class)));
        }
    };

    protected static final PyBuiltinMethodNarrow MAP_PUT_PROXY =
        new PyBuiltinMethodNarrow("__setitem__", 2, 2) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            return Py.java2py(((Map<Object, Object>)self.javaProxy).put(Py.tojava(key, Object.class),
                                                                        Py.tojava(value,
                                                                                  Object.class)));
        }
    };

    protected static final PyBuiltinMethodNarrow MAP_REMOVE_PROXY =
        new PyBuiltinMethodNarrow("__delitem__", 1, 1) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            return Py.java2py(((Map<?, ?>)self.javaProxy).remove(Py.tojava(key, Object.class)));
        }
    };

    protected static final PyBuiltinMethodNarrow LIST_GET_PROXY =
        new PyBuiltinMethodNarrow("__getitem__", 1, 1){
        @Override
        public PyObject __call__(PyObject key) {
            if (key instanceof PyInteger) {
                return Py.java2py(((List<?>)self.javaProxy).get(((PyInteger)key).getValue()));
            } else {
                throw Py.TypeError("only integer keys accepted");
            }
        }
    };

    protected static final PyBuiltinMethodNarrow LIST_SET_PROXY =
        new PyBuiltinMethodNarrow("__setitem__", 2, 2) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            if (key instanceof PyInteger) {
                ((List<Object>)self.javaProxy).set(((PyInteger)key).getValue(),
                                                   Py.tojava(value, Object.class));
            } else {
                throw Py.TypeError("only integer keys accepted");
            }
            return Py.None;
        }
    };

    protected static final PyBuiltinMethodNarrow LIST_REMOVE_PROXY =
        new PyBuiltinMethodNarrow("__delitem__", 1, 1) {
         @Override
        public PyObject __call__(PyObject key, PyObject value) {
            if (key instanceof PyInteger) {
                return Py.java2py(((List<Object>)self.javaProxy).remove(((PyInteger)key).getValue()));
            } else {
                throw Py.TypeError("only integer keys accepted");
            }
        }
    };

    public static final PyBuiltinMethodNarrow ITERABLE_PROXY =
        new PyBuiltinMethodNarrow("__iter__", 0, 0) {
        public PyObject __call__() {
            return new IteratorIter(((Iterable)self.javaProxy).iterator());
        }
    };

    public static final PyBuiltinMethodNarrow EQUALS_PROXY =
        new PyBuiltinMethodNarrow("__eq__", 1, 1) {
        @Override
        public PyObject __call__(PyObject o) {
            return self.javaProxy.equals(o.__tojava__(self.javaProxy.getClass())) ? Py.True
                    : Py.False;
        }
    };

    static Map<Class<?>, PyBuiltinMethod[]> _collectionProxies = Generic.map();
    static {
        _collectionProxies.put(Iterable.class, new PyBuiltinMethod[] {ITERABLE_PROXY});
        _collectionProxies.put(Collection.class, new PyBuiltinMethod[] {LEN_PROXY});
        _collectionProxies.put(Map.class, new PyBuiltinMethod[] {MAP_GET_PROXY,
                                                                 MAP_PUT_PROXY,
                                                                 MAP_REMOVE_PROXY});
        _collectionProxies.put(List.class, new PyBuiltinMethod[] {LIST_GET_PROXY,
                                                                  LIST_SET_PROXY,
                                                                  LIST_REMOVE_PROXY});
    }
}
