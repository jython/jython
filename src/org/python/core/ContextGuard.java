package org.python.core;

/**
 * Straightens the call path for some common cases
 */
public class ContextGuard implements ContextManager {

    private final PyObject __enter__method;
    private final PyObject __exit__method;

    private ContextGuard(PyObject manager) {
        __exit__method = manager.__getattr__("__exit__");
        __enter__method = manager.__getattr__("__enter__");
    }

    public PyObject __enter__(ThreadState ts) {
        return __enter__method.__call__(ts);
    }

    public boolean __exit__(ThreadState ts, PyObject type, PyObject value,
            PyObject traceback) {
        return __exit__method.__call__(ts, type, value, traceback).__nonzero__();
    }

    public static ContextManager getManager(PyObject manager) {
        if (manager instanceof ContextManager) {
            return (ContextManager) manager;
        } else {
            return new ContextGuard(manager);
        }
    }

    // XXX - tentative support for generators in conjunction w/ contextlib.contextmanager
    
    /* Sample usage:
     * 
     * from org.python.core.ContextGuard import makeManager as contextmanager
     * @contextmanager
     * def my_manager():
     *     print "setup"
     *     try:
     *         yield
     *     finally:
     *         print "done"
     */
    
    public static PyObject makeManager(PyObject object) {
        if (object instanceof PyFunction) {
            PyFunction function = (PyFunction) object;
            PyCode code = function.func_code;
            if (code instanceof PyBaseCode) {
                PyBaseCode pyCode = (PyBaseCode) code;
                if (pyCode.co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
                    return new PyFunction(function.func_globals,
                            function.func_defaults,
                            new ContextCode(pyCode),
                            function.__doc__,
                            (function.func_closure == null) ? null : 
                            ((PyTuple)function.func_closure).getArray());
                }
            }
        }
        throw Py.TypeError("Argument must be a generator function.");
    }

    private static class ContextCode extends PyBaseCode {
        private final PyBaseCode code;
        ContextCode(PyBaseCode code) {
            this.co_name = code.co_name;
            this.code = code;
            this.co_argcount = code.co_argcount;
            this.nargs = code.nargs;
            this.co_firstlineno = code.co_firstlineno;
            this.co_varnames = code.co_varnames;
            this.co_cellvars = code.co_cellvars;
            this.jy_npurecell = code.jy_npurecell;
            this.co_freevars = code.co_freevars;
            this.co_filename = code.co_filename;
            this.co_nlocals = code.co_nlocals;
            this.varargs = code.varargs;
            this.varkwargs = code.varkwargs;
            for (CodeFlag flag : CodeFlag.values()) {
                if (code.co_flags.isFlagSet(flag) && flag != CodeFlag.CO_GENERATOR) {
                    this.co_flags.setFlag(flag);
                }
            }
        }
        @SuppressWarnings("serial")
        @Override
        protected PyObject interpret(PyFrame frame, ThreadState ts) {
            frame.f_back = null;
            return new GeneratorContextManager(frame) {
                @Override
                PyObject body(ThreadState ts) {
                    return code.interpret(frame, ts);
                }
            };
        }
        @SuppressWarnings("serial")
        @Override
        public PyObject call(ThreadState ts, PyFrame frame, final PyObject closure) {
            frame.f_back = null;
            return new GeneratorContextManager(frame) {
                @Override
                PyObject body(ThreadState ts) {
                    return code.call(ts, frame, closure);
                }
            };
        }
    }
    
    @SuppressWarnings("serial")
    private static abstract class GeneratorContextManager extends PyObject implements ContextManager {
        final PyFrame frame;

        public GeneratorContextManager(PyFrame frame) {
            this.frame = frame;
        }

        public PyObject __enter__(ThreadState ts) {
            PyObject res = body(ts);
            if (frame.f_lasti == -1) {
                throw Py.RuntimeError("generator didn't yield");
            }
            return res;
        }
        
        public boolean __exit__(ThreadState ts, PyObject type, PyObject value,
                PyObject traceback) {
            PyException ex = null;
            if (type != null && type != Py.None) {
                ex = Py.makeException(type, value, traceback);
                frame.setGeneratorInput(ex);
            }
            PyObject res = null;
            try {
                res = body(ts);
            } catch(PyException e) {
                if (e == ex) {
                    return false;
                }
            }
            if (frame.f_lasti != -1) {
                throw Py.RuntimeError("generator didn't stop");
            }
            return res.__nonzero__();
        }

        abstract PyObject body(ThreadState ts);
    }
}
