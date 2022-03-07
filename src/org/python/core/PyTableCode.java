// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * An implementation of PyCode where the actual executable content
 * is stored as a PyFunctionTable instance and an integer index.
 */
@Untraversable
public class PyTableCode extends PyBaseCode
{

    PyFunctionTable funcs;
    int func_id;
    public String co_code = ""; // only used by inspect

    public PyTableCode(int argcount, String[] varnames,
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id)
    {
        this(argcount, varnames, filename, name, firstlineno, varargs,
             varkwargs, funcs, func_id, null, null, 0, 0);
    }

    public PyTableCode(int argcount, String[] varnames,
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id,
                       String[] cellvars, String[] freevars, int npurecell,
                       int moreflags) // may change
    {
        co_argcount = nargs = argcount;
        co_varnames = varnames;
        co_nlocals = varnames.length;
        co_filename = filename;
        co_firstlineno = firstlineno;
        co_cellvars = cellvars;
        co_freevars = freevars;
        this.jy_npurecell = npurecell;
        this.varargs = varargs;
        co_name = name;
        if (varargs) {
            co_argcount -= 1;
            co_flags.setFlag(CodeFlag.CO_VARARGS);
        }
        this.varkwargs = varkwargs;
        if (varkwargs) {
            co_argcount -= 1;
            co_flags.setFlag(CodeFlag.CO_VARKEYWORDS);
        }
        co_flags = new CompilerFlags(co_flags.toBits() | moreflags);
        this.funcs = funcs;
        this.func_id = func_id;
    }

    private static final String[] __members__ = {
        "co_name", "co_argcount",
        "co_varnames", "co_filename", "co_firstlineno",
        "co_flags","co_cellvars","co_freevars","co_nlocals"
        // not supported: co_code, co_consts, co_names,
        // co_lnotab, co_stacksize
    };

    @Override
    public PyObject __dir__() {
        PyString[] members = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++) {
            members[i] = new PyString(__members__[i]);
        }
        return new PyList(members);
    }

    private void throwReadonly(String name) {
        for (String s : __members__) {
            if (s.equals(name)) {
                throw Py.TypeError("readonly attribute");
            }
        }
        throw Py.AttributeError(name);
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        // no writable attributes
        throwReadonly(name);
    }

    @Override
    public void __delattr__(String name) {
        throwReadonly(name);
    }

    private static PyTuple toPyStringTuple(String[] ar) {
        if (ar == null) {
            return Py.EmptyTuple;
        }
        int sz = ar.length;
        PyString[] pystr = new PyString[sz];
        for (int i = 0; i < sz; i++) {
            pystr[i] = new PyString(ar[i]);
        }
        return new PyTuple(pystr);
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        if (name == null) {
            return null;
        }
        // have to craft co_varnames specially
        switch (name) {
            case "co_varnames":
                return toPyStringTuple(co_varnames);
            case "co_cellvars":
                return toPyStringTuple(co_cellvars);
            case "co_freevars":
                return toPyStringTuple(co_freevars);
            case "co_filename":
                return Py.fileSystemEncode(co_filename); // bytes object expected by clients
            case "co_name":
                return new PyString(co_name);
            case "co_flags":
                return Py.newInteger(co_flags.toBits());
        }
        return super.__findattr_ex__(name);
    }

    @Override
    public PyObject call(ThreadState ts, PyFrame frame, PyObject closure) {
//         System.err.println("tablecode call: "+co_name);
        if (ts.getSystemState() == null) {
            ts.setSystemState(Py.defaultSystemState);
        }
        //System.err.println("got ts: "+ts+", "+ts.systemState);

        // Cache previously defined exception
        PyException previous_exception = ts.exception;

        // Push frame
        frame.f_back = ts.frame;
        if (frame.f_builtins == null) {
            if (frame.f_back != null) {
                frame.f_builtins = frame.f_back.f_builtins;
            } else {
                //System.err.println("ts: "+ts);
                //System.err.println("ss: "+ts.systemState);
                frame.f_builtins = ts.getSystemState().builtins;
            }
        }
        // nested scopes: setup env with closure
        // this should only be done once, so let the frame take care of it
        frame.setupEnv((PyTuple)closure);

        ts.frame = frame;

        // Handle trace function for debugging
        if (ts.tracefunc != null) {
            frame.f_lineno = co_firstlineno;
            frame.tracefunc = ts.tracefunc.traceCall(frame);
        }

        // Handle trace function for profiling
        if (ts.profilefunc != null) {
            ts.profilefunc.traceCall(frame);
        }

        PyObject ret;
        ThreadStateMapping.enterCall(ts);
        try {
            ret = funcs.call_function(func_id, frame, ts);
        } catch (Throwable t) {
            // Convert exceptions that occurred in Java code to PyExceptions
            if (!(t instanceof Exception)) {
                Py.warning(Py.RuntimeWarning, "PyTableCode.call caught a Throwable that is "
                        + "not an Exception:\n"+t+"\nJython internals might be in a bad state now "
                        + "that can cause deadlocks later on."
                        + "\nSee http://bugs.jython.org/issue2536 for details.");
            }
            PyException pye = Py.JavaError(t);
            pye.tracebackHere(frame);

            frame.f_lasti = -1;

            if (frame.tracefunc != null) {
                frame.tracefunc.traceException(frame, pye);
            }
            if (ts.profilefunc != null) {
                ts.profilefunc.traceException(frame, pye);
            }

            // Rethrow the exception to the next stack frame
            ts.exception = previous_exception;
            ts.frame = ts.frame.f_back;
            throw pye;
        } finally {
            ThreadStateMapping.exitCall(ts);
        }

        if (frame.tracefunc != null) {
            frame.tracefunc.traceReturn(frame, ret);
        }
        // Handle trace function for profiling
        if (ts.profilefunc != null) {
            ts.profilefunc.traceReturn(frame, ret);
        }

        // Restore previously defined exception
        ts.exception = previous_exception;
        ts.frame = ts.frame.f_back;
        return ret;
    }

    @Override
    protected PyObject interpret(PyFrame f, ThreadState ts) {
        throw new UnsupportedOperationException("Inlined interpret to improve call performance (may want to reconsider in the future).");
    }
}
