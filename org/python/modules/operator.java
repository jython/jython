package org.python.modules;
import org.python.core.*;

class OperatorFunctions extends PyBuiltinFunctionSet {
    public PyObject __call__(PyObject arg1) {
        switch(index) {
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
            default:
                throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        switch(index) {
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
            case 21: arg1.__delitem__(arg2); return Py.None;
            case 23: return arg1.__getitem__(arg2);        
            default:
                throw argCountError(2);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        switch(index) {
            case 22: arg1.__delslice__(arg2, arg3); return Py.None;
            case 24: return arg1.__getslice__(arg2, arg3);
            case 25: arg1.__setitem__(arg2, arg3); return Py.None;        
            default:
                throw argCountError(3);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
        switch(index) {
            case 26: arg1.__setslice__(arg2, arg3, arg4); return Py.None;        
            default:
                throw argCountError(4);
        }
    }  
}

public class operator implements InitModule {
    public void initModule(PyObject dict) {
        dict.__setitem__("__add__", new OperatorFunctions().init("__add__", 0, 2));
        dict.__setitem__("add", new OperatorFunctions().init("add", 0, 2));
        dict.__setitem__("__concat__", new OperatorFunctions().init("__concat__", 0, 2));
        dict.__setitem__("concat", new OperatorFunctions().init("concat", 0, 2));
        dict.__setitem__("__and__", new OperatorFunctions().init("__and__", 1, 2));
        dict.__setitem__("and_", new OperatorFunctions().init("and_", 1, 2));
        dict.__setitem__("__div__", new OperatorFunctions().init("__div__", 2, 2));
        dict.__setitem__("div", new OperatorFunctions().init("div", 2, 2));
        dict.__setitem__("__lshift__", new OperatorFunctions().init("__lshift__", 3, 2));
        dict.__setitem__("lshift", new OperatorFunctions().init("lshift", 3, 2));
        dict.__setitem__("__mod__", new OperatorFunctions().init("__mod__", 4, 2));
        dict.__setitem__("mod", new OperatorFunctions().init("mod", 4, 2));
        dict.__setitem__("__mul__", new OperatorFunctions().init("__mul__", 5, 2));
        dict.__setitem__("mul", new OperatorFunctions().init("mul", 5, 2));
        dict.__setitem__("__repeat__", new OperatorFunctions().init("__repeat__", 5, 2));
        dict.__setitem__("repeat", new OperatorFunctions().init("repeat", 5, 2));
        dict.__setitem__("__or__", new OperatorFunctions().init("__or__", 6, 2));
        dict.__setitem__("or_", new OperatorFunctions().init("or_", 6, 2));
        dict.__setitem__("__rshift__", new OperatorFunctions().init("__rshift__", 7, 2));
        dict.__setitem__("rshift", new OperatorFunctions().init("rshift", 7, 2));
        dict.__setitem__("__sub__", new OperatorFunctions().init("__sub__", 8, 2));
        dict.__setitem__("sub", new OperatorFunctions().init("sub", 8, 2));
        dict.__setitem__("__xor__", new OperatorFunctions().init("__xor__", 9, 2));
        dict.__setitem__("xor", new OperatorFunctions().init("xor", 9, 2));
        dict.__setitem__("__abs__", new OperatorFunctions().init("__abs__", 10, 1));
        dict.__setitem__("abs", new OperatorFunctions().init("abs", 10, 1));
        dict.__setitem__("__inv__", new OperatorFunctions().init("__inv__", 11, 1));
        dict.__setitem__("inv", new OperatorFunctions().init("inv", 11, 1));
        dict.__setitem__("__neg__", new OperatorFunctions().init("__neg__", 12, 1));
        dict.__setitem__("neg", new OperatorFunctions().init("neg", 12, 1));
        dict.__setitem__("__not__", new OperatorFunctions().init("__not__", 13, 1));
        dict.__setitem__("not_", new OperatorFunctions().init("not_", 13, 1));
        dict.__setitem__("__pos__", new OperatorFunctions().init("__pos__", 14, 1));
        dict.__setitem__("pos", new OperatorFunctions().init("pos", 14, 1));
        dict.__setitem__("truth", new OperatorFunctions().init("truth", 15, 1));
        dict.__setitem__("isCallable", new OperatorFunctions().init("isCallable", 16, 1));
        dict.__setitem__("isMappingType", new OperatorFunctions().init("isMappingType", 17, 1));
        dict.__setitem__("isNumberType", new OperatorFunctions().init("isNumberType", 18, 1));
        dict.__setitem__("isSequenceType", new OperatorFunctions().init("isSequenceType", 19, 1));
        dict.__setitem__("contains", new OperatorFunctions().init("contains", 20, 2));
        dict.__setitem__("sequenceIncludes", new OperatorFunctions().init("sequenceIncludes", 20, 2));
        dict.__setitem__("__delitem__", new OperatorFunctions().init("__delitem__", 21, 2));
        dict.__setitem__("delitem", new OperatorFunctions().init("delitem", 21, 2));
        dict.__setitem__("__delslice__", new OperatorFunctions().init("__delslice__", 22, 3));
        dict.__setitem__("delslice", new OperatorFunctions().init("delslice", 22, 3));
        dict.__setitem__("__getitem__", new OperatorFunctions().init("__getitem__", 23, 2));
        dict.__setitem__("getitem", new OperatorFunctions().init("getitem", 23, 2));
        dict.__setitem__("__getslice__", new OperatorFunctions().init("__getslice__", 24, 3));
        dict.__setitem__("getslice", new OperatorFunctions().init("getslice", 24, 3));
        dict.__setitem__("__setitem__", new OperatorFunctions().init("__setitem__", 25, 3));
        dict.__setitem__("setitem", new OperatorFunctions().init("setitem", 25, 3));
        dict.__setitem__("__setslice__", new OperatorFunctions().init("__setslice__", 26, 4));
        dict.__setitem__("setslice", new OperatorFunctions().init("setslice", 26, 4));
    }
    
    public static int countOf(PyObject seq, PyObject item) {
        PyObject tmp;
        int i = 0;
        int count = 0;
        
        while ((tmp = seq.__finditem__(i++)) != null) {
            if (item._eq(tmp).__nonzero__()) count++;
        }
        
        return count;
    }
    
    public static int indexOf(PyObject seq, PyObject item) {
        PyObject tmp;
        int i = 0;
        
        while ((tmp = seq.__finditem__(i++)) != null) {
            if (item._eq(tmp).__nonzero__()) return i;
        }

        return -1;
    }
}