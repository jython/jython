package org.python.core;

public abstract class PyBuiltinMethodNarrow extends PyBuiltinMethod {

    public PyBuiltinMethodNarrow(PyObject self, Info info) {
        super(self, info);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if(keywords.length != 0) {
            throw info.unexpectedCall(args.length, true);
        }
        return __call__(args);
    }

    public PyObject __call__(PyObject[] args) {
        switch(args.length){
            case 0:
                return __call__();
            case 1:
                return __call__(args[0]);
            case 2:
                return __call__(args[0], args[1]);
            case 3:
                return __call__(args[0], args[1], args[2]);
            case 4:
                return __call__(args[0], args[1], args[2], args[3]);
            default:
                throw info.unexpectedCall(args.length, false);
        }
    }

    public PyObject __call__() {
        throw info.unexpectedCall(0, false);
    }

    public PyObject __call__(PyObject arg0) {
        throw info.unexpectedCall(1, false);
    }

    public PyObject __call__(PyObject arg0, PyObject arg1) {
        throw info.unexpectedCall(2, false);
    }

    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        throw info.unexpectedCall(3, false);
    }

    public PyObject __call__(PyObject arg0,
                             PyObject arg1,
                             PyObject arg2,
                             PyObject arg3) {
        throw info.unexpectedCall(4, false);
    }
}
