package org.python.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.core.util.StringUtil;
import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedType;
import org.python.util.Generic;

public class PyJavaType extends PyType implements ExposeAsSuperclass {

    private final static Class<?>[] OO = {PyObject.class, PyObject.class};

    private static Map<Class<?>, PyBuiltinMethod[]> collectionProxies;

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
    protected void init() {
        name = underlying_class.getName();
        // Strip the java fully qualified class name from Py classes in core
        if (name.startsWith("org.python.core.Py")) {
            name = name.substring("org.python.core.Py".length()).toLowerCase();
        }
        dict = new PyStringMap();
        Class<?> baseClass = underlying_class.getSuperclass();
        if (PyObject.class.isAssignableFrom(underlying_class)) {
            // Non-exposed subclasses of PyObject use a simple linear mro to PyObject that ignores
            // their interfaces
            computeLinearMro(baseClass);
        } else {
            // Wrapped Java types fill in their mro first using their base class and then all of
            // their interfaces.
            if (baseClass == null) {
                base = PyType.fromClass(PyObject.class);
            } else {
                base = PyType.fromClass(baseClass);
            }
            bases = new PyObject[1 + underlying_class.getInterfaces().length];
            bases[0] = base;
            for (int i = 1; i < bases.length; i++) {
                bases[i] = PyType.fromClass(underlying_class.getInterfaces()[i - 1]);
            }
            Set<PyObject> seen = Generic.set();
            List<PyObject> mros = Generic.list();
            mros.add(this);
            for (PyObject obj : bases) {
                for (PyObject mroObj : ((PyType)obj).mro) {
                    if (seen.add(mroObj)) {
                        mros.add(mroObj);
                    }
                }
            }
            mro = mros.toArray(new PyObject[mros.size()]);
        }

        // PyReflected* can't call or access anything from non-public classes that aren't in
        // org.python.core
        if (!Modifier.isPublic(underlying_class.getModifiers()) &&
                !name.startsWith("org.python.core")) {
            return;
        }

        // Add methods and determine bean properties declared on this class
        Map<String, PyBeanProperty> props = Generic.map();
        Map<String, PyBeanEvent> events = Generic.map();
        for (Method meth : underlying_class.getMethods()) {
            if (!declaredOnMember(baseClass, meth) || ignore(meth)) {
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

            // Now check if this is a bean method, for which it must be an instance method
            if (Modifier.isStatic(meth.getModifiers())) {
                continue;
            }

            // First check if this is a bean event addition method
            int n = meth.getParameterTypes().length;
            if ((methname.startsWith("add") || methname.startsWith("set"))
                    && methname.endsWith("Listener") && n == 1 &&
                    meth.getReturnType() == Void.TYPE &&
                    EventListener.class.isAssignableFrom(meth.getParameterTypes()[0])) {
                Class<?> eventClass = meth.getParameterTypes()[0];
                String ename = eventClass.getName();
                int idot = ename.lastIndexOf('.');
                if (idot != -1) {
                    ename = ename.substring(idot + 1);
                }
                ename = normalize(StringUtil.decapitalize(ename));
                events.put(ename, new PyBeanEvent(ename, eventClass, meth));
                continue;
            }

            // Now check if it's a bean property accessor
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
            if (!declaredOnMember(baseClass, field)) {
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

        for (PyBeanEvent ev : events.values()) {
            if (dict.__finditem__(ev.__name__) == null) {
                dict.__setitem__(ev.__name__, ev);
            }

            for (Method meth : ev.eventClass.getMethods()) {
                String name = meth.getName().intern();
                if (dict.__finditem__(name) != null) {
                    continue;
                }
                dict.__setitem__(name, new PyBeanEventProperty(name,
                                                               ev.eventClass,
                                                               ev.addMethod,
                                                               meth));
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
            // Only add the class if there isn't something else with that name and it came from this
            // class
            if (inner.getDeclaringClass() == underlying_class &&
                    dict.__finditem__(inner.getSimpleName()) == null) {
                // If this class is currently being loaded, any exposed types it contains won't have
                // set their builder in PyType yet, so add them to BOOTSTRAP_TYPES so they're
                // created as PyType instead of PyJavaType
                if (inner.getAnnotation(ExposedType.class) != null
                        || ExposeAsSuperclass.class.isAssignableFrom(inner)) {
                    Py.BOOTSTRAP_TYPES.add(inner);
                }
                dict.__setitem__(inner.getSimpleName(), PyType.fromClass(inner));
            }
        }
        for (Map.Entry<Class<?>, PyBuiltinMethod[]> entry : getCollectionProxies().entrySet()) {
            if (entry.getKey() == underlying_class) {
                for (PyBuiltinMethod meth : entry.getValue()) {
                    dict.__setitem__(meth.info.getName(), new PyMethodDescr(this, meth));
                }
            }
        }
        if (ClassDictInit.class.isAssignableFrom(underlying_class)
                && underlying_class != ClassDictInit.class) {
            try {
                Method m = underlying_class.getMethod("classDictInit", PyObject.class);
                m.invoke(null, dict);
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        }
        if (baseClass != Object.class) {
            has_set = getDescrMethod(underlying_class, "__set__", OO) != null
                    || getDescrMethod(underlying_class, "_doset", OO) != null;
            has_delete = getDescrMethod(underlying_class, "__delete__", PyObject.class) != null
                    || getDescrMethod(underlying_class, "_dodel", PyObject.class) != null;
        } else {
            // Pass __eq__ and __repr__ through to subclasses of Object
            PyBuiltinCallable equals = new PyBuiltinMethodNarrow("__eq__", 1, 1) {
                @Override
                public PyObject __call__(PyObject o) {
                    Object oAsJava = o.__tojava__(self.getJavaProxy().getClass());
                    return self.getJavaProxy().equals(oAsJava) ? Py.True : Py.False;
                }
            };
            dict.__setitem__("__eq__", new PyMethodDescr(this, equals));
            PyBuiltinCallable repr = new PyBuiltinMethodNarrow("__repr__", 0, 0) {
                @Override
                public PyObject __call__() {
                    return Py.newString(self.getJavaProxy().toString());
                }
            };
            dict.__setitem__("__repr__", new PyMethodDescr(this, repr));
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

    private class EnumerationIter extends PyIterator {

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

        public IteratorIter(Iterable<Object> proxy) {
            this(proxy.iterator());
        }

        public IteratorIter(Iterator<Object> proxy) {
            this.proxy = proxy;
        }

        public PyObject __iternext__() {
            return proxy.hasNext() ? Py.java2py(proxy.next()) : null;
        }
    }

    private static class ListMethod extends PyBuiltinMethodNarrow {
        protected ListMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        protected List<Object> asList(){
            return (List<Object>)self.getJavaProxy();
        }
    }

    private static class MapMethod extends PyBuiltinMethodNarrow {
        protected MapMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        protected Map<Object, Object> asMap(){
            return (Map<Object, Object>)self.getJavaProxy();
        }
    }

    private static Map<Class<?>, PyBuiltinMethod[]> getCollectionProxies() {
        if (collectionProxies == null) {
            collectionProxies = Generic.map();

            PyBuiltinMethodNarrow iterableProxy = new PyBuiltinMethodNarrow("__iter__", 0, 0) {
                public PyObject __call__() {
                    return new IteratorIter(((Iterable)self.getJavaProxy()));
                }
            };
            collectionProxies.put(Iterable.class, new PyBuiltinMethod[] {iterableProxy});

            PyBuiltinMethodNarrow lenProxy = new PyBuiltinMethodNarrow("__len__", 0, 0) {
                @Override
                public PyObject __call__() {
                    return Py.newInteger(((Collection<?>)self.getJavaProxy()).size());
                }
            };

            PyBuiltinMethodNarrow containsProxy = new PyBuiltinMethodNarrow("__contains__", 1, 1) {
                @Override
                public PyObject __call__(PyObject obj) {
                    Object other = obj.__tojava__(Object.class);
                    boolean contained = ((Collection<?>)self.getJavaProxy()).contains(other);
                    return contained ? Py.True : Py.False;
                }
            };
            collectionProxies.put(Collection.class, new PyBuiltinMethod[] {lenProxy,
                                                                           containsProxy});

            PyBuiltinMethodNarrow iteratorProxy = new PyBuiltinMethodNarrow("__iter__", 0, 0) {
                public PyObject __call__() {
                    return new IteratorIter(((Iterator)self.getJavaProxy()));
                }
            };
            collectionProxies.put(Iterator.class, new PyBuiltinMethod[] {iteratorProxy});

            // Map doesn't extend Collection, so it needs its own version of len, iter and contains
            PyBuiltinMethodNarrow mapLenProxy = new MapMethod("__len__", 0, 0) {
                @Override
                public PyObject __call__() {
                    return Py.java2py(asMap().size());
                }
            };
            PyBuiltinMethodNarrow mapIterProxy = new MapMethod("__iter__", 0, 0) {
                @Override
                public PyObject __call__() {
                    return new IteratorIter(asMap().keySet());
                }
            };
            PyBuiltinMethodNarrow mapContainsProxy = new MapMethod("__contains__", 1, 1) {
                public PyObject __call__(PyObject obj) {
                    Object other = obj.__tojava__(Object.class);
                    return asMap().containsKey(other) ? Py.True : Py.False;
                }
            };
            PyBuiltinMethodNarrow mapGetProxy = new MapMethod("__getitem__", 1, 1) {
                @Override
                public PyObject __call__(PyObject key) {
                    return Py.java2py(asMap().get(Py.tojava(key, Object.class)));
                }
            };
            PyBuiltinMethodNarrow mapPutProxy = new MapMethod("__setitem__", 2, 2) {
                @Override
                public PyObject __call__(PyObject key, PyObject value) {
                    return Py.java2py(asMap().put(Py.tojava(key, Object.class),
                                                  Py.tojava(value, Object.class)));
                }
            };
            PyBuiltinMethodNarrow mapRemoveProxy = new MapMethod("__delitem__", 1, 1) {
                @Override
                public PyObject __call__(PyObject key) {
                    return Py.java2py(asMap().remove(Py.tojava(key, Object.class)));
                }
            };
            collectionProxies.put(Map.class, new PyBuiltinMethod[] {mapLenProxy,
                                                                    mapIterProxy,
                                                                    mapContainsProxy,
                                                                    mapGetProxy,
                                                                    mapPutProxy,
                                                                    mapRemoveProxy});

            PyBuiltinMethodNarrow listGetProxy = new ListMethod("__getitem__", 1, 1) {
                @Override
                public PyObject __call__(PyObject key) {
                    return new ListIndexDelegate(asList()).checkIdxAndGetItem(key);
                }
            };
            PyBuiltinMethodNarrow listSetProxy = new ListMethod("__setitem__", 2, 2) {
                @Override
                public PyObject __call__(PyObject key, PyObject value) {
                    new ListIndexDelegate(asList()).checkIdxAndSetItem(key, value);
                    return Py.None;
                }
            };
            PyBuiltinMethodNarrow listRemoveProxy = new ListMethod("__delitem__", 1, 1) {
                 @Override
                public PyObject __call__(PyObject key) {
                     new ListIndexDelegate(asList()).checkIdxAndDelItem(key);
                     return Py.None;
                }
            };
            collectionProxies.put(List.class, new PyBuiltinMethod[] {listGetProxy,
                                                                     listSetProxy,
                                                                     listRemoveProxy});
        }
        return collectionProxies;
    }

    protected static class ListIndexDelegate extends SequenceIndexDelegate {

        private final List list;

        public ListIndexDelegate(List list) {
            this.list = list;
        }
        @Override
        public void delItem(int idx) {
            list.remove(idx);
        }

        @Override
        public PyObject getItem(int idx) {
            return Py.java2py(list.get(idx));
        }

        @Override
        public PyObject getSlice(int start, int stop, int step) {
            if (step > 0 && stop < start) {
                stop = start;
            }
            int n = PySequence.sliceLength(start, stop, step);
            List<Object> newList;
             try {
                newList = list.getClass().newInstance();
            } catch (Exception e) {
                throw Py.JavaError(e);
            }
            int j = 0;
            for (int i = start; j < n; i += step) {
                newList.add(list.get(i));
            }
            return Py.java2py(newList);
        }

        @Override
        public String getTypeName() {
            return list.getClass().getName();
        }

        @Override
        public int len() {
            return list.size();
        }

        @Override
        public void setItem(int idx, PyObject value) {
            list.set(idx, value.__tojava__(Object.class));
        }

        @Override
        public void setSlice(int start, int stop, int step, PyObject value) {
            if (stop < start) {
                stop = start;
            }
            if (step == 0) {
                return;
            }
            if (value.javaProxy == this) {
                List newseq = new ArrayList(len());
                for (Object object : ((List)value.javaProxy)) {
                    newseq.add(object);
                }
                value = Py.java2py(newseq);
            }
            int j = start;
            for (PyObject obj : value.asIterable()) {
                setItem(j, obj);
                j += step;
            }
        }

        @Override
        public void delItems(int start, int stop) {
            int n = stop - start;
            while (n-- > 0) {
                delItem(start);
            }
        }
    }
}
