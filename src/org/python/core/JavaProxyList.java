package org.python.core;

/**
 * Proxy Java objects implementing java.util.List with Python methods
 * corresponding to the standard list type
 */

import org.python.util.Generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


class JavaProxyList {

    @Untraversable
    private static class ListMethod extends PyBuiltinMethodNarrow {
        protected ListMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        protected ListMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        protected List asList() {
            return (List) self.getJavaProxy();
        }

        protected List newList() {
            try {
                return (List) asList().getClass().newInstance();
            } catch (IllegalAccessException e) {
                throw Py.JavaError(e);
            } catch (InstantiationException e) {
                throw Py.JavaError(e);
            }
        }
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
            List newList;
            try {
                newList = list.getClass().newInstance();
            } catch (Exception e) {
                throw Py.JavaError(e);
            }
            int j = 0;
            for (int i = start; j < n; i += step) {
                newList.add(list.get(i));
                j++;
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

        protected int fixBoundIndex(PyObject index) {
            PyInteger length = Py.newInteger(len());
            if (index._lt(Py.Zero).__nonzero__()) {
                index = index._add(length);
                if (index._lt(Py.Zero).__nonzero__()) {
                    index = Py.Zero;
                }
            } else if (index._gt(length).__nonzero__()) {
                index = length;
            }
            int i = index.asIndex();
            assert i >= 0;
            return i;
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
            if (JyAttribute.getAttr(value, JyAttribute.JAVA_PROXY_ATTR) == this.list) {
                List xs = Generic.list();
                xs.addAll(this.list);
                setsliceList(start, stop, step, xs);
            } else if (value instanceof PyList) {
                setslicePyList(start, stop, step, (PyList) value);
            } else {
                Object valueList = value.__tojava__(List.class);
                if (valueList != null && valueList != Py.NoConversion) {
                    setsliceList(start, stop, step, (List) valueList);
                } else {
                    setsliceIterator(start, stop, step, value.asIterable().iterator());
                }
            }
        }

        final private void setsliceList(int start, int stop, int step, List value) {
            if (step == 1) {
                list.subList(start, stop).clear();
                list.addAll(start, value);
            } else {
                int size = list.size();
                Iterator<Object> iter = value.listIterator();
                for (int j = start; iter.hasNext(); j += step) {
                    Object item = iter.next();
                    if (j >= size) {
                        list.add(item);
                    } else {
                        list.set(j, item);
                    }
                }
            }
        }

        final private void setsliceIterator(int start, int stop, int step, Iterator<PyObject> iter) {
            if (step == 1) {
                List insertion = new ArrayList();
                if (iter != null) {
                    while (iter.hasNext()) {
                        insertion.add(iter.next().__tojava__(Object.class));
                    }
                }
                list.subList(start, stop).clear();
                list.addAll(start, insertion);
            } else {
                int size = list.size();
                for (int j = start; iter.hasNext(); j += step) {
                    Object item = iter.next().__tojava__(Object.class);
                    if (j >= size) {
                        list.add(item);
                    } else {
                        list.set(j, item);
                    }
                }
            }
        }

        final private void setslicePyList(int start, int stop, int step, PyList value) {
            if (step == 1) {
                list.subList(start, stop).clear();
                int n = value.getList().size();
                for (int i = 0, j = start; i < n; i++, j++) {
                    Object item = value.getList().get(i).__tojava__(Object.class);
                    list.add(j, item);
                }
            } else {
                int size = list.size();
                Iterator<PyObject> iter = value.getList().listIterator();
                for (int j = start; iter.hasNext(); j += step) {
                    Object item = iter.next().__tojava__(Object.class);
                    if (j >= size) {
                        list.add(item);
                    } else {
                        list.set(j, item);
                    }
                }
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

    @Untraversable
    private static class ListMulProxyClass extends ListMethod {
        protected ListMulProxyClass(String name, int numArgs) {
            super(name, numArgs);
        }

        @Override
        public PyObject __call__(PyObject obj) {
            List jList = asList();
            int mult = obj.asInt();
            List newList = null;
            // anything below 0 multiplier, we return an empty list
            if (mult > 0) {
                try {
                    newList = new ArrayList(jList.size() * mult);
                    // otherwise, extend it x times, where x is int-cast from obj
                    for (; mult > 0; mult--) {
                        for (Object entry : jList) {
                            newList.add(entry);
                        }
                    }
                } catch (OutOfMemoryError t) {
                    throw Py.MemoryError("");
                }
            } else {
                newList = Collections.EMPTY_LIST;
            }
            return Py.java2py(newList);
        }
    }


    private static class KV {

        private final PyObject key;
        private final Object value;

        KV(PyObject key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class KVComparator implements Comparator<KV> {

        private final PyObject cmp;

        KVComparator(PyObject cmp) {
            this.cmp = cmp;
        }

        public int compare(KV o1, KV o2) {
            int result;
            if (cmp != null && cmp != Py.None) {
                PyObject pyresult = cmp.__call__(o1.key, o2.key);
                if (pyresult instanceof PyInteger || pyresult instanceof PyLong) {
                    return pyresult.asInt();
                } else {
                    throw Py.TypeError(
                            String.format("comparison function must return int, not %.200s",
                                    pyresult.getType().fastGetName()));
                }
            } else {
                result = o1.key._cmp(o2.key);
            }
            return result;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (o instanceof KVComparator) {
                return cmp.equals(((KVComparator) o).cmp);
            }
            return false;
        }
    }

    private synchronized static void list_sort(List list, PyObject cmp, PyObject key, boolean reverse) {
        int size = list.size();
        final ArrayList<KV> decorated = new ArrayList(size);
        for (Object value : list) {
            PyObject pyvalue = Py.java2py(value);
            if (key == null || key == Py.None) {
                decorated.add(new KV(pyvalue, value));
            } else {
                decorated.add(new KV(key.__call__(pyvalue), value));
            }
        }
        // we will rebuild the list from the decorated version
        list.clear();
        KVComparator c = new KVComparator(cmp);
        if (reverse) {
            Collections.reverse(decorated); // maintain stability of sort by reversing first
        }
        Collections.sort(decorated, c);
        if (reverse) {
            Collections.reverse(decorated);
        }
        boolean modified = list.size() > 0;
        for (KV kv : decorated) {
            list.add(kv.value);
        }
        if (modified) {
            throw Py.ValueError("list modified during sort");
        }
    }

    private static final PyBuiltinMethodNarrow listGetProxy = new ListMethod("__getitem__", 1) {
        @Override
        public PyObject __call__(PyObject key) {
            return new ListIndexDelegate(asList()).checkIdxAndGetItem(key);
        }
    };
    private static final PyBuiltinMethodNarrow listSetProxy = new ListMethod("__setitem__", 2) {
        @Override
        public PyObject __call__(PyObject key, PyObject value) {
            new ListIndexDelegate(asList()).checkIdxAndSetItem(key, value);
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow listRemoveProxy = new ListMethod("__delitem__", 1) {
        @Override
        public PyObject __call__(PyObject key) {
            new ListIndexDelegate(asList()).checkIdxAndDelItem(key);
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow listEqProxy = new ListMethod("__eq__", 1) {
        @Override
        public PyObject __call__(PyObject other) {
            List jList = asList();
            if (other.getType().isSubType(PyList.TYPE)) {
                PyList oList = (PyList) other;
                if (jList.size() != oList.size()) {
                    return Py.False;
                }
                for (int i = 0; i < jList.size(); i++) {
                    if (!Py.java2py(jList.get(i))._eq(oList.pyget(i)).__nonzero__()) {
                        return Py.False;
                    }
                }
                return Py.True;
            } else {
                Object oj = other.getJavaProxy();
                if (oj instanceof List) {
                    List oList = (List) oj;
                    if (jList.size() != oList.size()) {
                        return Py.False;
                    }
                    for (int i = 0; i < jList.size(); i++) {
                        if (!Py.java2py(jList.get(i))._eq(
                                Py.java2py(oList.get(i))).__nonzero__()) {
                            return Py.False;
                        }
                    }
                    return Py.True;
                } else {
                    return null;
                }
            }
        }
    };
    private static final PyBuiltinMethodNarrow listAppendProxy = new ListMethod("append", 1) {
        @Override
        public PyObject __call__(PyObject value) {
            asList().add(value);
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow listExtendProxy = new ListMethod("extend", 1) {
        @Override
        public PyObject __call__(PyObject obj) {
            List jList = asList();
            List extension = new ArrayList();

            // Extra step to build the extension list is necessary
            // in case of adding to oneself
            for (PyObject item : obj.asIterable()) {
                extension.add(item);
            }
            jList.addAll(extension);
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow listInsertProxy = new ListMethod("insert", 2) {
        @Override
        public PyObject __call__(PyObject index, PyObject object) {
            List jlist = asList();
            ListIndexDelegate lid = new ListIndexDelegate(jlist);
            int idx = lid.fixBoundIndex(index);
            jlist.add(idx, object);
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow listPopProxy = new ListMethod("pop", 0, 1) {
        @Override
        public PyObject __call__() {
            return __call__(Py.newInteger(-1));
        }

        @Override
        public PyObject __call__(PyObject index) {
            List jlist = asList();
            if (jlist.isEmpty()) {
                throw Py.IndexError("pop from empty list");
            }
            ListIndexDelegate ldel = new ListIndexDelegate(jlist);
            PyObject item = ldel.checkIdxAndFindItem(index.asInt());
            if (item == null) {
                throw Py.IndexError("pop index out of range");
            } else {
                ldel.checkIdxAndDelItem(index);
                return item;
            }
        }
    };
    private static final PyBuiltinMethodNarrow listIndexProxy = new ListMethod("index", 1, 3) {
        @Override
        public PyObject __call__(PyObject object) {
            return __call__(object, Py.newInteger(0), Py.newInteger(asList().size()));
        }

        @Override
        public PyObject __call__(PyObject object, PyObject start) {
            return __call__(object, start, Py.newInteger(asList().size()));
        }

        @Override
        public PyObject __call__(PyObject object, PyObject start, PyObject end) {
            List jlist = asList();
            ListIndexDelegate lid = new ListIndexDelegate(jlist);
            int start_index = lid.fixBoundIndex(start);
            int end_index = lid.fixBoundIndex(end);
            int i = start_index;
            try {
                for (ListIterator it = jlist.listIterator(start_index); it.hasNext(); i++) {
                    if (i == end_index) {
                        break;
                    }
                    Object jobj = it.next();
                    if (Py.java2py(jobj)._eq(object).__nonzero__()) {
                        return Py.newInteger(i);
                    }
                }
            } catch (ConcurrentModificationException e) {
                throw Py.ValueError(object.toString() + " is not in list");
            }
            throw Py.ValueError(object.toString() + " is not in list");
        }
    };
    private static final PyBuiltinMethodNarrow listCountProxy = new ListMethod("count", 1) {
        @Override
        public PyObject __call__(PyObject object) {
            int count = 0;
            List jlist = asList();
            for (int i = 0; i < jlist.size(); i++) {
                Object jobj = jlist.get(i);
                if (Py.java2py(jobj)._eq(object).__nonzero__()) {
                    ++count;
                }
            }
            return Py.newInteger(count);
        }
    };
    private static final PyBuiltinMethodNarrow listReverseProxy = new ListMethod("reverse", 0) {
        @Override
        public PyObject __call__() {
            List jlist = asList();
            Collections.reverse(jlist);
            return Py.None;
        }
    };
    private static final PyBuiltinMethodNarrow listRemoveOverrideProxy = new ListMethod("remove", 1) {
        @Override
        public PyObject __call__(PyObject object) {
            List jlist = asList();
            for (int i = 0; i < jlist.size(); i++) {
                Object jobj = jlist.get(i);
                if (Py.java2py(jobj)._eq(object).__nonzero__()) {
                    jlist.remove(i);
                    return Py.None;
                }
            }
            if (object.isIndex()) {
                // this op is still O(n), but with also the extra O(n) above
                ListIndexDelegate ldel = new ListIndexDelegate(jlist);
                ldel.checkIdxAndDelItem(object);
                return Py.None;
            }
            throw Py.ValueError(object.toString() + " is not in list");
        }
    };
    private static final PyBuiltinMethodNarrow listRAddProxy = new ListMethod("__radd__", 1) {
        @Override
        public PyObject __call__(PyObject obj) {
            // first, clone the self list
            List jList = asList();
            List jClone;
            try {
                jClone = (List) jList.getClass().newInstance();
            } catch (IllegalAccessException e) {
                throw Py.JavaError(e);
            } catch (InstantiationException e) {
                throw Py.JavaError(e);
            }
            for (Object entry : jList) {
                jClone.add(entry);
            }

            // then, extend it with elements from the other list
            // (but, since this is reverse add, we are technically
            // pre-pending the clone with elements from the other list)
            if (obj instanceof Collection) {
                jClone.addAll(0, (Collection) obj);
            } else {
                int i = 0;
                for (PyObject item : obj.asIterable()) {
                    jClone.add(i, item);
                    i++;
                }
            }

            return Py.java2py(jClone);
        }
    };
    private static final PyBuiltinMethodNarrow listIAddProxy = new ListMethod("__iadd__", 1) {
        @Override
        public PyObject __call__(PyObject obj) {
            List jList = asList();
            if (obj instanceof Collection) {
                jList.addAll((Collection) obj);
            } else {
                for (PyObject item : obj.asIterable()) {
                    jList.add(item);
                }
            }
            return self;
        }
    };
    private static final PyBuiltinMethodNarrow listIMulProxy = new ListMethod("__imul__", 1) {
        @Override
        public PyObject __call__(PyObject obj) {
            List jList = asList();
            int mult = obj.asInt();

            // anything below 0 multiplier, we clear the list
            if (mult <= 0) {
                jList.clear();
            } else {
                try {
                    if (jList instanceof ArrayList) {
                        ((ArrayList) jList).ensureCapacity(jList.size() * (mult - 1));
                    }
                    // otherwise, extend it (in-place) x times, where x is int-cast from obj
                    int originalSize = jList.size();
                    for (mult = mult - 1; mult > 0; mult--) {
                        for (int i = 0; i < originalSize; i++) {
                            jList.add(jList.get(i));
                        }
                    }
                } catch (OutOfMemoryError t) {
                    throw Py.MemoryError("");
                }
            }
            return self;
        }
    };
    private static final PyBuiltinMethodNarrow listSortProxy = new ListMethod("sort", 0, 3) {
        @Override
        public PyObject __call__() {
            list_sort(asList(), Py.None, Py.None, false);
            return Py.None;
        }

        @Override
        public PyObject __call__(PyObject cmp) {
            list_sort(asList(), cmp, Py.None, false);
            return Py.None;
        }

        @Override
        public PyObject __call__(PyObject cmp, PyObject key) {
            list_sort(asList(), cmp, key, false);
            return Py.None;
        }

        @Override
        public PyObject __call__(PyObject cmp, PyObject key, PyObject reverse) {
            list_sort(asList(), cmp, key, reverse.__nonzero__());
            return Py.None;
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] kwds) {
            ArgParser ap = new ArgParser("list", args, kwds, new String[]{
                    "cmp", "key", "reverse"}, 0);
            PyObject cmp = ap.getPyObject(0, Py.None);
            PyObject key = ap.getPyObject(1, Py.None);
            PyObject reverse = ap.getPyObject(2, Py.False);
            list_sort(asList(), cmp, key, reverse.__nonzero__());
            return Py.None;
        }
    };

    static PyBuiltinMethod[] getProxyMethods() {
        return new PyBuiltinMethod[]{
                listGetProxy,
                listSetProxy,
                listEqProxy,
                listRemoveProxy,
                listAppendProxy,
                listExtendProxy,
                listInsertProxy,
                listPopProxy,
                listIndexProxy,
                listCountProxy,
                listReverseProxy,
                listRAddProxy,
                listIAddProxy,
                new ListMulProxyClass("__mul__", 1),
                new ListMulProxyClass("__rmul__", 1),
                listIMulProxy
        };
    }

    static PyBuiltinMethod[] getPostProxyMethods() {
        return new PyBuiltinMethod[]{
                listSortProxy,
                listRemoveOverrideProxy
        };
    }

}
