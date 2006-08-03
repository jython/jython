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
    public boolean args, keywords;
    PyFunctionTable funcs;
    int func_id;

    final public static int CO_OPTIMIZED      = 0x0001;
    //final public static int CO_NEWLOCALS    = 0x0002
    final public static int CO_VARARGS        = 0x0004;
    final public static int CO_VARKEYWORDS    = 0x0008;
    final public static int CO_GENERATOR      = 0x0020;
    
    final public static int CO_NESTED         = 0x0010;
    final public static int CO_GENERATOR_ALLOWED = 0x1000;
    final public static int CO_FUTUREDIVISION = 0x2000;
    final public static int CO_ALL_FEATURES = CO_NESTED|CO_GENERATOR_ALLOWED|CO_FUTUREDIVISION;


    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean args, boolean keywords,
                       PyFunctionTable funcs, int func_id)
    {
        this(argcount, varnames, filename, name, firstlineno, args,
             keywords, funcs, func_id, null, null, 0, 0);
    }

    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean args, boolean keywords,
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
        this.args = args;
        co_name = name;
        if (args) {
            co_argcount -= 1;
            co_flags |= CO_VARARGS;
        }
        this.keywords = keywords;
        if (keywords) {
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
                frame.f_builtins = ts.systemState.builtins;
            }
        }
        // nested scopes: setup env with closure
        int env_j = 0;
        int ncells = frame.f_ncells;
        int nfreevars = frame.f_nfreevars;
        PyCell[] env = frame.f_env;
        PyTuple freevars = (PyTuple)closure;
        for (int i = 0; i < ncells; i++,env_j++) {
            env[env_j] = new PyCell();
        }
        for (int i=0; i<nfreevars; i++,env_j++) {
            env[env_j] = (PyCell)freevars.pyget(i);
        }

        ts.frame = frame;

        // Handle trace function for debugging
        PySystemState ss = ts.systemState;
        if (ss.tracefunc != null) {
            // JPython and CPython differ here.  CPython actually lays down
            // an extra SET_LINENO bytecode for function definition line.
            // This is ostensibly so that a tuple unpacking failure in
            // argument passing gets the right line number in the
            // traceback.  It also means that when tracing a function,
            // you'll see two 'line' events, one for the def line and then
            // immediately after, one for the first line of the function.
            //
            // JPython on the other hand only lays down a call in the
            // generated Java function to set the line number for the first
            // line of the function (i.e. not the def line).  This
            // difference in behavior doesn't seem to affect arg tuple
            // unpacking tracebacks, but it does mean that function tracing
            // gives slightly different behavior.  Is this bad?  Until
            // someone complains... no.
            //
            // The second commented out line fixes this but it is probably
            // not the right solution.  Better would be to fix the code
            // generator to lay down two calls to setline() in the
            // classfile.  This would allow that call to be optimized out
            // when using the -O option.  I suppose on the other hand we
            // could test that flag here and not call the setline below.
            // In either case, it probably doesn't make sense to slow down
            // function calling even by this miniscule amount until it's
            // shown to have a detrimental effect.
            //
            // Note also that if you were to print out frame.f_lineno in
            // the `call' event handler of your trace function, you'd see
            // zero instead of the line of the def.  That's what the first
            // commented line fixes.
            //
            //  9-Sep-1999 baw
            //
//             frame.f_lineno = co_firstlineno;
            frame.tracefunc = ss.tracefunc.traceCall(frame);
            frame.setline(co_firstlineno);
        }

        // Handle trace function for profiling
        if (ss.profilefunc != null) {
            ss.profilefunc.traceCall(frame);
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
            if (ss.profilefunc != null) {
                ss.profilefunc.traceException(frame, e);
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
        if (ss.profilefunc != null) {
            ss.profilefunc.traceReturn(frame, ret);
        }

        // Restore previously defined exception
        ts.exception = previous_exception;

        ts.frame = ts.frame.f_back;
        return ret;
    }

    public PyObject call(PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 0 || args || keywords)
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
        if (co_argcount != 1 || args || keywords)
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
        if (co_argcount != 2 || args || keywords)
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
        if (co_argcount != 3 || args || keywords)
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

    public PyObject call(PyObject self, PyObject call_args[],
                         String call_keywords[], PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        PyObject[] os = new PyObject[call_args.length+1];
        os[0] = (PyObject)self;
        System.arraycopy(call_args, 0, os, 1, call_args.length);
        return call(os, call_keywords, globals, defaults, closure);
    }

    private String prefix() {
        return co_name.toString()+"() ";
    }

    public PyObject call(PyObject call_args[], String call_keywords[],
                         PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        //Needs try except finally blocks
        PyFrame my_frame = new PyFrame(this, globals);

        PyObject actual_args[], extra_args[] = null;
        PyDictionary extra_keywords = null;
        int plain_args = call_args.length - call_keywords.length;
        int i;

        if (plain_args > co_argcount)
            plain_args = co_argcount;

        actual_args = my_frame.f_fastlocals;
        if (plain_args > 0)
            System.arraycopy(call_args, 0, actual_args, 0, plain_args);

        if (!((call_keywords == null || call_keywords.length == 0) &&
              call_args.length == co_argcount && !keywords && !args))
        {
            if (keywords)
                extra_keywords = new PyDictionary();

            for (i=0; i<call_keywords.length; i++) {
                int index=0;
                while (index<co_argcount) {
                    if (co_varnames[index].equals(call_keywords[i]))
                        break;
                    index++;
                }
                if (index < co_argcount) {
                    if (actual_args[index] != null) {
                        throw Py.TypeError(prefix()+
                                           "got multiple values for " +
                                           "keyword argument '"+
                                           call_keywords[i] + "'");
                    }
                    actual_args[index] =
                        call_args[i+(call_args.length-call_keywords.length)];
                }
                else {
                    if (extra_keywords == null) {
                        throw Py.TypeError(prefix()+
                                           "got an unexpected keyword " +
                                           "argument '"+ call_keywords[i] +
                                           "'");
                    }
                    extra_keywords.__setitem__(
                        call_keywords[i],
                        call_args[i+(call_args.length-call_keywords.length)]);
                }
            }
            if (call_args.length-call_keywords.length > co_argcount) {
                if (!args)
                    throw Py.TypeError(
                        prefix()+
                        "too many arguments; expected "+
                        co_argcount+" got "+
                        (call_args.length-call_keywords.length));
                extra_args = new PyObject[call_args.length-
                                         call_keywords.length-
                                         co_argcount];

                for (i=0; i<extra_args.length; i++) {
                    extra_args[i] = call_args[i+co_argcount];
                }
            }
            for (i=plain_args; i<co_argcount; i++) {
                if (actual_args[i] == null) {
                    if (co_argcount-i > defaults.length) {
                        int min = co_argcount-defaults.length;
                        throw Py.TypeError(
                            prefix()+
                            "takes at least " + min +
                            (min == 1 ? " argument (" : " arguments (") +
                            (call_args.length-call_keywords.length)+
                            " given)");
                    }
                    actual_args[i] =
                          defaults[defaults.length-(co_argcount-i)];
                }
            }
            if (args) {
                if (extra_args == null)
                    actual_args[co_argcount] = Py.EmptyTuple;
                else
                    actual_args[co_argcount] = new PyTuple(extra_args);
            }
            if (extra_keywords != null) {
                actual_args[nargs-1] = extra_keywords;
            }
        }
        if ((co_flags & CO_GENERATOR) != 0) {
            return new PyGenerator(my_frame, closure);
        }
        return call(my_frame, closure);
    }

    public String toString() {
        return "<code object " + co_name + " "+Py.idstr(this) +
            ", file \"" + co_filename + "\", line " +
            co_firstlineno + ">";
    }
}
