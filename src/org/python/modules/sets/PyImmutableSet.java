package org.python.modules.sets;

import org.python.core.PyBuiltinFunction;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "ImmutableSet", base = PyObject.class)
public class PyImmutableSet extends BaseSet {
    
    public static final PyType TYPE = PyType.fromClass(PyImmutableSet.class);
    
    public PyImmutableSet() {
        super();
    }

    public PyImmutableSet(PyType type) {
        super(type);
    }

    public PyImmutableSet(PyObject data) {
        super(data);
    }

    @ExposedNew
    @ExposedMethod
    final void ImmutableSet___init__(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, "ImmutableSet", 0, 1);
        }
        if (nargs == 0) {
            return;
        }

        PyObject o = args[0];
        _update(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___ne__(PyObject o) {
        return baseset___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___eq__(PyObject o) {
        return baseset___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___or__(PyObject o) {
        return baseset___or__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___xor__(PyObject o) {
        return baseset___xor__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___sub__(PyObject o) {
        return baseset___sub__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___and__(PyObject o) {
        return baseset___and__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___lt__(PyObject o) {
        return baseset___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___gt__(PyObject o) {
        return baseset___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___ge__(PyObject o) {
        return baseset___ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject ImmutableSet___le__(PyObject o) {
        return baseset___le__(o);
    }

    @ExposedMethod
    final PyObject ImmutableSet___iter__() {
        return baseset___iter__();
    }

    @ExposedMethod
    final boolean ImmutableSet___contains__(PyObject item) {
        return baseset___contains__(item);
    }

    @ExposedMethod
    final PyObject ImmutableSet___deepcopy__(PyObject memo) {
        return baseset___deepcopy__(memo);
    }

    @ExposedMethod
    final boolean ImmutableSet___nonzero__() {
        return baseset___nonzero__();
    }

    @ExposedMethod
    final PyObject ImmutableSet_copy() {
        return baseset_copy();
    }

    @ExposedMethod
    final PyObject ImmutableSet_union(PyObject set) {
        return baseset_union(set);
    }

    @ExposedMethod
    final PyObject ImmutableSet_difference(PyObject set) {
        return baseset_difference(set);
    }

    @ExposedMethod
    final PyObject ImmutableSet_symmetric_difference(PyObject set) {
        return baseset_symmetric_difference(set);
    }

    @ExposedMethod
    final PyObject ImmutableSet_intersection(PyObject set) {
        return baseset_intersection(set);
    }

    @ExposedMethod
    final PyObject ImmutableSet_issubset(PyObject set) {
        return baseset_issubset(set);
    }

    @ExposedMethod
    final PyObject ImmutableSet_issuperset(PyObject set) {
        return baseset_issuperset(set);
    }

    @ExposedMethod
    final int ImmutableSet___len__() {
        return baseset___len__();
    }

    @ExposedMethod
    final PyObject ImmutableSet___reduce__() {
        return baseset___reduce__();
    }

    @ExposedMethod
    final int ImmutableSet___hash__() {
        return hashCode();
    }

    public int hashCode() {
        return this._set.hashCode();
    }

    public PyObject _as_immutable() {
        return this;
    }
}
