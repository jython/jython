package org.python.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Proxy Java objects implementing java.util.List with Python methods
 * corresponding to the standard list type
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

    private static PyObject mapEq(PyObject self, PyObject other) {
        Map<Object, Object> selfMap = ((Map<Object, Object>) self.getJavaProxy());
        if (other.getType().isSubType(PyDictionary.TYPE)) {
            PyDictionary oDict = (PyDictionary) other;
            if (selfMap.size() != oDict.size()) {
                return Py.False;
            }
            for (Object jkey : selfMap.keySet()) {
                Object jval = selfMap.get(jkey);
                PyObject oVal = oDict.__finditem__(Py.java2py(jkey));
                if (oVal == null) {
                    return Py.False;
                }
                if (!Py.java2py(jval)._eq(oVal).__nonzero__()) {
                    return Py.False;
                }
            }
            return Py.True;
        } else {
            Object oj = other.getJavaProxy();
            if (oj instanceof Map) {
                Map<Object, Object> oMap = (Map<Object, Object>) oj;
                return Py.newBoolean(selfMap.equals(oMap));
            } else {
                return null;
            }
        }
    }

    // Map ordering comparisons (lt, le, gt, ge) are based on the key sets;
    // we just define mapLe + mapEq for total ordering of such key sets
    private static PyObject mapLe(PyObject self, PyObject other) {
        Set<Object> selfKeys = ((Map<Object, Object>) self.getJavaProxy()).keySet();
        if (other.getType().isSubType(PyDictionary.TYPE)) {
            PyDictionary oDict = (PyDictionary) other;
            for (Object jkey : selfKeys) {
                if (!oDict.__contains__(Py.java2py(jkey))) {
                    return Py.False;
                }
            }
            return Py.True;
        } else {
            Object oj = other.getJavaProxy();
            if (oj instanceof Map) {
                Map<Object, Object> oMap = (Map<Object, Object>) oj;
                return Py.newBoolean(oMap.keySet().containsAll(selfKeys));
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
            StringBuilder repr = new StringBuilder("{");
            for (Map.Entry<Object, Object> entry : asMap().entrySet()) {
                Object jkey = entry.getKey();
                Object jval = entry.getValue();
                repr.append(jkey.toString());
                repr.append(": ");
                repr.append(jval == asMap() ? "{...}" : (jval == null ? "None" : jval.toString()));
                repr.append(", ");
            }
            int lastindex = repr.lastIndexOf(", ");
            if (lastindex > -1) {
                repr.delete(lastindex, lastindex + 2);
            }
            repr.append("}");
            return new PyString(repr.toString());
        }
    };
    private static final PyBuiltinMethodNarrow mapEqProxy = new MapMethod("__eq__", 1) {
        @Override
        public PyObject __call__(PyObject other) {
            return mapEq(self, other);
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
            Object other = obj.__tojava__(Object.class);
            return asMap().containsKey(other) ? Py.True : Py.False;
        }
    };
    // "get" needs to override java.util.Map#get() in its subclasses, too, so this needs to be injected last
    // (i.e. when HashMap is loaded not when it is recursively loading its super-type Map)
    private static final PyBuiltinMethodNarrow mapGetProxy = new MapMethod("get", 1, 2) {
        @Override
        public PyObject __call__(PyObject key) {
            return __call__(key, Py.None);
        }

        @Override
        public PyObject __call__(PyObject key, PyObject _default) {
            Object jkey = Py.tojava(key, Object.class);
            if (asMap().containsKey(jkey)) {
                return Py.java2py(asMap().get(jkey));
            } else {
                return _default;
            }
        }
    };
    private static final PyBuiltinMethodNarrow mapGetItemProxy = new MapMethod("__getitem__", 1) {
        @Override
        public PyObject __call__(PyObject key) {
            Object jkey = Py.tojava(key, Object.class);
            if (asMap().containsKey(jkey)) {
                return Py.java2py(asMap().get(jkey));
            } else {
                throw Py.KeyError(key);
            }
        }
    };
    private static final PyBuiltinMethodNarrow mapPutProxy = new MapMethod("__setitem__", 2) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            asMap().put(Py.tojava(key, Object.class),
                    value == Py.None ? Py.None : Py.tojava(value, Object.class));
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow mapRemoveProxy = new MapMethod("__delitem__", 1) {
        @Override
        public PyObject __call__(PyObject key) {
            Object jkey = Py.tojava(key, Object.class);
            if (asMap().remove(jkey) == null) {
                throw Py.KeyError(key);
            }
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow mapIterItemsProxy = new MapMethod("iteritems", 0) {
        @Override
        public PyObject __call__() {
            final Iterator<Map.Entry<Object, Object>> entrySetIterator = asMap().entrySet().iterator();
            return new PyIterator() {
                @Override
                public PyObject __iternext__() {
                    if (entrySetIterator.hasNext()) {
                        Map.Entry<Object, Object> nextEntry = entrySetIterator.next();
                        // yield a Python tuple object (key, value)
                        return new PyTuple(Py.java2py(nextEntry.getKey()),
                                Py.java2py(nextEntry.getValue()));
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
            return asMap().containsKey(Py.tojava(key, Object.class)) ? Py.True : Py.False;
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
    private static final PyBuiltinMethodNarrow mapSetDefaultProxy = new MapMethod("setdefault", 1, 2) {
        @Override
        public PyObject __call__(PyObject key) {
            return __call__(key, Py.None);
        }

        @Override
        public PyObject __call__(PyObject key, PyObject _default) {
            Object jkey = Py.tojava(key, Object.class);
            Object jval = asMap().get(jkey);
            if (jval == null) {
                asMap().put(jkey, _default == Py.None ? Py.None : Py.tojava(_default, Object.class));
                return _default;
            }
            return Py.java2py(jval);
        }
    };
    private static final PyBuiltinMethodNarrow mapPopProxy = new MapMethod("pop", 1, 2) {
        @Override
        public PyObject __call__(PyObject key) {
            return __call__(key, null);
        }

        @Override
        public PyObject __call__(PyObject key, PyObject _default) {
            Object jkey = Py.tojava(key, Object.class);
            if (asMap().containsKey(jkey)) {
                PyObject value = Py.java2py(asMap().remove(jkey));
                assert (value != null);
                return Py.java2py(value);
            } else {
                if (_default == null) {
                    throw Py.KeyError(key);
                }
                return _default;
            }
        }
    };
    private static final PyBuiltinMethodNarrow mapPopItemProxy = new MapMethod("popitem", 0) {
        @Override
        public PyObject __call__() {
            if (asMap().size() == 0) {
                throw Py.KeyError("popitem(): map is empty");
            }
            Object key = asMap().keySet().toArray()[0];
            Object val = asMap().remove(key);
            return Py.java2py(val);
        }
    };
    private static final PyBuiltinMethodNarrow mapItemsProxy = new MapMethod("items", 0) {
        @Override
        public PyObject __call__() {
            PyList items = new PyList();
            for (Map.Entry<Object, Object> entry : asMap().entrySet()) {
                items.add(new PyTuple(Py.java2py(entry.getKey()),
                        Py.java2py(entry.getValue())));
            }
            return items;
        }
    };
    private static final PyBuiltinMethodNarrow mapCopyProxy = new MapMethod("copy", 0) {
        @Override
        public PyObject __call__() {
            Map<Object, Object> jmap = asMap();
            Map<Object, Object> jclone;
            try {
                jclone = (Map<Object, Object>) jmap.getClass().newInstance();
            } catch (IllegalAccessException e) {
                throw Py.JavaError(e);
            } catch (InstantiationException e) {
                throw Py.JavaError(e);
            }
            for (Map.Entry<Object, Object> entry : jmap.entrySet()) {
                jclone.put(entry.getKey(), entry.getValue());
            }
            return Py.java2py(jclone);
        }
    };
    private static final PyBuiltinMethodNarrow mapUpdateProxy = new MapMethod("update", 0, 1) {
        private Map<Object, Object> jmap;

        @Override
        public PyObject __call__() {
            return Py.None;
        }

        @Override
        public PyObject __call__(PyObject other) {
            // `other` is either another dict-like object, or an iterable of key/value pairs (as tuples
            // or other iterables of length two)
            return __call__(new PyObject[]{other}, new String[]{});
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] keywords) {
            if ((args.length - keywords.length) != 1) {
                throw info.unexpectedCall(args.length, false);
            }
            jmap = asMap();
            PyObject other = args[0];
            // update with entries from `other` (adapted from their equivalent in PyDictionary#update)
            Object proxy = other.getJavaProxy();
            if (proxy instanceof Map) {
                merge((Map<Object, Object>) proxy);
            } else if (other.__findattr__("keys") != null) {
                merge(other);
            } else {
                mergeFromSeq(other);
            }
            // update with entries from keyword arguments
            for (int i = 0; i < keywords.length; i++) {
                String jkey = keywords[i];
                PyObject value = args[1 + i];
                jmap.put(jkey, Py.tojava(value, Object.class));
            }
            return Py.None;
        }

        private void merge(Map<Object, Object> other) {
            for (Map.Entry<Object, Object> entry : other.entrySet()) {
                jmap.put(entry.getKey(), entry.getValue());
            }
        }

        private void merge(PyObject other) {
            if (other instanceof PyDictionary) {
                jmap.putAll(((PyDictionary) other).getMap());
            } else if (other instanceof PyStringMap) {
                mergeFromKeys(other, ((PyStringMap) other).keys());
            } else {
                mergeFromKeys(other, other.invoke("keys"));
            }
        }

        private void mergeFromKeys(PyObject other, PyObject keys) {
            for (PyObject key : keys.asIterable()) {
                jmap.put(Py.tojava(key, Object.class),
                        Py.tojava(other.__getitem__(key), Object.class));
            }
        }

        private void mergeFromSeq(PyObject other) {
            PyObject pairs = other.__iter__();
            PyObject pair;

            for (int i = 0; (pair = pairs.__iternext__()) != null; i++) {
                try {
                    pair = PySequence.fastSequence(pair, "");
                } catch (PyException pye) {
                    if (pye.match(Py.TypeError)) {
                        throw Py.TypeError(String.format("cannot convert dictionary update sequence "
                                + "element #%d to a sequence", i));
                    }
                    throw pye;
                }
                int n;
                if ((n = pair.__len__()) != 2) {
                    throw Py.ValueError(String.format("dictionary update sequence element #%d "
                            + "has length %d; 2 is required", i, n));
                }
                jmap.put(Py.tojava(pair.__getitem__(0), Object.class),
                        Py.tojava(pair.__getitem__(1), Object.class));
            }
        }
    };
    private static final PyBuiltinClassMethodNarrow mapFromKeysProxy = new MapClassMethod("fromkeys", 1, 2) {
        @Override
        public PyObject __call__(PyObject keys) {
            return __call__(keys, null);
        }

        @Override
        public PyObject __call__(PyObject keys, PyObject _default) {
            Object defobj = _default == null ? Py.None : Py.tojava(_default, Object.class);
            Class<?> theClass = asClass();
            try {
                // always injected to java.util.Map, so we know the class object we get from asClass is subtype of java.util.Map
                Map<Object, Object> theMap = (Map<Object, Object>) theClass.newInstance();
                for (PyObject key : keys.asIterable()) {
                    theMap.put(Py.tojava(key, Object.class), defobj);
                }
                return Py.java2py(theMap);
            } catch (InstantiationException e) {
                throw Py.JavaError(e);
            } catch (IllegalAccessException e) {
                throw Py.JavaError(e);
            }
        }
    };

    static PyBuiltinMethod[] getProxyMethods() {
        return new PyBuiltinMethod[]{
                mapLenProxy,
                // map IterProxy can conflict with Iterable.class;
                // fix after the fact in handleMroError
                mapIterProxy,
                mapReprProxy,
                mapEqProxy,
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
    }

    static PyBuiltinMethod[] getPostProxyMethods() {
        return new PyBuiltinMethod[]{
                mapGetProxy,
                mapValuesProxy
        };
    }
}
