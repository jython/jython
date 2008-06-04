/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedType;

/**
 * A weak reference proxy object.
 */
// XXX: not subclassable
@ExposedType(name = "weakproxy")
public class ProxyType extends AbstractReference {

    public static final PyType TYPE = PyType.fromClass(ProxyType.class);

    public ProxyType(PyType subType, GlobalRef ref, PyObject callback) {
        super(subType, ref, callback);
    }

    public ProxyType(GlobalRef ref, PyObject callback) {
        this(TYPE, ref, callback);
    }

    public boolean __nonzero__() { return py().__nonzero__(); }
    public int __len__() { return py().__len__(); }

    public PyObject __finditem__(PyObject key) { return py().__finditem__(key); }
    public void __setitem__(PyObject key, PyObject value) { py().__setitem__(key, value); }
    public void __delitem__(PyObject key) { py().__delitem__(key); }

    public PyObject __getslice__(PyObject start, PyObject stop, PyObject step) {
        return py().__getslice__(start, stop, step);
    }
    public void __setslice__(PyObject start, PyObject stop, PyObject step) {
        py().__setslice__(start, stop, step);
    }
    public void __delslice__(PyObject start, PyObject stop, PyObject step) {
        py().__delslice__(start, stop, step);
    }

    public PyObject __findattr__(String name) { return py().__findattr__(name); }
    public void __setattr__(String name, PyObject value) { py().__setattr__(name, value); }
    public void __delattr__(String name) { py().__delattr__(name); }

    public PyObject __iter__() { return py().__iter__(); }
    public PyString __str__() { return py().__str__(); }
    public PyString __hex__() { return py().__hex__(); }
    public PyString __oct__() { return py().__oct__(); }
    public PyObject __int__() { return py().__int__(); }
    public PyFloat __float__() { return py().__float__(); }
    public PyLong __long__() { return py().__long__(); }
    public PyComplex __complex__() { return py().__complex__(); }
    public PyObject __pos__() { return py().__pos__(); }
    public PyObject __neg__() { return py().__neg__(); }
    public PyObject __abs__() { return py().__abs__(); }
    public PyObject __invert__() { return py().__invert__(); }


    public boolean __contains__(PyObject o) { return py().__contains__(o); }

    public PyObject __add__(PyObject o) { return py().__add__(o); }
    public PyObject __radd__(PyObject o) { return py().__radd__(o); }
    public PyObject __iadd__(PyObject o) { return py().__iadd__(o); }
    public PyObject __sub__(PyObject o) { return py().__sub__(o); }
    public PyObject __rsub__(PyObject o) { return py().__rsub__(o); }
    public PyObject __isub__(PyObject o) { return py().__isub__(o); }
    public PyObject __mul__(PyObject o) { return py().__mul__(o); }
    public PyObject __rmul__(PyObject o) { return py().__rmul__(o); }
    public PyObject __imul__(PyObject o) { return py().__imul__(o); }
    public PyObject __div__(PyObject o) { return py().__div__(o); }
    public PyObject __rdiv__(PyObject o) { return py().__rdiv__(o); }
    public PyObject __idiv__(PyObject o) { return py().__idiv__(o); }
    public PyObject __mod__(PyObject o) { return py().__mod__(o); }
    public PyObject __rmod__(PyObject o) { return py().__rmod__(o); }
    public PyObject __imod__(PyObject o) { return py().__imod__(o); }
    public PyObject __divmod__(PyObject o) { return py().__divmod__(o); }
    public PyObject __rdivmod__(PyObject o) { return py().__rdivmod__(o);}
    public PyObject __pow__(PyObject o) { return py().__pow__(o); }
    public PyObject __rpow__(PyObject o) { return py().__rpow__(o); }
    public PyObject __ipow__(PyObject o) { return py().__ipow__(o); }
    public PyObject __lshift__(PyObject o) { return py().__lshift__(o); }
    public PyObject __rlshift__(PyObject o) { return py().__rlshift__(o);}
    public PyObject __ilshift__(PyObject o) { return py().__ilshift__(o);}

    public PyObject __rshift__(PyObject o) { return py().__rshift__(o); }
    public PyObject __rrshift__(PyObject o) { return py().__rrshift__(o);}
    public PyObject __irshift__(PyObject o) { return py().__irshift__(o);}
    public PyObject __and__(PyObject o) { return py().__and__(o); }
    public PyObject __rand__(PyObject o) { return py().__rand__(o); }
    public PyObject __iand__(PyObject o) { return py().__iand__(o); }
    public PyObject __or__(PyObject o) { return py().__or__(o); }
    public PyObject __ror__(PyObject o) { return py().__ror__(o); }
    public PyObject __ior__(PyObject o) { return py().__ior__(o); }
    public PyObject __xor__(PyObject o) { return py().__xor__(o); }
    public PyObject __rxor__(PyObject o) { return py().__rxor__(o); }
    public PyObject __ixor__(PyObject o) { return py().__ixor__(o); }
}
