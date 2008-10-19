package org.python.core;

public class PyBuiltinFunctionNarrow extends PyBuiltinFunction {

    protected PyBuiltinFunctionNarrow(String name, int minargs, int maxargs, String doc) {
        super(name, minargs, maxargs, doc);
    }

    public PyObject fancyCall(PyObject[] args) {
        throw info.unexpectedCall(args.length, false);
    }

    public PyObject __call__(PyObject[] args) {
        int nargs = args.length;
        switch(nargs){
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
                return fancyCall(args);
        }
    }

    public PyObject __call__(PyObject[] args, String[] kws) {
        if (kws.length != 0) {
            throw Py.TypeError(fastGetName() + "() takes no keyword arguments");
        }
        return __call__(args);
    }


    public PyObject __call__() {
        throw info.unexpectedCall(0, false);
    }

    public PyObject __call__(PyObject arg1) {
        throw info.unexpectedCall(1, false);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        throw info.unexpectedCall(2, false);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        throw info.unexpectedCall(3, false);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
        throw info.unexpectedCall(4, false);
    }
}
