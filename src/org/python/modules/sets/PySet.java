package org.python.modules.sets;

import java.util.Iterator;

import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "Set", base = PyObject.class)
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
    final void Set___init__(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, "Set", 0, 1);
        }
        if (nargs == 0) {
            return;
        }

        PyObject o = args[0];
        _update(o);
    }
    
    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___cmp__(PyObject o) {
        return new PyInteger(baseset___cmp__(o));
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___ne__(PyObject o) {
        return baseset___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___eq__(PyObject o) {
        return baseset___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___or__(PyObject o) {
        return baseset___or__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___xor__(PyObject o) {
        return baseset___xor__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___sub__(PyObject o) {
        return baseset___sub__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___and__(PyObject o) {
        return baseset___and__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___lt__(PyObject o) {
        return baseset___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___gt__(PyObject o) {
        return baseset___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___ge__(PyObject o) {
        return baseset___ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___le__(PyObject o) {
        return baseset___le__(o);
    }

    @ExposedMethod
    final PyObject Set___iter__() {
        return baseset___iter__();
    }

    @ExposedMethod
    final boolean Set___contains__(PyObject item) {
        return baseset___contains__(item);
    }

    @ExposedMethod
    final PyObject Set___deepcopy__(PyObject memo) {
        return baseset___deepcopy__(memo);
    }

    @ExposedMethod
    final boolean Set___nonzero__() {
        return baseset___nonzero__();
    }

    @ExposedMethod
    final PyObject Set_copy() {
        return baseset_copy();
    }

    @ExposedMethod
    final PyObject Set_union(PyObject set) {
        return baseset_union(set);
    }

    @ExposedMethod
    final PyObject Set_difference(PyObject set) {
        return baseset_difference(set);
    }

    @ExposedMethod
    final PyObject Set_symmetric_difference(PyObject set) {
        return baseset_symmetric_difference(set);
    }

    @ExposedMethod
    final PyObject Set_intersection(PyObject set) {
        return baseset_intersection(set);
    }

    @ExposedMethod
    final PyObject Set_issubset(PyObject set) {
        return baseset_issubset(set);
    }

    @ExposedMethod
    final PyObject Set_issuperset(PyObject set) {
        return baseset_issuperset(set);
    }

    @ExposedMethod
    final int Set___len__() {
        return baseset___len__();
    }

    @ExposedMethod
    final PyObject Set___reduce__() {
        return baseset___reduce__();
    }

    public PyObject __ior__(PyObject other) {
        return Set___ior__(other);
    }

    final PyObject Set___ior__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.addAll(bs._set);
        return this;
    }

    public PyObject __ixor__(PyObject other) {
        return Set___ixor__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___ixor__(PyObject other) {
        this._binary_sanity_check(other);
        Set_symmetric_difference_update(other);
        return this;
    }

    public PyObject __iand__(PyObject other) {
        return Set___iand__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)  
    final PyObject Set___iand__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set = ((BaseSet) this.__and__(bs))._set;
        return this;
    }

    public PyObject __isub__(PyObject other) {
        return Set___isub__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject Set___isub__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.removeAll(bs._set);
        return this;
    }

    public int hashCode() {
        return Set___hash__();
    }

    @ExposedMethod
    final int Set___hash__() {
        throw Py.TypeError("Can't hash a Set, only an ImmutableSet.");
    }

    @ExposedMethod
    final void Set_add(PyObject o) {
        try {
            this._set.add(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.add(immutable);
        }
    }

    @ExposedMethod
    final void Set_remove(PyObject o) {
        boolean b = false;
        try {
            b = this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            b = this._set.remove(immutable);
        }
        if (!b) {
            throw new PyException(Py.LookupError, o.toString());
        }
    }

    @ExposedMethod
    final void Set_discard(PyObject o) {
        try {
            this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.remove(immutable);
        }
    }

    @ExposedMethod
    final PyObject Set_pop() {
        Iterator iterator = this._set.iterator();
        Object first = iterator.next();
        this._set.remove(first);
        return (PyObject) first;
    }

    @ExposedMethod
    final void Set_clear() {
        this._set.clear();
    }

    @ExposedMethod
    final void Set_update(PyObject data) {
        this._update(data);
    }

    @ExposedMethod
    final void Set_union_update(PyObject other) {
        this._update(other);
    }

    @ExposedMethod
    final void Set_intersection_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__iand__(other);
        } else {
            BaseSet set = (BaseSet) baseset_intersection(other);
            this._set = set._set;
        }
    }

    @ExposedMethod
    final void Set_symmetric_difference_update(PyObject other) {
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
    final void Set_difference_update(PyObject other) {
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
    final PyObject Set__as_immutable() {
        return new PyImmutableSet(this);
    }
}
