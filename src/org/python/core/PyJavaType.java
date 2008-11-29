package org.python.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
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
        Class<?> base = underlying_class.getSuperclass();

        // Add methods and determine bean properties declared on this class
        Map<String, PyBeanProperty> props = Generic.map();
        for (Method meth : underlying_class.getMethods()) {
            if (!declaredOnMember(base, meth) || ignore(meth)) {
                continue;
            }
            String methname = meth.getName();
            String nmethname = normalize(methname);
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if (reflfunc == null) {
                dict.__setitem__(nmethname, new PyReflectedFunction(meth));
            } else {
                reflfunc.addMethod(meth);
            }
            if (!Modifier.isStatic(meth.getModifiers())) {
                // check for xxxX.*
                int n = meth.getParameterTypes().length;
                String name = null;
                boolean get = true;
                if (methname.startsWith("get") && methname.length() > 3 && n == 0) {
                    name = methname.substring(3);
                } else if (methname.startsWith("is") && methname.length() > 2 && n == 0
                        && meth.getReturnType() == Boolean.TYPE) {
                    name = methname.substring(2);
                } else if (methname.startsWith("set") && methname.length() > 3 && n == 1) {
                    name = methname.substring(3);
                    get = false;
                }
                if (name != null) {
                    name = normalize(StringUtil.decapitalize(name));
                    PyBeanProperty prop = props.get(name);
                    if (prop == null) {
                        prop = new PyBeanProperty(name, null, null, null);
                        props.put(name, prop);
                    }
                    if (get) {
                        prop.getMethod = meth;
                        prop.myType = meth.getReturnType();
                    } else {
                        prop.setMethod = meth;
                    }
                }
            }
        }

        // Add arguments for superclass methods with the same names as methods declared on this type
        for (Method meth : underlying_class.getMethods()) {
            String nmethname = normalize(meth.getName());
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if (reflfunc != null) {
                reflfunc.addMethod(meth);
            }
        }

        // Add fields declared on this type
        for (Field field : underlying_class.getFields()) {
            if (!declaredOnMember(base, field)) {
                continue;
            }
            String fldname = field.getName();
            if (Modifier.isStatic(field.getModifiers())) {
                if (fldname.startsWith("__doc__") && fldname.length() > 7
                        && field.getType() == PyString.class) {
                    String fname = fldname.substring(7).intern();
                    PyObject memb = dict.__finditem__(fname);
                    if (memb != null && memb instanceof PyReflectedFunction) {
                        PyString doc = null;
                        try {
                            doc = (PyString)field.get(null);
                        } catch (IllegalAccessException e) {
                            throw Py.JavaError(e);
                        }
                        ((PyReflectedFunction)memb).__doc__ = doc;
                    }
                }
            }
            if (dict.__finditem__(normalize(fldname)) == null) {
                dict.__setitem__(normalize(fldname), new PyReflectedField(field));
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
            // If the return types on the set and get methods for a property don't agree, the get
            // get method takes precedence
            if (prop.getMethod != null && prop.setMethod != null
                    && prop.myType != prop.setMethod.getParameterTypes()[0]) {
                prop.setMethod = null;
            }
            dict.__setitem__(prop.__name__, prop);
        }

        final PyReflectedConstructor reflctr = new PyReflectedConstructor("_new_impl");
        for (Constructor<?> ctr : underlying_class.getConstructors()) {
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
                throw Py.JavaError(exc);
            }
        }
        if (base != Object.class) {
            has_set = getDescrMethod(underlying_class, "__set__", OO) != null
                    || getDescrMethod(underlying_class, "_doset", OO) != null;
            has_delete = getDescrMethod(underlying_class, "__delete__", PyObject.class) != null
                    || getDescrMethod(underlying_class, "_dodel", PyObject.class) != null;
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

    private static final PyBuiltinMethodNarrow LEN_PROXY =
        new PyBuiltinMethodNarrow("__len__", 0, 0) {
        @Override
        public PyObject __call__() {
            return Py.newInteger(((Collection<?>)self.getJavaProxy()).size());
        }
    };

    private static final PyBuiltinMethodNarrow MAP_GET_PROXY =
        new PyBuiltinMethodNarrow("__getitem__", 1, 1) {
        @Override
        public PyObject __call__(PyObject key) {
            return Py.java2py(((Map<?, ?>)self.getJavaProxy()).get(Py.tojava(key, Object.class)));
        }
    };

    private static final PyBuiltinMethodNarrow MAP_PUT_PROXY =
        new PyBuiltinMethodNarrow("__setitem__", 2, 2) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            return Py.java2py(((Map<Object, Object>)self.getJavaProxy()).put(Py.tojava(key, Object.class),
                                                                        Py.tojava(value,
                                                                                  Object.class)));
        }
    };

    private static final PyBuiltinMethodNarrow MAP_REMOVE_PROXY =
        new PyBuiltinMethodNarrow("__delitem__", 1, 1) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            return Py.java2py(((Map<?, ?>)self.getJavaProxy()).remove(Py.tojava(key, Object.class)));
        }
    };

    private static final PyBuiltinMethodNarrow LIST_GET_PROXY =
        new PyBuiltinMethodNarrow("__getitem__", 1, 1){
        @Override
        public PyObject __call__(PyObject key) {
            if (key instanceof PyInteger) {
                return Py.java2py(((List<?>)self.getJavaProxy()).get(((PyInteger)key).getValue()));
            } else {
                throw Py.TypeError("only integer keys accepted");
            }
        }
    };

    private static final PyBuiltinMethodNarrow LIST_SET_PROXY =
        new PyBuiltinMethodNarrow("__setitem__", 2, 2) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            if (key instanceof PyInteger) {
                ((List<Object>)self.getJavaProxy()).set(((PyInteger)key).getValue(),
                                                   Py.tojava(value, Object.class));
            } else {
                throw Py.TypeError("only integer keys accepted");
            }
            return Py.None;
        }
    };

    private static final PyBuiltinMethodNarrow LIST_REMOVE_PROXY =
        new PyBuiltinMethodNarrow("__delitem__", 1, 1) {
         @Override
        public PyObject __call__(PyObject key, PyObject value) {
            if (key instanceof PyInteger) {
                return Py.java2py(((List<Object>)self.getJavaProxy()).remove(((PyInteger)key).getValue()));
            } else {
                throw Py.TypeError("only integer keys accepted");
            }
        }
    };
    class EnumerationIter extends PyIterator {

        private Enumeration<Object> proxy;

        public EnumerationIter(Enumeration<Object> proxy) {
            this.proxy = proxy;
        }

        public PyObject __iternext__() {
            return proxy.hasMoreElements() ? Py.java2py(proxy.nextElement()) : null;
        }
    }

    private static class IteratorIter extends PyIterator {

        private Iterator<Object> proxy;

        public IteratorIter(Iterator<Object> proxy) {
            this.proxy = proxy;
        }

        public PyObject __iternext__() {
            return proxy.hasNext() ? Py.java2py(proxy.next()) : null;
        }
    }


    private static final PyBuiltinMethodNarrow ITERABLE_PROXY =
        new PyBuiltinMethodNarrow("__iter__", 0, 0) {
        public PyObject __call__() {
            return new IteratorIter(((Iterable)self.getJavaProxy()).iterator());
        }
    };

    private static final PyBuiltinMethodNarrow EQUALS_PROXY =
        new PyBuiltinMethodNarrow("__eq__", 1, 1) {
        @Override
        public PyObject __call__(PyObject o) {
            return self.getJavaProxy().equals(o.__tojava__(self.getJavaProxy().getClass())) ?
                    Py.True : Py.False;
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
