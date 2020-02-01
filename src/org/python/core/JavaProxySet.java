package org.python.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Proxy objects implementing java.util.Set
 */
class JavaProxySet {

    @Untraversable
    private static class SetMethod extends PyBuiltinMethodNarrow {

        protected SetMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        protected SetMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        @SuppressWarnings("unchecked")
        protected Set<Object> asSet() {
            return (Set<Object>) self.getJavaProxy();
        }

        // Unlike list and dict, set maintains the derived type for the set
        // so we replicate this behavior
        protected PyObject makePySet(Set newSet) {
            PyObject newPySet = self.getType().__call__();
            @SuppressWarnings("unchecked")
            Set<Object> jSet = ((Set<Object>) newPySet.getJavaProxy());
            jSet.addAll(newSet);
            return newPySet;
        }

        /**
         * Compares this object with other to check for equality. Used to implement __eq __ and
         * __ne__. May return null if the other object cannot be compared i.e. is not a Python or
         * Java set.
         *
         * @param other The object to compare to this
         * @return true is equal, false if not equal and null if we can't compare
         */
        protected PyBoolean isEqual(PyObject other) {
            if (isPySet(other)) {
                // Being compared to a Python set
                final Set<PyObject> otherPySet = ((BaseSet) other).getSet();
                final Set<Object> selfSet = asSet();
                if (selfSet.size() != otherPySet.size()) {
                    // Sets are different sizes therefore not equal
                    return Py.False;
                }
                // Do element by element comparison, if any elements are not contained return false
                for (Object obj : selfSet) {
                    if (!otherPySet.contains(Py.java2py(obj))) {
                        return Py.False;
                    }
                }
                // All elements are equal so the sets are equal
                return Py.True;
            } else {
                // Being compared to something that is not a Python set
                final Object oj = other.getJavaProxy();
                if (oj instanceof Set) {
                    // Being compared to Java Set convert to Python set and call recursively
                    final PySet otherPySet = new PySet(Py.javas2pys(((Set) oj).toArray()));
                    return isEqual(otherPySet);
                } else {
                    // other is not a Python or Java set, so we don't know if
                    // were equal therefore return null
                    return null;
                }
            }
        }

