package org.python.core;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Proxy Java objects implementing java.util.List with Python methods corresponding to the standard
 * list type
 */
class JavaProxyMap {

    @Untraversable
    private static class MapMethod extends PyBuiltinMethodNarrow {

        protected MapMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        protected MapMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        protected Map<Object, Object> asMap() {
            return (Map<Object, Object>) self.getJavaProxy();
        }
    }

    @Untraversable
    private static class MapClassMethod extends PyBuiltinClassMethodNarrow {

        protected MapClassMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        protected Class<?> asClass() {
            return (Class<?>) self.getJavaProxy();
        }
    }

    /**
     * Compares this object with other to check for equality. Used to implement __eq __ and __ne__.
     * May return null if the other object cannot be compared i.e. is not a Python dict or Java Map.
     *
     * @param other The object to compare to this
     * @return true is equal, false if not equal and null if we can't compare
     */
    private static PyBoolean mapEq(PyObject self, PyObject other) {
        if (isPyDict(other)) {
            // Being compared to Python dict
            PyDictionary oDict = (PyDictionary) other;
            Map<Object, Object> selfMap = (Map<Object, Object>) self.getJavaProxy();
            if (selfMap.size() != oDict.size()) {
                // Map/dict are different sizes therefore not equal
                return Py.False;
            }
            // Loop through all entries checking the keys and values are matched
            for (Entry<Object, Object> entry : selfMap.entrySet()) {
                Object k = entry.getKey();
                Object v = entry.getValue();
                PyObject oVal = oDict.__finditem__(Py.java2py(k));
                if (oVal == null) {
                    // No value for this key in oDict, therefore not equal
                    return Py.False;
                }
                if (!Py.java2py(v)._eq(oVal).__nonzero__()) {
                    // The values for this key differ therefore not equal
                    return Py.False;
                }
            }
            // All keys and values are equal therefore map/dict are equal
            return Py.True;
        } else {
            // Being compared to something that is not a Python dict
            Object oj = other.getJavaProxy();
            if (oj instanceof Map) {
                // Being compared to a Java Map convert to Python
                Map<Object, Object> map = (Map) oj;
                final Map<PyObject, PyObject> pyMap = new HashMap<>();
                for (Entry<Object, Object> el : map.entrySet()) {
                    pyMap.put(Py.java2py(el.getKey()), Py.java2py(el.getValue()));
                }
                // Compare again this time after conversion to Python dict
                return mapEq(self, new PyDictionary(pyMap));
            } else {
                /*
                 * other is not a Python dict or Java Map, so we don't know if were equal therefore
                 * return null
                 */
                return null;
            }
        }
    }

    private static boolean isPyDict(PyObject object) {
        return object.getType().isSubType(PyDictionary.TYPE);
    }

    /**
     * Substitute for {@link Py#tojava(PyObject, Class)} when the second argument is
     * {@code Object.class}, and in which we allow a {@code null} argument to signify {@code None},
     * since {@code null} is then the return value.
     */
    private static Object tojava(PyObject pyo) {
        return (pyo == null || pyo == Py.None) ? null : Py.tojava(pyo, Object.class);
    }

    /** Return Python {@code ValueError} that None is not allowed. */
    private static RuntimeException nullException() {
        return Py.ValueError("None is not allowed: underlying container cannot store Java null.");
    }

    /**
     * Return Python {@code ValueError} that None is not allowed, or the
     * {@code NullPointerException}, if in fact the value was not {@code None}.
     *
     * @param npe original exception
     * @param key possibly causing the problem
     * @param value possibly causing the problem
     * @return the Python {@code ValueError}
     */
    private static RuntimeException nullException(NullPointerException npe, Object key,
            Object value) {
        return (key == Py.None || value == Py.None) ? nullException() : npe;
    }

