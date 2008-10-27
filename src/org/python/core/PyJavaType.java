package org.python.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.python.core.util.StringUtil;
import org.python.expose.ExposeAsSuperclass;

public class PyJavaType extends PyType implements ExposeAsSuperclass {

    private final static Class<?>[] OO = {PyObject.class, PyObject.class};

    public PyJavaType() {
        super(TYPE == null ? fromClass(PyType.class) : TYPE);
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
            if (declaring != base && base.isAssignableFrom(declaring)) {
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
                dict.__setitem__(normalize_name(fldname), new PyReflectedField(field));
            }
        }
        for (String propname : propnames.keySet()) {
            String npropname = normalize_name(StringUtil.decapitalize(propname));
            PyObject prev = dict.__finditem__(npropname);
            if (prev != null && prev instanceof PyReflectedFunction) {
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
                dict.__setitem__(npropname, new PyBeanProperty(npropname, proptype, getter, setter));
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
}
