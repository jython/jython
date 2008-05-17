package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "frozenset", base = PyObject.class)
public class PyFrozenSet extends BaseSet {
    
    public static final PyType TYPE = PyType.fromClass(PyFrozenSet.class);
    
    public PyFrozenSet() {
        super();
    }

    public PyFrozenSet(PyType type) {
        super(type);
    }

    public PyFrozenSet(PyObject data) {
        super(data);
    }

    @ExposedNew
    @ExposedMethod
    final void frozenset___init__(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, "FrozenSet", 0, 1);
        }
        if (nargs == 0) {
            return;
        }

        PyObject o = args[0];
        _update(o);
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
    final PyObject frozenset___deepcopy__(PyObject memo) {
        return baseset___deepcopy__(memo);
    }

    @ExposedMethod
    final boolean frozenset___nonzero__() {
        return baseset___nonzero__();
    }

    @ExposedMethod
    final PyObject frozenset_copy() {
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
        return hashCode();
    }

    public int hashCode() {
        return this._set.hashCode();
    }

    public PyObject _as_immutable() {
        return this;
    }
}
