package org.python.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.util.Generic;

@ExposedType(name = "set", base = PyObject.class, doc = BuiltinDocs.set_doc)
public class PySet extends BaseSet {

    public static final PyType TYPE = PyType.fromClass(PySet.class);

    public PySet() {
        this(TYPE);
    }

    public PySet(PyType type) {
        super(type, Generic.<PyObject>concurrentSet());
    }

    public PySet(PyObject data) {
        super(TYPE, _update(Generic.<PyObject>concurrentSet(), data));
    }

    public PySet(PyObject[] data) {
        super(TYPE, _update(Generic.<PyObject>concurrentSet(), data));
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.set___init___doc)
    final void set___init__(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinCallable.DefaultInfo.unexpectedCall(nargs, false, "Set", 0, 1);
        }
        if (nargs == 0) {
            return;
        }

        _set.clear();
        _update(args[0]);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___cmp___doc)
    final PyObject set___cmp__(PyObject o) {
        return new PyInteger(baseset___cmp__(o));
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___ne___doc)
    final PyObject set___ne__(PyObject o) {
        return baseset___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___eq___doc)
    final PyObject set___eq__(PyObject o) {
        return baseset___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___or___doc)
    final PyObject set___or__(PyObject o) {
        return baseset___or__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___xor___doc)
    final PyObject set___xor__(PyObject o) {
        return baseset___xor__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___sub___doc)
    final PyObject set___sub__(PyObject o) {
        return baseset___sub__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___and___doc)
    final PyObject set___and__(PyObject o) {
        return baseset___and__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___lt___doc)
    final PyObject set___lt__(PyObject o) {
        return baseset___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___gt___doc)
    final PyObject set___gt__(PyObject o) {
        return baseset___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___ge___doc)
    final PyObject set___ge__(PyObject o) {
        return baseset___ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___le___doc)
    final PyObject set___le__(PyObject o) {
        return baseset___le__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.set___iter___doc)
    final PyObject set___iter__() {
        return baseset___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.set___contains___doc)
    final boolean set___contains__(PyObject item) {
        return baseset___contains__(item);
    }

    @ExposedMethod(doc = BuiltinDocs.set_copy_doc)
    final PyObject set_copy() {
        return baseset_copy();
    }

    @ExposedMethod(doc = BuiltinDocs.set_union_doc)
    final PyObject set_union(PyObject set) {
        return baseset_union(set);
    }

    @ExposedMethod(doc = BuiltinDocs.set_difference_doc)
    final PyObject set_difference(PyObject set) {
        return baseset_difference(set);
    }

    @ExposedMethod(doc = BuiltinDocs.set_symmetric_difference_doc)
    final PyObject set_symmetric_difference(PyObject set) {
        return baseset_symmetric_difference(set);
    }

    @ExposedMethod(doc = BuiltinDocs.set_intersection_doc)
    final PyObject set_intersection(PyObject set) {
        return baseset_intersection(set);
    }

    @ExposedMethod(doc = BuiltinDocs.set_issubset_doc)
    final PyObject set_issubset(PyObject set) {
        return baseset_issubset(set);
    }

    @ExposedMethod(doc = BuiltinDocs.set_issuperset_doc)
    final PyObject set_issuperset(PyObject set) {
        return baseset_issuperset(set);
    }

    @ExposedMethod(doc = BuiltinDocs.set___len___doc)
    final int set___len__() {
        return baseset___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.set___reduce___doc)
    final PyObject set___reduce__() {
        return baseset___reduce__();
    }

    public PyObject __ior__(PyObject other) {
        return set___ior__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___ior___doc)
    final PyObject set___ior__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        _set.addAll(((BaseSet)other)._set);
        return this;
    }

    public PyObject __ixor__(PyObject other) {
        return set___ixor__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___ixor___doc)
    final PyObject set___ixor__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        set_symmetric_difference_update(other);
        return this;
    }

    public PyObject __iand__(PyObject other) {
        return set___iand__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___iand___doc)
    final PyObject set___iand__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        _set = ((BaseSet)__and__(other))._set;
        return this;
    }

    public PyObject __isub__(PyObject other) {
        return set___isub__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.set___isub___doc)
    final PyObject set___isub__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            return null;
        }
        _set.removeAll(((BaseSet)other)._set);
        return this;
    }

    public int hashCode() {
        return set___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.set___hash___doc)
    final int set___hash__() {
        throw Py.TypeError("set objects are unhashable");
    }

    @ExposedMethod(doc = BuiltinDocs.set_add_doc)
    final void set_add(PyObject o) {
        _set.add(o);
    }

    @ExposedMethod(doc = BuiltinDocs.set_remove_doc)
    final void set_remove(PyObject o) {
        boolean b = false;
        try {
            b = _set.remove(o);
        } catch (PyException e) {
            PyObject frozen = asFrozen(e, o);
            b = _set.remove(frozen);
        }
        if (!b) {
            throw new PyException(Py.KeyError, o);
        }
    }

    @ExposedMethod(doc = BuiltinDocs.set_discard_doc)
    final void set_discard(PyObject o) {
        try {
            _set.remove(o);
        } catch (PyException e) {
            PyObject frozen = asFrozen(e, o);
            _set.remove(frozen);
        }
    }

    @ExposedMethod(doc = BuiltinDocs.set_pop_doc)
    final PyObject set_pop() {
        Iterator iterator = _set.iterator();
        try {
            Object first = iterator.next();
            _set.remove(first);
            return (PyObject)first;
        } catch (NoSuchElementException e) {
            throw new PyException(Py.KeyError, "pop from an empty set");
        }
    }

    @ExposedMethod(doc = BuiltinDocs.set_clear_doc)
    final void set_clear() {
        _set.clear();
    }

    @ExposedMethod(doc = BuiltinDocs.set_update_doc)
    final void set_update(PyObject data) {
        _update(data);
    }

    @ExposedMethod(doc = BuiltinDocs.set_intersection_update_doc)
    final void set_intersection_update(PyObject other) {
        if (other instanceof BaseSet) {
            __iand__(other);
        } else {
            BaseSet set = (BaseSet)baseset_intersection(other);
            _set = set._set;
        }
    }

    @ExposedMethod(doc = BuiltinDocs.set_symmetric_difference_update_doc)
    final void set_symmetric_difference_update(PyObject other) {
        if (this == other) {
            set_clear();
            return;
        }

        BaseSet bs = (other instanceof BaseSet) ? (BaseSet)other : new PySet(other);
        for (PyObject o : bs._set) {
            if (_set.contains(o)) {
                _set.remove(o);
            } else {
                _set.add(o);
            }
        }
    }

    @ExposedMethod(doc = BuiltinDocs.set_difference_update_doc)
    final void set_difference_update(PyObject other) {
        if (other instanceof BaseSet) {
            __isub__(other);
            return;
        }
        for (PyObject o : other.asIterable()) {
            if (__contains__(o)) {
                _set.remove(o);
            }
        }
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.set___repr___doc)
    final String set_toString() {
        return baseset_toString();
    }
}
