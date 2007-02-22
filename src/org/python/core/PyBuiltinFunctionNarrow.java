package org.python.core;

public abstract class PyBuiltinFunctionNarrow extends PyBuiltinFunction {

    public static final Class exposed_as = PyBuiltinFunction.class;
    
    public PyBuiltinFunctionNarrow(Info info) {
        super(info);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (keywords.length != 0) {
            throw info.unexpectedCall(args.length, true);
        }
        return __call__(args);

    }

    public PyObject __call__(PyObject[] args) {
        switch (args.length) {
            case 0 :
                return __call__();
            case 1 :
                return __call__(args[0]);
            case 2 :
                return __call__(args[0], args[1]);
            case 3 :
                return __call__(args[0], args[1], args[2]);
            case 4 :
                return __call__(args[0], args[1], args[2], args[3]);
            default :
                return wide_call(getSelf(), args);
        }
    }

    public PyObject inst_call(
        PyObject self,
        PyObject[] args,
        String[] keywords) {
        if (keywords.length != 0) {
            throw info.unexpectedCall(args.length, true);
        }
        return inst_call(self, args);
    }

    public PyObject inst_call(PyObject self, PyObject[] args) {
        switch (args.length) {
            case 0 :
                return inst_call(self);
            case 1 :
                return inst_call(self, args[0]);
            case 2 :
                return inst_call(self, args[0], args[1]);
            case 3 :
                return inst_call(self, args[0], args[1], args[2]);
            case 4 :
                return inst_call(self, args[0], args[1], args[2], args[3]);
            default :
                return wide_call(self, args);
        }

    }

    /* hooks */

    public PyObject __call__() {
        return inst_call(getSelf());
    }

    public PyObject __call__(PyObject arg0) {
        return inst_call(getSelf(), arg0);
    }

    public PyObject __call__(PyObject arg0, PyObject arg1) {
        return inst_call(getSelf(), arg0, arg1);
    }

    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        return inst_call(getSelf(), arg0, arg1, arg2);
    }

    public PyObject __call__(
        PyObject arg0,
        PyObject arg1,
        PyObject arg2,
        PyObject arg3) {
        return inst_call(getSelf(), arg0, arg1, arg2, arg3);
    }

    protected PyObject wide_call(PyObject self, PyObject[] wide_args) {
        throw info.unexpectedCall(wide_args.length, false);
    }

    public PyObject inst_call(PyObject self) {
        throw info.unexpectedCall(0, false);
    }

    public PyObject inst_call(PyObject self, PyObject arg0) {
        throw info.unexpectedCall(1, false);
    }

    public PyObject inst_call(PyObject self, PyObject arg0, PyObject arg1) {
        throw info.unexpectedCall(2, false);
    }

    public PyObject inst_call(
        PyObject self,
        PyObject arg0,
        PyObject arg1,
        PyObject arg2) {
        throw info.unexpectedCall(3, false);
    }

    public PyObject inst_call(
        PyObject self,
        PyObject arg0,
        PyObject arg1,
        PyObject arg2,
        PyObject arg3) {
        throw info.unexpectedCall(4, false);
    }
    
}
