// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

class OperatorFunctions extends PyBuiltinFunctionSet
{
    public OperatorFunctions(String name, int index, int argcount) {
        super(name, index, argcount, argcount, false, null);
    }

    public OperatorFunctions(String name, int index, int minargs, int maxargs)
    {
        super(name, index, minargs, maxargs, false, null);
    }

    public PyObject __call__(PyObject arg1) {
        switch (index) {
        case 10: return arg1.__abs__();
        case 11: return arg1.__invert__();
        case 12: return arg1.__neg__();
        case 13: return arg1.__not__();
        case 14: return arg1.__pos__();
        case 15: return Py.newBoolean(arg1.__nonzero__());
        case 16: return Py.newBoolean(arg1.isCallable());
        case 17: return Py.newBoolean(arg1.isMappingType());
        case 18: return Py.newBoolean(arg1.isNumberType());
        case 19: return Py.newBoolean(arg1.isSequenceType());
        case 32: return arg1.__invert__();
        default:
            throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        switch (index) {
        case 0: return arg1._add(arg2);
        case 1: return arg1._and(arg2);
        case 2: return arg1._div(arg2);
        case 3: return arg1._lshift(arg2);
        case 4: return arg1._mod(arg2);
        case 5: return arg1._mul(arg2);
        case 6: return arg1._or(arg2);
        case 7: return arg1._rshift(arg2);
        case 8: return arg1._sub(arg2);
        case 9: return arg1._xor(arg2);
        case 20: return Py.newBoolean(arg1.__contains__(arg2));
        case 21:
            arg1.__delitem__(arg2);
            return Py.None;
        case 23: return arg1.__getitem__(arg2);
        case 27: return arg1._ge(arg2);
        case 28: return arg1._le(arg2);
        case 29: return arg1._eq(arg2);
        case 30: return arg1._floordiv(arg2);
        case 31: return arg1._gt(arg2);
        case 33: return arg1._lt(arg2);
        case 34: return arg1._ne(arg2);
        case 35: return arg1._truediv(arg2);
        case 36: return arg1._pow(arg2);
        default:
            throw argCountError(2);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        switch (index) {
        case 22: arg1.__delslice__(arg2, arg3); return Py.None;
        case 24: return arg1.__getslice__(arg2, arg3);
        case 25: arg1.__setitem__(arg2, arg3); return Py.None;
        default:
            throw argCountError(3);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3,
                             PyObject arg4)
    {
        switch (index) {
        case 26:
            arg1.__setslice__(arg2, arg3, arg4);
            return Py.None;
        default:
            throw argCountError(4);
        }
    }
}

public class operator implements ClassDictInit
{
    public static PyString __doc__ = new PyString(
        "Operator interface.\n"+
        "\n"+
        "This module exports a set of functions implemented in C "+
                "corresponding\n"+
        "to the intrinsic operators of Python.  For example, "+
                "operator.add(x, y)\n"+
        "is equivalent to the expression x+y.  The function names "+
                "are those\n"+
        "used for special class methods; variants without leading "+
                "and trailing\n"+
        "'__' are also provided for convenience.\n"
    );

    public static void classDictInit(PyObject dict) throws PyIgnoreMethodTag {
        dict.__setitem__("__add__", new OperatorFunctions("__add__", 0, 2));
        dict.__setitem__("add", new OperatorFunctions("add", 0, 2));
        dict.__setitem__("__concat__",
                         new OperatorFunctions("__concat__", 0, 2));
        dict.__setitem__("concat", new OperatorFunctions("concat", 0, 2));
        dict.__setitem__("__and__", new OperatorFunctions("__and__", 1, 2));
        dict.__setitem__("and_", new OperatorFunctions("and_", 1, 2));
        dict.__setitem__("__div__", new OperatorFunctions("__div__", 2, 2));
        dict.__setitem__("div", new OperatorFunctions("div", 2, 2));
        dict.__setitem__("__lshift__",
                         new OperatorFunctions("__lshift__", 3, 2));
        dict.__setitem__("lshift", new OperatorFunctions("lshift", 3, 2));
        dict.__setitem__("__mod__", new OperatorFunctions("__mod__", 4, 2));
        dict.__setitem__("mod", new OperatorFunctions("mod", 4, 2));
        dict.__setitem__("__mul__", new OperatorFunctions("__mul__", 5, 2));
        dict.__setitem__("mul", new OperatorFunctions("mul", 5, 2));
        dict.__setitem__("__repeat__",
                         new OperatorFunctions("__repeat__", 5, 2));
        dict.__setitem__("repeat", new OperatorFunctions("repeat", 5, 2));
        dict.__setitem__("__or__", new OperatorFunctions("__or__", 6, 2));
        dict.__setitem__("or_", new OperatorFunctions("or_", 6, 2));
        dict.__setitem__("__rshift__",
                         new OperatorFunctions("__rshift__", 7, 2));
        dict.__setitem__("rshift", new OperatorFunctions("rshift", 7, 2));
        dict.__setitem__("__sub__", new OperatorFunctions("__sub__", 8, 2));
        dict.__setitem__("sub", new OperatorFunctions("sub", 8, 2));
        dict.__setitem__("__xor__", new OperatorFunctions("__xor__", 9, 2));
        dict.__setitem__("xor", new OperatorFunctions("xor", 9, 2));
        dict.__setitem__("__abs__", new OperatorFunctions("__abs__", 10, 1));
        dict.__setitem__("abs", new OperatorFunctions("abs", 10, 1));
        dict.__setitem__("__inv__", new OperatorFunctions("__inv__", 11, 1));
        dict.__setitem__("inv", new OperatorFunctions("inv", 11, 1));
        dict.__setitem__("__neg__", new OperatorFunctions("__neg__", 12, 1));
        dict.__setitem__("neg", new OperatorFunctions("neg", 12, 1));
        dict.__setitem__("__not__", new OperatorFunctions("__not__", 13, 1));
        dict.__setitem__("not_", new OperatorFunctions("not_", 13, 1));
        dict.__setitem__("__pos__", new OperatorFunctions("__pos__", 14, 1));
        dict.__setitem__("pos", new OperatorFunctions("pos", 14, 1));
        dict.__setitem__("truth", new OperatorFunctions("truth", 15, 1));
        dict.__setitem__("isCallable",
                         new OperatorFunctions("isCallable", 16, 1));
        dict.__setitem__("isMappingType",
                         new OperatorFunctions("isMappingType", 17, 1));
        dict.__setitem__("isNumberType",
                         new OperatorFunctions("isNumberType", 18, 1));
        dict.__setitem__("isSequenceType",
                         new OperatorFunctions("isSequenceType", 19, 1));
        dict.__setitem__("contains",
                         new OperatorFunctions("contains", 20, 2));
        dict.__setitem__("__contains__",
                         new OperatorFunctions("__contains__", 20, 2));
        dict.__setitem__("sequenceIncludes",
                         new OperatorFunctions("sequenceIncludes", 20, 2));
        dict.__setitem__("__delitem__",
                         new OperatorFunctions("__delitem__", 21, 2));
        dict.__setitem__("delitem", new OperatorFunctions("delitem", 21, 2));
        dict.__setitem__("__delslice__",
                         new OperatorFunctions("__delslice__", 22, 3));
        dict.__setitem__("delslice",
                         new OperatorFunctions("delslice", 22, 3));
        dict.__setitem__("__getitem__",
                         new OperatorFunctions("__getitem__", 23, 2));
        dict.__setitem__("getitem", new OperatorFunctions("getitem", 23, 2));
        dict.__setitem__("__getslice__",
                         new OperatorFunctions("__getslice__", 24, 3));
        dict.__setitem__("getslice",
                         new OperatorFunctions("getslice", 24, 3));
        dict.__setitem__("__setitem__",
                         new OperatorFunctions("__setitem__", 25, 3));
        dict.__setitem__("setitem", new OperatorFunctions("setitem", 25, 3));
        dict.__setitem__("__setslice__",
                         new OperatorFunctions("__setslice__", 26, 4));
        dict.__setitem__("setslice",
                         new OperatorFunctions("setslice", 26, 4));
        dict.__setitem__("ge", new OperatorFunctions("ge", 27, 2));
        dict.__setitem__("__ge__", new OperatorFunctions("__ge__", 27, 2));
        dict.__setitem__("le", new OperatorFunctions("le", 28, 2));
        dict.__setitem__("__le__", new OperatorFunctions("__le__", 28, 2));
        dict.__setitem__("eq", new OperatorFunctions("eq", 29, 2));
        dict.__setitem__("__eq__", new OperatorFunctions("__eq__", 29, 2));
        dict.__setitem__("floordiv",
                        new OperatorFunctions("floordiv", 30, 2));
        dict.__setitem__("__floordiv__",
                        new OperatorFunctions("__floordiv__", 30, 2));
        dict.__setitem__("gt", new OperatorFunctions("gt", 31, 2));
        dict.__setitem__("__gt__", new OperatorFunctions("__gt__", 31, 2));
        dict.__setitem__("invert", new OperatorFunctions("invert", 32, 1));
        dict.__setitem__("__invert__",
                        new OperatorFunctions("__invert__", 32, 1));
        dict.__setitem__("lt", new OperatorFunctions("lt", 33, 2));
        dict.__setitem__("__lt__", new OperatorFunctions("__lt__", 33, 2));
        dict.__setitem__("ne", new OperatorFunctions("ne", 34, 2));
        dict.__setitem__("__ne__", new OperatorFunctions("__ne__", 34, 2));
        dict.__setitem__("truediv", new OperatorFunctions("truediv", 35, 2));
        dict.__setitem__("__truediv__",
                        new OperatorFunctions("__truediv__", 35, 2));
        dict.__setitem__("pow", new OperatorFunctions("pow", 36, 2));
        dict.__setitem__("__pow__", new OperatorFunctions("pow", 36, 2));
    }

    public static int countOf(PyObject seq, PyObject item) {
        int count = 0;

        PyObject iter = seq.__iter__();
        for (PyObject tmp = null; (tmp = iter.__iternext__()) != null; ) {
            if (item._eq(tmp).__nonzero__())
                count++;
        }
        return count;
    }

    public static int indexOf(PyObject seq, PyObject item) {
        int i = 0;
        PyObject iter = seq.__iter__();
        for (PyObject tmp = null; (tmp = iter.__iternext__()) != null; i++) {
            if (item._eq(tmp).__nonzero__())
                return i;
        }
        throw Py.ValueError("sequence.index(x): x not in list");
    }
}
