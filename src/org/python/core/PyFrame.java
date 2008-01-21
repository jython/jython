// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A python frame object.
 */

public class PyFrame extends PyObject
{
    public PyFrame f_back;
    public PyTableCode f_code;
    public PyObject f_locals;
    public PyObject f_globals;
    public int f_lineno;
    public PyObject f_builtins;
    public PyObject[] f_fastlocals;
    public PyCell[] f_env; // nested scopes: cell + free env
    public int f_ncells;
    public int f_nfreevars;
    public int f_lasti;
    public Object[] f_savedlocals;
    public PyObject[] f_stackstate; // newcompiler uses this to allow yield in loops
    public int[] f_blockstate; // newcompiler uses this to allow yield in finally

    // an interface to functions suitable for tracing, e.g. via sys.settrace()
    public TraceFunction tracefunc;

    private static final String[] __members__ = {
        "f_back", "f_code", "f_locals", "f_globals", "f_lineno",
        "f_builtins", "f_trace"
    };

    public PyFrame(PyTableCode code, PyObject locals, PyObject globals,
                   PyObject builtins)
    {
        f_code = code;
        f_locals = locals;
        f_globals = globals;
        f_builtins = builtins;
        // This needs work to be efficient with multiple interpreter states
        if (locals == null && code != null) {
            // ! f_fastlocals needed for arg passing too
            if ((code.co_flags & PyTableCode.CO_OPTIMIZED) != 0
                    || code.nargs > 0) {
                if (code.co_nlocals > 0) {
                    // internal: may change
                    f_fastlocals = new PyObject[code.co_nlocals - code.jy_npurecell];
                }
            } else
                f_locals = new PyStringMap();
        }
        if (code != null) { // reserve space for env
            int env_sz = 0;
            if (code.co_freevars != null)
                env_sz += (f_nfreevars = code.co_freevars.length);
            if (code.co_cellvars != null)
                env_sz += (f_ncells = code.co_cellvars.length);
            if (env_sz > 0)
                f_env = new PyCell[env_sz];
        }
    }

    public PyFrame(PyTableCode code, PyObject globals) {
        this(code, null, globals, null);
    }