        public boolean isSuperset(PyObject other) {
            Set<Object> selfSet = asSet();
            Object oj = other.getJavaProxy();
            if (oj != null && oj instanceof Set) {
                Set otherSet = (Set) oj;
                return selfSet.containsAll(otherSet);
            } else if (isPySet(other)) {
                Set<PyObject> otherPySet = ((BaseSet) other).getSet();
                for (PyObject pyobj : otherPySet) {
                    if (!selfSet.contains(pyobj.__tojava__(Object.class))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        public boolean isSubset(PyObject other) {
            Set<Object> selfSet = asSet();
            Object oj = other.getJavaProxy();
            if (oj != null && oj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<Object> otherSet = (Set<Object>) oj;
                return otherSet.containsAll(selfSet);
            } else if (isPySet(other)) {
                Set<PyObject> otherPySet = ((BaseSet) other).getSet();
                for (Object obj : selfSet) {
                    if (!otherPySet.contains(Py.java2py(obj))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        protected Set difference(Collection<Object> other) {
            Set<Object> selfSet = asSet();
            Set<Object> diff = new HashSet<>(selfSet);
            diff.removeAll(other);
            return diff;
        }

        protected void differenceUpdate(Collection other) {
            asSet().removeAll(other);
        }

        protected Set intersect(Collection[] others) {
            Set<Object> selfSet = asSet();
            Set<Object> intersection = new HashSet<>(selfSet);
            for (Collection other : others) {
                intersection.retainAll(other);
            }
            return intersection;
        }

        protected void intersectUpdate(Collection[] others) {
            Set<Object> selfSet = asSet();
            for (Collection other : others) {
                selfSet.retainAll(other);
            }
        }

        protected Set union(Collection<Object> other) {
            Set<Object> selfSet = asSet();
            Set<Object> u = new HashSet<>(selfSet);
            u.addAll(other);
            return u;
        }

        protected void update(Collection<Object> other) {
            asSet().addAll(other);
        }

        protected Set symDiff(Collection<Object> other) {
            Set<Object> selfSet = asSet();
            Set<Object> symDiff = new HashSet<>(selfSet);
            symDiff.addAll(other);
            Set<Object> intersection = new HashSet<>(selfSet);
            intersection.retainAll(other);
            symDiff.removeAll(intersection);
            return symDiff;
        }

        protected void symDiffUpdate(Collection<Object> other) {
            Set<Object> selfSet = asSet();
            Set<Object> intersection = new HashSet<>(selfSet);
            intersection.retainAll(other);
            selfSet.addAll(other);
            selfSet.removeAll(intersection);
        }
    }

    @Untraversable
    private static class SetMethodVarargs extends SetMethod {

        protected SetMethodVarargs(String name) {
            super(name, 0, -1);
        }

        @Override
        public PyObject __call__() {
            return __call__(Py.EmptyObjects);
        }

        @Override
        public PyObject __call__(PyObject obj) {
            return __call__(new PyObject[] {obj});
        }

        @Override
        public PyObject __call__(PyObject obj1, PyObject obj2) {
            return __call__(new PyObject[] {obj1, obj2});
        }

        @Override
        public PyObject __call__(PyObject obj1, PyObject obj2, PyObject obj3) {
            return __call__(new PyObject[] {obj1, obj2, obj3});
        }

        @Override
        public PyObject __call__(PyObject obj1, PyObject obj2, PyObject obj3, PyObject obj4) {
            return __call__(new PyObject[] {obj1, obj2, obj3, obj4});
        }
    }

    private static boolean isPySet(PyObject obj) {
        PyType type = obj.getType();
        return type.isSubType(PySet.TYPE) || type.isSubType(PyFrozenSet.TYPE);
    }

    private static Collection<Object> getJavaSet(PyObject self, String op, PyObject obj) {
        Collection<Object> items;
        if (isPySet(obj)) {
            Set<PyObject> otherPySet = ((BaseSet) obj).getSet();
            items = new ArrayList<>(otherPySet.size());
            for (PyObject pyobj : otherPySet) {
                items.add(pyobj.__tojava__(Object.class));
            }
        } else {
            Object oj = obj.getJavaProxy();
            if (oj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<Object> jSet = (Set<Object>) oj;
                items = jSet;
            } else {
                throw Py.TypeError(
                        String.format("unsupported operand type(s) for %s: '%.200s' and '%.200s'",
                                op, self.getType().fastGetName(), obj.getType().fastGetName()));
            }
        }
        return items;
    }

    private static Collection<Object> getJavaCollection(PyObject obj) {
        Collection<Object> items;
        Object oj = obj.getJavaProxy();
        if (oj != null) {
            if (oj instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> jCollection = (Collection<Object>) oj;
                items = jCollection;
            } else if (oj instanceof Iterable) {
                items = new HashSet<>();
                for (Object item : (Iterable) oj) {
                    items.add(item);
                }
            } else {
                throw Py.TypeError(String.format("unsupported operand type(s): '%.200s'",
                        obj.getType().fastGetName()));
            }
        } else {
            // This step verifies objects are hashable
            items = new HashSet<>();
            for (PyObject pyobj : obj.asIterable()) {
                items.add(pyobj.__tojava__(Object.class));
            }
        }
        return items;
    }

    private static Collection<Object>[] getJavaCollections(PyObject[] objs) {
        Collection[] collections = new Collection[objs.length];
        int i = 0;
        for (PyObject obj : objs) {
            collections[i++] = getJavaCollection(obj);
        }
        return collections;
    }

    private static Collection<Object> getCombinedJavaCollections(PyObject[] objs) {
        if (objs.length == 0) {
            return Collections.emptyList();
        }
        if (objs.length == 1) {
            return getJavaCollection(objs[0]);
        }
        Set<Object> items = new HashSet<>();
        for (PyObject obj : objs) {
            Object oj = obj.getJavaProxy();
            if (oj != null) {
                if (oj instanceof Iterable) {
                    for (Object item : (Iterable) oj) {
                        items.add(item);
                    }
                } else {
                    throw Py.TypeError(String.format("unsupported operand type(s): '%.200s'",
                            obj.getType().fastGetName()));
                }
            } else {
                for (PyObject pyobj : obj.asIterable()) {
                    items.add(pyobj.__tojava__(Object.class));
                }
            }
        }
        return items;
    }

    private static final SetMethod cmpProxy = new SetMethod("__cmp__", 1) {

        @Override
        public PyObject __call__(PyObject value) {
            throw Py.TypeError("cannot compare sets using cmp()");
        }
    };
    private static final SetMethod eqProxy = new SetMethod("__eq__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return isEqual(other);
        }
    };
    private static final SetMethod neProxy = new SetMethod("__ne__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            // isEqual may return null if we don't know how to compare to other.
            PyBoolean equal = isEqual(other);
            if (equal != null) {
                // implement NOT equal by the inverse of equal
                return isEqual(other).__not__();
            }
            return null;
        }
    };
    private static final SetMethod ltProxy = new SetMethod("__lt__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return isEqual(other).__not__().__and__(Py.newBoolean(isSubset(other)));
        }
    };

    @Untraversable
    private static class IsSubsetMethod extends SetMethod {
        // __le__, issubset

        protected IsSubsetMethod(String name) {
            super(name, 1);
        }

        @Override
        public PyObject __call__(PyObject other) {
            return Py.newBoolean(isSubset(other));
        }
    }

    @Untraversable
    private static class IsSupersetMethod extends SetMethod {
        // __ge__, issuperset

        protected IsSupersetMethod(String name) {
            super(name, 1);
        }

        @Override
        public PyObject __call__(PyObject other) {
            return Py.newBoolean(isSuperset(other));
        }
    }

    private static final SetMethod gtProxy = new SetMethod("__gt__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return isEqual(other).__not__().__and__(Py.newBoolean(isSuperset(other)));
        }
    };

    private static final SetMethod isDisjointProxy = new SetMethod("isdisjoint", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            Collection[] otherJava = new Collection[] {getJavaCollection(other)};
            return Py.newBoolean(intersect(otherJava).size() == 0);
        }
    };

    private static final SetMethod differenceProxy = new SetMethodVarargs("difference") {

        @Override
        public PyObject __call__(PyObject[] others) {
            return makePySet(difference(getCombinedJavaCollections(others)));
        }
    };

    private static final SetMethod differenceUpdateProxy =
            new SetMethodVarargs("difference_update") {

                @Override
                public PyObject __call__(PyObject[] others) {
                    differenceUpdate(getCombinedJavaCollections(others));
                    return Py.None;
                }
            };

    private static final SetMethod subProxy = new SetMethod("__sub__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return makePySet(difference(getJavaSet(self, "-", other)));
        }
    };

    private static final SetMethod isubProxy = new SetMethod("__isub__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            differenceUpdate(getJavaSet(self, "-=", other));
            return self;
        }
    };

