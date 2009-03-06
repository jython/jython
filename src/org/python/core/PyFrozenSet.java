package org.python.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "frozenset", base = PyObject.class)
public class PyFrozenSet extends BaseSet {

    public static final PyType TYPE = PyType.fromClass(PyFrozenSet.class);

    public PyFrozenSet() {
        super(new HashSet<PyObject>());
    }

    public PyFrozenSet(PyObject data) {
        super(_update(new HashSet<PyObject>(), data));
    }

    public PyFrozenSet(PyType type, PyObject data) {
        super(type, _update(new HashSet<PyObject>(), data));
    }

    @ExposedNew
    public static PyObject frozenset___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                             PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("frozenset", args, keywords, new String[] {"iterable"}, 0);
        PyObject iterable = ap.getPyObject(0, null);
        PyFrozenSet fset = null;

        if (new_.for_type == subtype) {
            if (iterable == null) {
                fset = Py.EmptyFrozenSet;
            } else if (iterable.getType() == TYPE) {
                fset = (PyFrozenSet)iterable;
            } else {
                fset = new PyFrozenSet(iterable);
                if (fset.__len__() == 0) {
                    fset = Py.EmptyFrozenSet;
                }
            }
        } else {
            fset = new PyFrozenSetDerived(subtype, iterable);
        }

        return fset;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___cmp__(PyObject o) {
        return new PyInteger(baseset___cmp__(o));
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___ne__(PyObject o) {
        return baseset___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___eq__(PyObject o) {
        return baseset___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___or__(PyObject o) {
        return baseset___or__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___xor__(PyObject o) {
        return baseset___xor__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___sub__(PyObject o) {
        return baseset___sub__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___and__(PyObject o) {
        return baseset___and__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___lt__(PyObject o) {
        return baseset___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___gt__(PyObject o) {
        return baseset___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___ge__(PyObject o) {
        return baseset___ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject frozenset___le__(PyObject o) {
        return baseset___le__(o);
    }

    @ExposedMethod
    final PyObject frozenset___iter__() {
        return baseset___iter__();
    }

    @ExposedMethod
    final boolean frozenset___contains__(PyObject item) {
        return baseset___contains__(item);
    }

    @ExposedMethod
    final PyObject frozenset_copy() {
        if (getClass() == PyFrozenSet.class) {
                return this;
        }
        // subclasses should revert to normal behavior of creating a new instance
        return baseset_copy();
    }

    @ExposedMethod
    final PyObject frozenset_union(PyObject set) {
        return baseset_union(set);
    }

    @ExposedMethod
    final PyObject frozenset_difference(PyObject set) {
        return baseset_difference(set);
    }

    @ExposedMethod
    final PyObject frozenset_symmetric_difference(PyObject set) {
        return baseset_symmetric_difference(set);
    }

    @ExposedMethod
    final PyObject frozenset_intersection(PyObject set) {
        return baseset_intersection(set);
    }

    @ExposedMethod
    final PyObject frozenset_issubset(PyObject set) {
        return baseset_issubset(set);
    }

    @ExposedMethod
    final PyObject frozenset_issuperset(PyObject set) {
        return baseset_issuperset(set);
    }

    @ExposedMethod
    final int frozenset___len__() {
        return baseset___len__();
    }

    @ExposedMethod
    final PyObject frozenset___reduce__() {
        return baseset___reduce__();
    }

    @ExposedMethod
    final int frozenset___hash__() {
        return _set.hashCode();
    }

    @ExposedMethod(names = "__repr__")
    final String frozenset_toString() {
        return baseset_toString();
    }

    public int hashCode() {
        return frozenset___hash__();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public Iterator iterator() {
        final Iterator i = super.iterator();
        return new Iterator() {

            public boolean hasNext() {
                return i.hasNext();
            }
            public Object next() {
                return i.next();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