    /*
     * Map ordering comparisons (lt, le, gt, ge) are based on the key sets; we just define mapLe +
     * mapEq for total ordering of such key sets
     */
    private static PyObject mapLe(PyObject self, PyObject other) {
        Set<Object> selfKeys = ((Map<Object, Object>) self.getJavaProxy()).keySet();
        if (other.getType().isSubType(PyDictionary.TYPE)) {
            PyDictionary oDict = (PyDictionary) other;
            for (Object k : selfKeys) {
                if (!oDict.__contains__(Py.java2py(k))) {
                    return Py.False;
                }
            }
            return Py.True;
        } else {
            Object oj = other.getJavaProxy();
            if (oj instanceof Map) {
                Map<Object, Object> map = (Map<Object, Object>) oj;
                return Py.newBoolean(map.keySet().containsAll(selfKeys));
            } else {
                return null;
            }
        }
    }

    // Map doesn't extend Collection, so it needs its own version of len, iter and contains
    private static final PyBuiltinMethodNarrow mapLenProxy = new MapMethod("__len__", 0) {

        @Override
        public PyObject __call__() {
            return Py.java2py(asMap().size());
        }
    };
    private static final PyBuiltinMethodNarrow mapReprProxy = new MapMethod("__repr__", 0) {

        @Override
        public PyObject __call__() {
            ThreadState ts = Py.getThreadState();
            if (!ts.enterRepr(self)) {
                return Py.newString("{...}");
            } else {
                StringBuilder repr = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<Object, Object> entry : asMap().entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        repr.append(", ");
                    }
                    PyObject key = Py.java2py(entry.getKey());
                    repr.append(key.__repr__().toString());
                    repr.append(": ");
                    PyObject value = Py.java2py(entry.getValue());
                    repr.append(value.__repr__().toString());
                }
                repr.append("}");
                ts.exitRepr(self);
                return Py.newString(repr.toString());
            }
        }
    };
    private static final PyBuiltinMethodNarrow mapEqProxy = new MapMethod("__eq__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return mapEq(self, other);
        }
    };
    private static final PyBuiltinMethodNarrow mapNeProxy = new MapMethod("__ne__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            // mapEq may return null if we don't know how to compare to other.
            PyBoolean equal = mapEq(self, other);
            if (equal != null) {
                // implement NOT equal by the inverse of equal
                return equal.__not__();
            }
            return null;
        }
    };
    private static final PyBuiltinMethodNarrow mapLeProxy = new MapMethod("__le__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return mapLe(self, other);
        }
    };
    private static final PyBuiltinMethodNarrow mapGeProxy = new MapMethod("__ge__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return (mapLe(self, other).__not__()).__or__(mapEq(self, other));
        }
    };
    private static final PyBuiltinMethodNarrow mapLtProxy = new MapMethod("__lt__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return mapLe(self, other).__and__(mapEq(self, other).__not__());
        }
    };
    private static final PyBuiltinMethodNarrow mapGtProxy = new MapMethod("__gt__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return mapLe(self, other).__not__();
        }
    };
    private static final PyBuiltinMethodNarrow mapIterProxy = new MapMethod("__iter__", 0) {

        @Override
        public PyObject __call__() {
            return new JavaIterator(asMap().keySet());
        }
    };
    private static final PyBuiltinMethodNarrow mapContainsProxy = new MapMethod("__contains__", 1) {

        @Override
        public PyObject __call__(PyObject obj) {
            return asMap().containsKey(tojava(obj)) ? Py.True : Py.False;
        }
    };
    /*
     * "get" needs to override java.util.Map#get() in its subclasses, too, so this needs to be
     * injected last (i.e. when HashMap is loaded not when it is recursively loading its super-type
     * Map).
     */
    private static final PyBuiltinMethodNarrow mapGetProxy = new MapMethod("get", 1, 2) {

        @Override
        public PyObject __call__(PyObject key) {
            return __call__(key, Py.None);
        }

        @Override
        public PyObject __call__(PyObject key, PyObject _default) {
            Map<Object, Object> map = asMap();
            Object k = tojava(key);
            if (map.containsKey(k)) {
                return Py.java2py(map.get(k));
            } else {
                return _default;
            }
        }
    };

    private static final PyBuiltinMethodNarrow mapGetItemProxy = new MapMethod("__getitem__", 1) {

        @Override
        public PyObject __call__(PyObject key) {
            Map<Object, Object> map = asMap();
            Object k = tojava(key);
            if (map.containsKey(k)) {
                return Py.java2py(map.get(k));
            }
            throw Py.KeyError(key);
        }
    };

    private static final PyBuiltinMethodNarrow mapPutProxy = new MapMethod("__setitem__", 2) {

        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            try {
                asMap().put(tojava(key), tojava(value));
                return Py.None;
            } catch (NullPointerException npe) {
                throw nullException(npe, key, value);
            }
        }
    };

    private static final PyBuiltinMethodNarrow mapRemoveProxy = new MapMethod("__delitem__", 1) {

        @Override
        public PyObject __call__(PyObject key) {
            Map<Object, Object> map = asMap();
            Object k = tojava(key);
            if (map.containsKey(k)) {
                map.remove(k);
                return Py.None;
            }
            throw Py.KeyError(key);
        }
    };

    private static final PyBuiltinMethodNarrow mapIterItemsProxy = new MapMethod("iteritems", 0) {

        @Override
        public PyObject __call__() {
            final Iterator<Map.Entry<Object, Object>> entryIterator = asMap().entrySet().iterator();
            return new PyIterator() {

                @Override
                public PyObject __iternext__() {
                    if (entryIterator.hasNext()) {
                        Map.Entry<Object, Object> e = entryIterator.next();
                        // yield a Python tuple object (key, value)
                        return new PyTuple(Py.java2py(e.getKey()), Py.java2py(e.getValue()));
                    }
                    return null;
                }
            };
        }
    };

    private static final PyBuiltinMethodNarrow mapIterKeysProxy = new MapMethod("iterkeys", 0) {

        @Override
        public PyObject __call__() {
            final Iterator<Object> keyIterator = asMap().keySet().iterator();
            return new PyIterator() {

                @Override
                public PyObject __iternext__() {
                    if (keyIterator.hasNext()) {
                        Object nextKey = keyIterator.next();
                        // yield a Python key
                        return Py.java2py(nextKey);
                    }
                    return null;
                }
            };
        }
    };

    private static final PyBuiltinMethodNarrow mapIterValuesProxy = new MapMethod("itervalues", 0) {

        @Override
        public PyObject __call__() {
            final Iterator<Object> valueIterator = asMap().values().iterator();
            return new PyIterator() {

                @Override
                public PyObject __iternext__() {
                    if (valueIterator.hasNext()) {
                        Object nextValue = valueIterator.next();
                        // yield a Python value
                        return Py.java2py(nextValue);
                    }
                    return null;
                }
            };
        }
    };

    private static final PyBuiltinMethodNarrow mapHasKeyProxy = new MapMethod("has_key", 1) {

        @Override
        public PyObject __call__(PyObject key) {
            return asMap().containsKey(tojava(key)) ? Py.True : Py.False;
        }
    };

    private static final PyBuiltinMethodNarrow mapKeysProxy = new MapMethod("keys", 0) {

        @Override
        public PyObject __call__() {
            PyList keys = new PyList();
            for (Object key : asMap().keySet()) {
                keys.add(Py.java2py(key));
            }
            return keys;
        }
    };

    private static final PyBuiltinMethod mapValuesProxy = new MapMethod("values", 0) {

        @Override
        public PyObject __call__() {
            PyList values = new PyList();
            for (Object value : asMap().values()) {
                values.add(Py.java2py(value));
            }
            return values;
        }
    };

    private static final PyBuiltinMethodNarrow mapSetDefaultProxy =
            new MapMethod("setdefault", 1, 2) {

                @Override
                public PyObject __call__(PyObject key) {
                    return __call__(key, Py.None);
                }

                @Override
                public PyObject __call__(PyObject pykey, PyObject _default) {
                    Map<Object, Object> map = asMap();
                    Object key = tojava(pykey);
                    try {
                        if (map.containsKey(key)) {
                            return Py.java2py(map.get(key));
                        } else {
                            map.put(key, tojava(_default));
                            return _default;
                        }
                    } catch (NullPointerException npe) {
                        throw nullException(npe, key, _default);
                    }
                }
            };

    private static final PyBuiltinMethodNarrow mapPopProxy = new MapMethod("pop", 1, 2) {

        @Override
        public PyObject __call__(PyObject key) {
            return __call__(key, null);
        }

        @Override
        public PyObject __call__(PyObject key, PyObject _default) {
            Map<Object, Object> map = asMap();
            Object k = tojava(key);
            if (map.containsKey(k)) {
                return Py.java2py(map.remove(k));
            } else if (_default == null) {
                throw Py.KeyError(key);
            } else {
                return _default;
            }
        }
    };

    private static final PyBuiltinMethodNarrow mapPopItemProxy = new MapMethod("popitem", 0) {

        @Override
        public PyObject __call__() {
            Map<Object, Object> map = asMap();
            Iterator<Entry<Object, Object>> entryIterator = map.entrySet().iterator();
            if (entryIterator.hasNext()) {
                Map.Entry<Object, Object> e = entryIterator.next();
                entryIterator.remove();
                return new PyTuple(Py.java2py(e.getKey()), Py.java2py(e.getValue()));
            }
            throw Py.KeyError("popitem(): map is empty");
        }
    };

    private static final PyBuiltinMethodNarrow mapItemsProxy = new MapMethod("items", 0) {

        @Override
        public PyObject __call__() {
            PyList items = new PyList();
            for (Map.Entry<Object, Object> entry : asMap().entrySet()) {
                items.add(new PyTuple(Py.java2py(entry.getKey()), Py.java2py(entry.getValue())));
            }
            return items;
        }
    };

    private static final PyBuiltinMethodNarrow mapCopyProxy = new MapMethod("copy", 0) {

        @Override
        public PyObject __call__() {
            Map<Object, Object> map = asMap();
            Map<Object, Object> newMap;
            Class<? extends Map<Object, Object>> clazz;
            try {
                clazz = (Class<Map<Object, Object>>) map.getClass();
                Constructor<? extends Map<Object, Object>> ctor = clazz.getDeclaredConstructor();
                newMap = ctor.newInstance();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    newMap.put(entry.getKey(), entry.getValue());
                }
            } catch (NullPointerException npe) {
                throw nullException();
            } catch (ReflectiveOperationException | SecurityException
                    | IllegalArgumentException e) {
                throw Py.JavaError(e);
            }
            return Py.java2py(newMap);
        }
    };

    private static final PyBuiltinMethodNarrow mapUpdateProxy = new MapMethod("update", 0, 1) {

        @Override
        public PyObject __call__() {
            return Py.None;
        }

        @Override
        public PyObject __call__(PyObject other) {
            /*
             * `other` is either another dict-like object, or an iterable of key/value pairs (as
             * tuples or other iterables of length two)
             */
            return __call__(new PyObject[] {other}, new String[] {});
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] keywords) {
            // Adapted from PyDictionary#update
            int nargs = args.length - keywords.length;
            if (nargs > 1) {
                throw PyBuiltinCallable.DefaultInfo.unexpectedCall(nargs, false, "update", 0, 1);
            }
            Map<Object, Object> map = asMap();
            try {
                if (nargs == 1) {
                    PyObject other = args[0];
                    Object proxy = other.getJavaProxy();
                    if (proxy instanceof Map) {
                        // other proxies a Java container: take contents verbatim.
                        map.putAll((Map<Object, Object>) proxy);
                    } else if (other instanceof PyDictionary) {
                        // keys and values must be converted from Python to Java equivalents.
                        mergeFromSeq(map, other.invoke("items"));
                    } else if (other instanceof PyStringMap) {
                        // keys and values must be converted from Python to Java equivalents.
                        mergeFromKeys(map, other, ((PyStringMap) other).keys());
                    } else if (other.__findattr__("keys") != null) {
                        // This is a dict-like object but addressed by looking up the keys.
                        mergeFromKeys(map, other, other.invoke("keys"));
                    } else {
                        // This should be a sequence of tuples (each an entry).
                        mergeFromSeq(map, other);
                    }
                }
                // update with entries from keyword arguments
                for (int i = 0; i < keywords.length; i++) {
                    String k = keywords[i];
                    Object v = tojava(args[nargs + i]);
                    map.put(k, v);
                }
            } catch (NullPointerException npe) {
                throw nullException();
            }
            return Py.None;
        }

        private void mergeFromKeys(Map<Object, Object> map, PyObject other, PyObject keys) {
            for (PyObject key : keys.asIterable()) {
                Object value = tojava(other.__getitem__(key));
                map.put(tojava(key), value);
            }
        }

        private void mergeFromSeq(Map<Object, Object> map, PyObject other) {
            PyObject pairs = other.__iter__();
            PyObject pair;

            for (int i = 0; (pair = pairs.__iternext__()) != null; i++) {
                try {
                    pair = PySequence.fastSequence(pair, "");
                } catch (PyException pye) {
                    if (pye.match(Py.TypeError)) {
                        throw Py.TypeError(String.format(ERR_SEQ, i));
                    }
                    throw pye;
                }
                int n;
                if ((n = pair.__len__()) != 2) {
                    throw Py.ValueError(String.format(ERR_LENGTH, i, n));
                }
                map.put(tojava(pair.__getitem__(0)), tojava(pair.__getitem__(1)));
            }
        }

        private static final String ERR_SEQ =
                "cannot convert dictionary update element #%d to a sequence";
        private static final String ERR_LENGTH =
                "dictionary update sequence element #%d has length %d; 2 is required";
    };

    private static final PyBuiltinClassMethodNarrow mapFromKeysProxy =
            new MapClassMethod("fromkeys", 1, 2) {

                @Override
                public PyObject __call__(PyObject keys) {
                    return __call__(keys, null);
                }

                @Override
                public PyObject __call__(PyObject keys, PyObject _default) {
                    Object defobj = tojava(_default);
                    Class<? extends Map<Object, Object>> clazz;
                    try {
                        clazz = (Class<Map<Object, Object>>) asClass();
                        Constructor<? extends Map<Object, Object>> ctor =
                                clazz.getDeclaredConstructor();
                        Map<Object, Object> theMap = ctor.newInstance();
                        for (PyObject key : keys.asIterable()) {
                            theMap.put(tojava(key), defobj);
                        }
                        return Py.java2py(theMap);
                    } catch (NullPointerException npe) {
                        throw nullException();
                    } catch (ReflectiveOperationException | SecurityException
                            | IllegalArgumentException e) {
                        throw Py.JavaError(e);
                    }
                }
            };

    static PyBuiltinMethod[] getProxyMethods() {
        //@formatter:off
        return new PyBuiltinMethod[]{
                mapLenProxy,
                // map IterProxy can conflict with Iterable.class;
                // fix after the fact in handleMroError
                mapIterProxy,
                mapReprProxy,
                mapEqProxy,
                mapNeProxy,
                mapLeProxy,
                mapLtProxy,
                mapGeProxy,
                mapGtProxy,
                mapContainsProxy,
                mapGetItemProxy,
                mapPutProxy,
                mapRemoveProxy,
                mapIterItemsProxy,
                mapIterKeysProxy,
                mapIterValuesProxy,
                mapHasKeyProxy,
                mapKeysProxy,
                mapSetDefaultProxy,
                mapPopProxy,
                mapPopItemProxy,
                mapItemsProxy,
                mapCopyProxy,
                mapUpdateProxy,
                mapFromKeysProxy     // class method

        };
        //@formatter:on
    }

    static PyBuiltinMethod[] getPostProxyMethods() {
        return new PyBuiltinMethod[]{
                mapGetProxy,
                mapValuesProxy
        };
    }
}
