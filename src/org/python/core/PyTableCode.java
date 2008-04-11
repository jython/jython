// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * An implementation of PyCode where the actual executable content
 * is stored as a PyFunctionTable instance and an integer index.
 */

public class PyTableCode extends PyCode
{
    public int co_argcount;
    int nargs;
    public int co_firstlineno = -1;
    public String co_varnames[];
    public String co_cellvars[];
    public int jy_npurecell; // internal: jython specific
    public String co_freevars[];
    public String co_filename;
    public int co_flags;
    public int co_nlocals;
    public boolean varargs, varkwargs;
    PyFunctionTable funcs;
    int func_id;
    public String co_code = ""; // only used by inspect

    final public static int CO_OPTIMIZED         = 0x0001;
    //final public static int CO_NEWLOCALS       = 0x0002
    final public static int CO_VARARGS           = 0x0004;
    final public static int CO_VARKEYWORDS       = 0x0008;
    final public static int CO_GENERATOR         = 0x0020;
    
    final public static int CO_NESTED            = 0x0010;
    final public static int CO_GENERATOR_ALLOWED = 0x1000;
    final public static int CO_FUTUREDIVISION    = 0x2000;

    //XXX: I'm not positive that this is the right place for this constant.
    final public static int PyCF_ONLY_AST        = 0x0400;