    public String toString() {
        if (f_code == null) {
            return "<frame (unknown code) at line " + f_lineno + ">";
        } else {
            return "<frame in \"" + f_code.co_name + "\" at line " + f_lineno + ">";
        }
    }

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        return new PyList(members);
    }

    private Object generatorInput = Py.None;

    void setGeneratorInput(Object value) {
        generatorInput = value;
    }

    public Object getGeneratorInput() {
        Object input = generatorInput;
        generatorInput = Py.None;
        return input;
    }

    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++)
            if (__members__[i] == name)
                throw Py.TypeError("readonly attribute");
        throw Py.AttributeError(name);
    }

    public void __setattr__(String name, PyObject value) {
        // In CPython, some of the frame's attributes are read/writeable
        if (name == "f_trace")
            tracefunc = new PythonTraceFunction(value);
        // not yet implemented:
        // f_exc_type
        // f_exc_value
        // f_exc_traceback
        else throwReadonly(name);
    }

    public void __delattr__(String name) {
        if (name == "f_trace")
            tracefunc = null;
        // not yet implemented:
        // f_exc_type
        // f_exc_value
        // f_exc_traceback
        else throwReadonly(name);
    }

    public PyObject __findattr__(String name) {
        if (name == "f_locals")
            return getf_locals();
        else if (name == "f_trace") {
            if (tracefunc instanceof PythonTraceFunction) {
                return ((PythonTraceFunction)tracefunc).tracefunc;
            }
            return Py.None;
        }
        return super.__findattr__(name);
    }

    public PyObject getf_locals() {
        if (f_locals == null)
            f_locals = new PyStringMap();
        if (f_code != null && (f_code.co_nlocals > 0 || f_nfreevars > 0)) {
            int i;
            if (f_fastlocals != null) {
                for (i = 0; i < f_fastlocals.length; i++) {
                    PyObject o = f_fastlocals[i];
                    if (o != null) f_locals.__setitem__(f_code.co_varnames[i], o);
                }
                if ((f_code.co_flags & PyTableCode.CO_OPTIMIZED) == 0)
                    f_fastlocals = null;
            }
            int j = 0;
            for (i = 0; i < f_ncells; i++, j++) {
                PyObject v = f_env[j].ob_ref;
                if (v != null) f_locals.__setitem__(f_code.co_cellvars[i], v);
            }
            for (i = 0; i < f_nfreevars; i++, j++) {
                PyObject v = f_env[j].ob_ref;
                if (v != null) f_locals.__setitem__(f_code.co_freevars[i], v);
            }
        }
        return f_locals;
    }

    public void setline(int line) {
        f_lineno = line;
        if (tracefunc != null)
            tracefunc = tracefunc.traceLine(this, line);
    }

    public int getline() {
        return f_lineno;
    }

    public PyObject getlocal(int index) {
        if (f_fastlocals != null) {
            PyObject ret = f_fastlocals[index];
            if (ret != null)
                return ret;
        }
        return getlocal(f_code.co_varnames[index]);
    }

    public PyObject getlocal(String index) {
        // System.err.println("getlocal: "+index);
        if (f_locals == null)
            getf_locals();
        PyObject ret = f_locals.__finditem__(index);
        if (ret != null)
            return ret;

        throw Py.UnboundLocalError("local: '" + index + "'");
        //return getglobal(index);
    }

    public PyObject getname(String index) {
        if (f_locals == null) getf_locals();
        if (f_locals == f_globals) return getglobal(index);

        PyObject ret = f_locals.__finditem__(index);
        if (ret != null) return ret;
        return getglobal(index);
    }

    public PyObject getglobal(String index) {
        PyObject ret = f_globals.__finditem__(index);
        if (ret != null) {
            return ret;
        }

        // Set up f_builtins if not already set
        if (f_builtins == null) {
            f_builtins = PySystemState.builtins;
        }
        ret = f_builtins.__finditem__(index);
        if (ret != null) return ret;

        throw Py.NameError(index);
    }

    public void setlocal(int index, PyObject value) {
        if (f_fastlocals != null)
            f_fastlocals[index] = value;
        else
            setlocal(f_code.co_varnames[index], value);
    }

    public void setlocal(String index, PyObject value) {
        if (f_locals == null)
            getf_locals();
        f_locals.__setitem__(index, value);
    }

    public void setglobal(String index, PyObject value) {
        f_globals.__setitem__(index, value);
    }

    public void dellocal(int index) {
        if (f_fastlocals != null) {
            if (f_fastlocals[index] == null) {
                throw Py.UnboundLocalError("local: '" + f_code.co_varnames[index] + "'");
            }
            f_fastlocals[index] = null;
        } else
            dellocal(f_code.co_varnames[index]);
    }

    public void dellocal(String index) {
        if (f_locals == null)
            getf_locals();
        try {
            f_locals.__delitem__(index);
        } catch (PyException e) {
            if (!Py.matchException(e, Py.KeyError)) throw e;
            throw Py.UnboundLocalError("local: '" + index + "'");
        }
    }

    public void delglobal(String index) {
        f_globals.__delitem__(index);
    }

    // nested scopes helpers

    public PyObject getclosure(int index) {
        return f_env[index];
    }

    public PyObject getderef(int index) {
        PyObject obj = f_env[index].ob_ref;
        if (obj != null) return obj;
        String name;
        if (index >= f_ncells) name = f_code.co_freevars[index - f_ncells];
        else name = f_code.co_cellvars[index];
        throw Py.UnboundLocalError("local: '" + name + "'");
    }

    public void setderef(int index, PyObject value) {
        f_env[index].ob_ref = value;
    }

    public void to_cell(int parm_index, int env_index) {
        f_env[env_index].ob_ref = f_fastlocals[parm_index];
    }
}