    private static final SetMethod intersectionProxy = new SetMethodVarargs("intersection") {

        @Override
        public PyObject __call__(PyObject[] others) {
            return makePySet(intersect(getJavaCollections(others)));
        }
    };

    private static final SetMethod intersectionUpdateProxy =
            new SetMethodVarargs("intersection_update") {

                @Override
                public PyObject __call__(PyObject[] others) {
                    intersectUpdate(getJavaCollections(others));
                    return Py.None;
                }
            };

    private static final SetMethod andProxy = new SetMethod("__and__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return makePySet(intersect(new Collection[] {getJavaSet(self, "&", other)}));
        }
    };

    private static final SetMethod iandProxy = new SetMethod("__iand__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            intersectUpdate(new Collection[] {getJavaSet(self, "&=", other)});
            return self;
        }
    };

    private static final SetMethod symDiffProxy = new SetMethod("symmetric_difference", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return makePySet(symDiff(getJavaCollection(other)));
        }
    };

    private static final SetMethod symDiffUpdateProxy =
            new SetMethod("symmetric_difference_update", 1) {

                @Override
                public PyObject __call__(PyObject other) {
                    symDiffUpdate(getJavaCollection(other));
                    return Py.None;
                }
            };

    private static final SetMethod xorProxy = new SetMethod("__xor__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return makePySet(symDiff(getJavaSet(self, "^", other)));
        }
    };

    private static final SetMethod ixorProxy = new SetMethod("__ixor__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            symDiffUpdate(getJavaSet(self, "^=", other));
            return self;
        }
    };

    private static final SetMethod unionProxy = new SetMethodVarargs("union") {

        @Override
        public PyObject __call__(PyObject[] others) {
            return makePySet(union(getCombinedJavaCollections(others)));
        }
    };

    private static final SetMethod updateProxy = new SetMethodVarargs("update") {

        @Override
        public PyObject __call__(PyObject[] others) {
            update(getCombinedJavaCollections(others));
            return Py.None;
        }
    };

    private static final SetMethod orProxy = new SetMethod("__or__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            return makePySet(union(getJavaSet(self, "|", other)));
        }
    };

    private static final SetMethod iorProxy = new SetMethod("__ior__", 1) {

        @Override
        public PyObject __call__(PyObject other) {
            update(getJavaSet(self, "|=", other));
            return self;
        }
    };

    @Untraversable
    private static class CopyMethod extends SetMethod {

        protected CopyMethod(String name) {
            super(name, 0);
        }

        @Override
        public PyObject __call__() {
            return makePySet(asSet());
        }
    }

    private static final SetMethod deepcopyOverrideProxy = new SetMethod("__deepcopy__", 1) {

        @Override
        public PyObject __call__(PyObject memo) {
            Set<Object> newSet = new HashSet<>();
            for (Object obj : asSet()) {
                PyObject pyobj = Py.java2py(obj);
                PyObject newobj = pyobj.invoke("__deepcopy__", memo);
                newSet.add(newobj.__tojava__(Object.class));
            }
            return makePySet(newSet);
        }
    };

    private static final SetMethod reduceProxy = new SetMethod("__reduce__", 0) {

        @Override
        public PyObject __call__() {
            PyObject args = new PyTuple(new PyList(new JavaIterator(asSet())));
            PyObject dict = __findattr__("__dict__");
            if (dict == null) {
                dict = Py.None;
            }
            return new PyTuple(self.getType(), args, dict);
        }
    };

    private static final SetMethod containsProxy = new SetMethod("__contains__", 1) {

        @Override
        public PyObject __call__(PyObject value) {
            return Py.newBoolean(asSet().contains(value.__tojava__(Object.class)));
        }
    };
    private static final SetMethod hashProxy = new SetMethod("__hash__", 0) {

        // in general, we don't know if this is really true or not
        @Override
        public PyObject __call__(PyObject value) {
            throw Py.TypeError(
                    String.format("unhashable type: '%.200s'", self.getType().fastGetName()));
        }
    };

    private static final SetMethod discardProxy = new SetMethod("discard", 1) {

        @Override
        public PyObject __call__(PyObject value) {
            asSet().remove(value.__tojava__(Object.class));
            return Py.None;
        }
    };
    private static final SetMethod popProxy = new SetMethod("pop", 0) {

        @Override
        public PyObject __call__() {
            Set selfSet = asSet();
            Iterator it;
            if (selfSet instanceof NavigableSet) {
                it = ((NavigableSet) selfSet).descendingIterator();
            } else {
                it = selfSet.iterator();
            }
            try {
                PyObject value = Py.java2py(it.next());
                it.remove();
                return value;
            } catch (NoSuchElementException ex) {
                throw Py.KeyError("pop from an empty set");
            }
        }
    };
    private static final SetMethod removeOverrideProxy = new SetMethod("remove", 1) {

        @Override
        public PyObject __call__(PyObject value) {
            boolean removed = asSet().remove(value.__tojava__(Object.class));
            if (!removed) {
                throw Py.KeyError(value);
            }
            return Py.None;
        }
    };

    static PyBuiltinMethod[] getProxyMethods() {
        //@formatter:off
        return new PyBuiltinMethod[]{
                cmpProxy,
                eqProxy,
                neProxy,
                ltProxy,
                new IsSubsetMethod("__le__"),
                new IsSubsetMethod("issubset"),
                new IsSupersetMethod("__ge__"),
                new IsSupersetMethod("issuperset"),
                gtProxy,
                isDisjointProxy,

                differenceProxy,
                differenceUpdateProxy,
                subProxy,
                isubProxy,

                intersectionProxy,
                intersectionUpdateProxy,
                andProxy,
                iandProxy,

                symDiffProxy,
                symDiffUpdateProxy,
                xorProxy,
                ixorProxy,

                unionProxy,
                updateProxy,
                orProxy,
                iorProxy,

                new CopyMethod("copy"),
                new CopyMethod("__copy__"),
                reduceProxy,

                containsProxy,
                hashProxy,

                discardProxy,
                popProxy
        };
        //@formatter:on
    }

    static PyBuiltinMethod[] getPostProxyMethods() {
        //@formatter:off
        return new PyBuiltinMethod[]{
                deepcopyOverrideProxy,
                removeOverrideProxy
        };
        //@formatter:on
    }

}