    final public static int CO_ALL_FEATURES = PyCF_ONLY_AST|CO_NESTED|CO_GENERATOR_ALLOWED|
                                              CO_FUTUREDIVISION;

    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id)
    {
        this(argcount, varnames, filename, name, firstlineno, varargs,
             varkwargs, funcs, func_id, null, null, 0, 0);
    }

    public PyTableCode(int argcount, String varnames[],
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
            co_flags |= CO_VARARGS;
        }
        this.varkwargs = varkwargs;
        if (varkwargs) {
            co_argcount -= 1;
            co_flags |= CO_VARKEYWORDS;
        }
        co_flags |= moreflags;
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

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        return new PyList(members);
    }

    public boolean hasFreevars() {
        return co_freevars != null && co_freevars.length > 0;
    }

    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++)
            if (__members__[i] == name)
                throw Py.TypeError("readonly attribute");
        throw Py.AttributeError(name);
    }

    public void __setattr__(String name, PyObject value) {
        // no writable attributes
        throwReadonly(name);
    }

    public void __delattr__(String name) {
        throwReadonly(name);
    }

    private static PyTuple toPyStringTuple(String[] ar) {
        if (ar == null) return Py.EmptyTuple;
        int sz = ar.length;
        PyString[] pystr = new PyString[sz];
        for (int i = 0; i < sz; i++) {
            pystr[i] = new PyString(ar[i]);
        }
        return new PyTuple(pystr);
    }

    public PyObject __findattr__(String name) {
        // have to craft co_varnames specially
        if (name == "co_varnames") return toPyStringTuple(co_varnames);
        if (name == "co_cellvars") return toPyStringTuple(co_cellvars);
        if (name == "co_freevars") return toPyStringTuple(co_freevars);
        return super.__findattr__(name);
    }

    public PyObject call(PyFrame frame, PyObject closure) {
//         System.err.println("tablecode call: "+co_name);
        ThreadState ts = Py.getThreadState();
        if (ts.systemState == null) {
            ts.systemState = Py.defaultSystemState;
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
                frame.f_builtins = PySystemState.builtins;
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
        try {
            ret = funcs.call_function(func_id, frame);
        } catch (Throwable t) {
            //t.printStackTrace();
            //Convert exceptions that occured in Java code to PyExceptions
            PyException e = Py.JavaError(t);

            //Add another traceback object to the exception if needed
            if (e.traceback.tb_frame != frame) {
                PyTraceback tb;
                // If f_back is null, we've jumped threads so use the current
                // threadstate's frame. Bug #1533624
                if(e.traceback.tb_frame.f_back == null) {
                    tb = new PyTraceback(ts.frame);
                } else {
                    tb = new PyTraceback(e.traceback.tb_frame.f_back);
                }
                tb.tb_next = e.traceback;
                e.traceback = tb;
            }

            frame.f_lasti = -1;

            if (frame.tracefunc != null) {
                frame.tracefunc.traceException(frame, e);
            }
            if (ts.profilefunc != null) {
                ts.profilefunc.traceException(frame, e);
            }

            //Rethrow the exception to the next stack frame
            ts.exception = previous_exception;
            ts.frame = ts.frame.f_back;
            throw e;
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

    public PyObject call(PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 0 || varargs || varkwargs)
            return call(Py.EmptyObjects, Py.NoKeywords, globals, defaults,
                        closure);
        PyFrame frame = new PyFrame(this, globals);
        if ((co_flags & CO_GENERATOR) != 0) {
            return new PyGenerator(frame, closure);
        }
        return call(frame, closure);
    }

    public PyObject call(PyObject arg1, PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 1 || varargs || varkwargs)
            return call(new PyObject[] {arg1},
                        Py.NoKeywords, globals, defaults, closure);
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        if ((co_flags & CO_GENERATOR) != 0) {
            return new PyGenerator(frame, closure);
        }
        return call(frame, closure);
    }

    public PyObject call(PyObject arg1, PyObject arg2, PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        if (co_argcount != 2 || varargs || varkwargs)
            return call(new PyObject[] {arg1, arg2},
                        Py.NoKeywords, globals, defaults, closure);
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        if ((co_flags & CO_GENERATOR) != 0) {
            return new PyGenerator(frame, closure);
        }
        return call(frame, closure);
    }

    public PyObject call(PyObject arg1, PyObject arg2, PyObject arg3,
                         PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 3 || varargs || varkwargs)
            return call(new PyObject[] {arg1, arg2, arg3},
                        Py.NoKeywords, globals, defaults, closure);
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        frame.f_fastlocals[2] = arg3;
        if ((co_flags & CO_GENERATOR) != 0) {
            return new PyGenerator(frame, closure);
        }
        return call(frame, closure);
    }

    public PyObject call(PyObject self, PyObject args[],
                         String keywords[], PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        PyObject[] os = new PyObject[args.length+1];
        os[0] = self;
        System.arraycopy(args, 0, os, 1, args.length);
        return call(os, keywords, globals, defaults, closure);
    }

    public PyObject call(PyObject args[], String kws[], PyObject globals, PyObject[] defs,
                          PyObject closure) {
        PyFrame frame = new PyFrame(this, globals);
        int argcount = args.length - kws.length;
        PyObject[] fastlocals = frame.f_fastlocals;
        
        if (co_argcount > 0 || (varargs || varkwargs)) {
            int i;
            int n = argcount;
            PyObject kwdict = null;
            if (varkwargs) {
                kwdict = new PyDictionary();
                i = co_argcount;
                if (varargs) {
                    i++;
                }
                fastlocals[i] = kwdict;
            }
            if (argcount > co_argcount) {
                if (!varargs) {
                    String msg = String.format("%.200s() takes %s %d %sargument%s (%d given)",
                                               co_name,
                                               defs.length > 0 ? "at most" : "exactly",
                                               co_argcount,
                                               kws.length > 0 ? "non-keyword " : "",
                                               co_argcount == 1 ? "" : "s",
                                               argcount);
                    throw Py.TypeError(msg);
                }
                n = co_argcount;
            }

            System.arraycopy(args, 0, fastlocals, 0, n);

            if (varargs) {
                PyObject[] u = new PyObject[argcount - n];
                System.arraycopy(args, n, u, 0, argcount - n);
                PyObject uTuple = new PyTuple(u);
                fastlocals[co_argcount] = uTuple;
            }
            for (i = 0; i < kws.length; i++) {
                String keyword = kws[i];
                PyObject value = args[i + argcount];
                int j;
                // XXX: keywords aren't PyObjects, can't ensure strings
                //if (keyword == null || keyword.getClass() != PyString.class) {
                //    throw Py.TypeError(String.format("%.200s() keywords must be strings",
                //                                     co_name));
                //}
                for (j = 0; j < co_argcount; j++) {
                    if (co_varnames[j].equals(keyword)) {
                        break;
                    }
                }
                if (j >= co_argcount) {
                    if (kwdict == null) {
                        throw Py.TypeError(String.format("%.200s() got an unexpected keyword "
                                                         + "argument '%.400s'",
                                                         co_name, keyword));
                    }
                    kwdict.__setitem__(keyword, value);
                } else {
                    if (fastlocals[j] != null) {
                        throw Py.TypeError(String.format("%.200s() got multiple values for "
                                                         + "keyword argument '%.400s'",
                                                         co_name, keyword));
                    }
                    fastlocals[j] = value;
                }
            }
            if (argcount < co_argcount) {
                int m = co_argcount - defs.length;
                for (i = argcount; i < m; i++) {
                    if (fastlocals[i] == null) {
                        String msg =
                                String.format("%.200s() takes %s %d %sargument%s (%d given)",
                                              co_name, (varargs || defs.length > 0) ?
                                              "at least" : "exactly",
                                              m, kws.length > 0 ? "non-keyword " : "",
                                              m == 1 ? "" : "s", i);
                        throw Py.TypeError(msg);
                    }
                }
                if (n > m) {
                    i = n - m;
                } else {
                    i = 0;
                }
                for (; i < defs.length; i++) {
                    if (fastlocals[m + i] == null) {
                        fastlocals[m + i] = defs[i];
                    }
                }
            }
        } else if (argcount > 0) {
            throw Py.TypeError(String.format("%.200s() takes no arguments (%d given)",
                                             co_name, argcount));
        }

        if ((co_flags & CO_GENERATOR) != 0) {
            return new PyGenerator(frame, closure);
        }
        return call(frame, closure);
    }

    public String toString() {
        return String.format("<code object %.100s at %s, file \"%.300s\", line %d>",
                             co_name, Py.idstr(this), co_filename, co_firstlineno);
    }
}
