package org.python.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "set", base = PyObject.class)
public class PySet extends BaseSet {

    public static final PyType TYPE = PyType.fromClass(PySet.class);
    
    public PySet() {
        super();
    }

    public PySet(PyType type) {
        super(type);
    }

    public PySet(PyObject data) {
        super(data);
    }
    
    @ExposedNew
    @ExposedMethod
    final void set___init__(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, "Set", 0, 1);
        }
        if (nargs == 0) {
            return;
        }

        this._set.clear();
        PyObject o = args[0];
        _update(o);
    }
    
    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___cmp__(PyObject o) {
        return new PyInteger(baseset___cmp__(o));
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___ne__(PyObject o) {
        return baseset___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___eq__(PyObject o) {
        return baseset___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___or__(PyObject o) {
        return baseset___or__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___xor__(PyObject o) {
        return baseset___xor__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___sub__(PyObject o) {
        return baseset___sub__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___and__(PyObject o) {
        return baseset___and__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___lt__(PyObject o) {
        return baseset___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___gt__(PyObject o) {
        return baseset___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___ge__(PyObject o) {
        return baseset___ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___le__(PyObject o) {
        return baseset___le__(o);
    }

    @ExposedMethod
    final PyObject set___iter__() {
        return baseset___iter__();
    }

    @ExposedMethod
    final boolean set___contains__(PyObject item) {
        return baseset___contains__(item);
    }

    @ExposedMethod
    final PyObject set___deepcopy__(PyObject memo) {
        return baseset___deepcopy__(memo);
    }

    @ExposedMethod
    final boolean set___nonzero__() {
        return baseset___nonzero__();
    }

    @ExposedMethod
    final PyObject set_copy() {
        return baseset_copy();
    }

    @ExposedMethod
    final PyObject set_union(PyObject set) {
        return baseset_union(set);
    }

    @ExposedMethod
    final PyObject set_difference(PyObject set) {
        return baseset_difference(set);
    }

    @ExposedMethod
    final PyObject set_symmetric_difference(PyObject set) {
        return baseset_symmetric_difference(set);
    }

    @ExposedMethod
    final PyObject set_intersection(PyObject set) {
        return baseset_intersection(set);
    }

    @ExposedMethod
    final PyObject set_issubset(PyObject set) {
        return baseset_issubset(set);
    }

    @ExposedMethod
    final PyObject set_issuperset(PyObject set) {
        return baseset_issuperset(set);
    }

    @ExposedMethod
    final int set___len__() {
        return baseset___len__();
    }

    @ExposedMethod
    final PyObject set___reduce__() {
        return baseset___reduce__();
    }

    public PyObject __ior__(PyObject other) {
        return set___ior__(other);
    }

    final PyObject set___ior__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.addAll(bs._set);
        return this;
    }

    public PyObject __ixor__(PyObject other) {
        return set___ixor__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___ixor__(PyObject other) {
        this._binary_sanity_check(other);
        set_symmetric_difference_update(other);
        return this;
    }

    public PyObject __iand__(PyObject other) {
        return set___iand__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)  
    final PyObject set___iand__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set = ((BaseSet) this.__and__(bs))._set;
        return this;
    }

    public PyObject __isub__(PyObject other) {
        return set___isub__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject set___isub__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.removeAll(bs._set);
        return this;
    }

    public int hashCode() {
        return set___hash__();
    }

    @ExposedMethod
    final int set___hash__() {
        throw Py.TypeError("set objects are unhashable");
    }

    @ExposedMethod
    final void set_add(PyObject o) {
        this._set.add(o);
    }

    @ExposedMethod
    final void set_remove(PyObject o) {
        boolean b = false;
        try {
            b = this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            b = this._set.remove(immutable);
        }
        if (!b) {
            throw new PyException(Py.KeyError, o);
        }
    }

    @ExposedMethod
    final void set_discard(PyObject o) {
        try {
            this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.remove(immutable);
        }
    }

    @ExposedMethod
    final PyObject set_pop() {
        Iterator iterator = this._set.iterator();
        try {
                Object first = iterator.next();
            this._set.remove(first);
            return (PyObject) first;
        } catch (NoSuchElementException e) {
                throw new PyException(Py.KeyError, "pop from an empty set");
        }
    }

    @ExposedMethod
    final void set_clear() {
        this._set.clear();
    }

    @ExposedMethod
    final void set_update(PyObject data) {
        this._update(data);
    }

    @ExposedMethod
    final void set_union_update(PyObject other) {
        this._update(other);
    }

    @ExposedMethod
    final void set_intersection_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__iand__(other);
        } else {
            BaseSet set = (BaseSet) baseset_intersection(other);
            this._set = set._set;
        }
    }

    @ExposedMethod
    final void set_symmetric_difference_update(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet) other : new PySet(other);
        for (Iterator iterator = bs._set.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if (this._set.contains(o)) {
                this._set.remove(o);
            } else {
                this._set.add(o);
            }
        }
    }

    @ExposedMethod
    final void set_difference_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__isub__(other);
            return;
        }
        for (PyObject o : other.asIterable()) {
            if (this.__contains__(o)) {
                this._set.remove(o);
            }
        }
    }

    @ExposedMethod
    final PyObject set__as_immutable() {
        return new PyFrozenSet(this);
    }
}
