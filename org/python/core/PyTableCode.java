// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyTableCode extends PyCode {
    public int co_argcount;
    int nargs;
    public int co_firstlineno = -1;
    public String co_varnames[];
    public String co_filename;
    public boolean args, keywords;
    PyFunctionTable funcs;
    int func_id;

    public static PyClass __class__;
                            
    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean args, boolean keywords,
                       PyFunctionTable funcs, int func_id)
    {
        super(__class__);
        co_argcount = nargs = argcount;
        co_varnames = varnames;
        co_filename = filename;
        co_firstlineno = firstlineno;
        this.args = args;
        co_name = name;
        if (args)
            co_argcount -= 1;
        this.keywords = keywords;
        if (keywords)
            co_argcount -= 1;
        this.funcs = funcs;
        this.func_id = func_id;
    }

    public PyObject call(PyFrame frame) {
        //System.out.println("tablecode call: "+co_name);
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
        ts.frame = frame;

        // Handle trace function for debugging
        PySystemState ss = ts.systemState;
        if (ss.tracefunc != null) {
            frame.tracefunc = ss.tracefunc.traceCall(frame);
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
                PyTraceback tb = new PyTraceback(e.traceback.tb_frame.f_back);
                tb.tb_next = e.traceback;
                e.traceback = tb;
            }
                        
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
    public PyObject call(PyObject globals, PyObject[] defaults) {
        if (co_argcount != 0 || args || keywords)
            return call(Py.EmptyObjects, Py.NoKeywords, globals, defaults);
        PyFrame frame = new PyFrame(this, globals);
        return call(frame);
    }
    
    public PyObject call(PyObject arg1, PyObject globals, PyObject[] defaults)
    {
        if (co_argcount != 1 || args || keywords)
            return call(new PyObject[] {arg1},
                        Py.NoKeywords, globals, defaults);
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        return call(frame);
    }
    
    public PyObject call(PyObject arg1, PyObject arg2, PyObject globals,
                         PyObject[] defaults)
    {
        if (co_argcount != 2 || args || keywords)
            return call(new PyObject[] {arg1, arg2},
                        Py.NoKeywords, globals, defaults);
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        return call(frame);
    }
    
    public PyObject call(PyObject arg1, PyObject arg2, PyObject arg3,
                         PyObject globals, PyObject[] defaults)
    {
        if (co_argcount != 2 || args || keywords)
            return call(new PyObject[] {arg1, arg2, arg3},
                        Py.NoKeywords, globals, defaults);
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        frame.f_fastlocals[2] = arg3;
        return call(frame);
    }   
        
    /*
      public PyObject call(PyObject arg1, PyObject arg2, PyObject globals, PyObject[] defaults) {
      if (co_argcount != 2) return call(new PyObject[] {arg1, arg2}, Py.NoKeywords, globals, defaults);
      PyFrame frame = new PyFrame(this, globals);
      frame.f_fastlocals[0] = arg1;
      frame.f_fastlocals[1] = arg2;
      return call(frame);
      }*/

    public PyObject call(PyObject self, PyObject call_args[],
                         String call_keywords[], PyObject globals,
                         PyObject[] defaults)
    {
        PyObject[] os = new PyObject[call_args.length+1];
        os[0] = (PyObject)self;
        System.arraycopy(call_args, 0, os, 1, call_args.length);
        return call(os, call_keywords, globals, defaults);
    }
        
    private String prefix() {
        return co_name.toString()+"() ";
    }

    public PyObject call(PyObject call_args[], String call_keywords[],
                         PyObject globals, PyObject[] defaults)
    {
        //System.out.println("call: "+nargs+", "+call_args.length+", "+func.func_defaults.length+", "+call_keywords.length);
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
                                           "duplicate keyword argument: "+
                                           call_keywords[i]);
                    }
                    actual_args[index] =
                        call_args[i+(call_args.length-call_keywords.length)];
                }
                else {
                    if (extra_keywords == null) {
                        throw Py.TypeError(prefix()+
                                           "unexpected keyword argument: "+
                                           call_keywords[i]);
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
                        //System.out.println("nea: "+nargs+", "+i+", "+defaults.length+", "+plain_args);
                        throw Py.TypeError(
                            prefix()+
                            "not enough arguments; expected "+
                            (co_argcount-defaults.length)+" got "+
                            (call_args.length-call_keywords.length));
                    }
                    actual_args[i] = defaults[defaults.length-(co_argcount-i)];
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
        return call(my_frame);
    }
}
