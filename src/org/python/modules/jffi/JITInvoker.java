package org.python.modules.jffi;

import com.kenai.jffi.*;
import org.python.core.Py;
import org.python.core.PyObject;

/**
 *
 */
abstract public class JITInvoker extends Invoker {
    protected static final com.kenai.jffi.Invoker jffiInvoker = com.kenai.jffi.Invoker.getInstance();
    private final int arity;

    protected JITInvoker(int arity) {
        this.arity = arity;
    }

    protected final PyObject invalidArity(int got) {
        checkArity(arity, got);
        return Py.None;
    }

    protected final void checkArity(PyObject[] args) {
        checkArity(arity, args.length);
    }

    public static void checkArity(int arity, int got) {
        if (got != arity) {
            throw Py.TypeError(String.format("__call__() takes exactly %d arguments (%d given)", arity, got));
        }
    }


    public PyObject invoke(PyObject[] args) {
        checkArity(args);
        switch (arity) {
            case 0:
                return invoke();

            case 1:
                return invoke(args[0]);

            case 2:
                return invoke(args[0], args[1]);

            case 3:
                return invoke(args[0], args[1], args[2]);

            case 4:
                return invoke(args[0], args[1], args[2], args[3]);

            case 5:
                return invoke(args[0], args[1], args[2], args[3], args[4]);

            case 6:
                return invoke(args[0], args[1], args[2], args[3], args[4], args[5]);

            default:
                throw Py.RuntimeError("invalid fast-int arity");
        }
    }
}
