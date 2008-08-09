package org.python.core;

/**
 * A utility class for handling mixed positional and keyword arguments.
 * 
 * A typical usage:
 * 
 * <pre>
 *   public MatchObject search(PyObject[] args, String[] kws) {
 *       ArgParser ap = new ArgParser(&quot;search&quot;, args, kws,
 *                                    &quot;pattern&quot;, &quot;pos&quot;, &quot;endpos&quot;);
 *       String string = ap.getString(0);
 *       int start     = ap.getInt(1, 0);
 *       int end       = ap.getInt(2, string.length());
 *       ...
 * </pre>
 */

public class ArgParser {
    // The name of the function. Used in exception messages
    private String funcname;

    // The actual argument values.
    private PyObject[] args;

    // The list of actual keyword names.
    private String[] kws;

    // The list of allowed and expected keyword names.
    private String[] params = null;

    // A marker.
    private static Object required = new Object();

    private static String[] emptyKws = new String[0];

    // private PyBuiltinFunction.Info info;

    private ArgParser(String funcname, PyObject[] args, String[] kws) {
        this.funcname = funcname;
        this.args = args;
        if (kws == null) {
            kws = emptyKws;
        }
        this.kws = kws;
    }

    /**
     * Create an ArgParser with one method argument
     * 
     * @param funcname Name of the method. Used in error messages.
     * @param args The actual call arguments supplied in the call.
     * @param kws The actual keyword names supplied in the call.
     * @param p0 The expected argument in the method definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws, String p0) {
        this(funcname, args, kws);
        this.params = new String[] { p0 };
        check();
    }

    /**
     * Create an ArgParser with two method argument
     * 
     * @param funcname Name of the method. Used in error messages.
     * @param args The actual call arguments supplied in the call.
     * @param kws The actual keyword names supplied in the call.
     * @param p0 The first expected argument in the method definition.
     * @param p1 The second expected argument in the method definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws, String p0,
            String p1) {
        this(funcname, args, kws);
        this.params = new String[] { p0, p1 };
        check();
    }

    /**
     * Create an ArgParser with three method argument
     * 
     * @param funcname Name of the method. Used in error messages.
     * @param args The actual call arguments supplied in the call.
     * @param kws The actual keyword names supplied in the call.
     * @param p0 The first expected argument in the method definition.
     * @param p1 The second expected argument in the method definition.
     * @param p2 The third expected argument in the method definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws, String p0,
            String p1, String p2) {
        this(funcname, args, kws);
        this.params = new String[] { p0, p1, p2 };
        check();
    }

    /**
     * Create an ArgParser with three method argument
     * 
     * @param funcname Name of the method. Used in error messages.
     * @param args The actual call arguments supplied in the call.
     * @param kws The actual keyword names supplied in the call.
     * @param paramnames The list of expected argument in the method definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws,
            String[] paramnames) {
        this(funcname, args, kws);
        this.params = paramnames;
        check();
    }

    public ArgParser(String funcname, PyObject[] args, String[] kws,
            String[] paramnames, int minargs) {
        this(funcname, args, kws);
        this.params = paramnames;
        check();
        if (!PyBuiltinFunction.DefaultInfo.check(args.length, minargs,
                this.params.length)) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(args.length,
                    false, funcname, minargs, this.params.length);
        }
    }

    /**
     * Return a required argument as a String.
     * 
     * @param pos The position of the .. First argument is numbered 0.
     */
    public String getString(int pos) {
        return (String) getArg(pos, String.class, "string");
    }

    /**
     * Return an optional argument as a String.
     * 
     * @param pos The position of the argument. First argument is numbered 0.
     */
    public String getString(int pos, String def) {
        return (String) getArg(pos, String.class, "string", def);
    }

    /**
     * Return a required argument as an int.
     * 
     * @param pos The position of the argument. First argument is numbered 0.
     */
    public int getInt(int pos) {
        return asInt(getRequiredArg(pos));
    }

    /**
     * Return an optional argument as an int.
     * 
     * @param pos The position of the argument. First argument is numbered 0.
     */
    public int getInt(int pos, int def) {
        PyObject value = getOptionalArg(pos);
        if (value == null) {
            return def;
        }
        return asInt(value);
    }

    /**
     * Convert a PyObject to a Java integer.
     *
     * @param value a PyObject
     * @return value as an int
     */
    private int asInt(PyObject value) {
        if (value instanceof PyFloat) {
            Py.warning(Py.DeprecationWarning, "integer argument expected, got float");
            value = value.__int__();
        }
        return value.asInt();
    }

    /**
     * Return a required argument as a PyObject.
     * 
     * @param pos The position of the argument. First argument is numbered 0.
     */
    public PyObject getPyObject(int pos) {
        return getRequiredArg(pos);
    }

    /**
     * Return an optional argument as a PyObject.
     * 
     * @param pos The position of the argument. First argument is numbered 0.
     */
    public PyObject getPyObject(int pos, PyObject def) {
        PyObject value = getOptionalArg(pos);
        if (value == null) {
            value = def;
        }
        return value;
    }

    /**
     * Return a required argument as a PyObject, ensuring the object
     * is of the specified type.
     *
     * @param pos the position of the argument. First argument is numbered 0
     * @param type the desired PyType of the argument
     * @return the PyObject of PyType
     */
    public PyObject getPyObjectByType(int pos, PyType type) {
        PyObject arg = getRequiredArg(pos);
        if (!Py.isInstance(arg, type)) {
            throw Py.TypeError(String.format("argument %d must be %s, not %s", pos + 1,
                                             type.fastGetName(), arg.getType().fastGetName()));
        }
        return arg;
    }

    /**
     * Return the remaining arguments as a tuple.
     * 
     * @param pos The position of the argument. First argument is numbered 0.
     */
    public PyObject getList(int pos) {
        int kws_start = this.args.length - this.kws.length;
        if (pos < kws_start) {
            PyObject[] ret = new PyObject[kws_start - pos];
            System.arraycopy(this.args, pos, ret, 0, kws_start - pos);
            return new PyTuple(ret);
        }
        return Py.EmptyTuple;
    }

    /**
     * Ensure no keyword arguments were passed, raising a TypeError if
     * so.
     *
     */
    public void noKeywords() {
        if (kws.length > 0) {
            throw Py.TypeError(String.format("%s does not take keyword arguments", funcname));
        }
    }

    private void check() {
        int nargs = this.args.length - this.kws.length;
        l1: for (int i = 0; i < this.kws.length; i++) {
            for (int j = 0; j < this.params.length; j++) {
                if (this.kws[i].equals(this.params[j])) {
                    if (j < nargs) {
                        throw Py.TypeError("keyword parameter '"
                                + this.params[j]
                                + "' was given by position and by name");
                    }
                    continue l1;
                }
            }
            throw Py.TypeError("'" + this.kws[i] + "' is an invalid keyword "
                    + "argument for this function");
        }
    }

    private PyObject getRequiredArg(int pos) {
        PyObject ret = getOptionalArg(pos);
        if (ret == null) {
            throw Py.TypeError(this.funcname + ": The " + ordinal(pos)
                    + " argument is required");
        }
        return ret;
    }

    private PyObject getOptionalArg(int pos) {
        int kws_start = this.args.length - this.kws.length;
        if (pos < kws_start) {
            return this.args[pos];
        }
        for (int i = 0; i < this.kws.length; i++) {
            if (this.kws[i].equals(this.params[pos])) {
                return this.args[kws_start + i];
            }
        }
        return null;
    }

    private Object getArg(int pos, Class clss, String classname) {
        return getArg(pos, clss, classname, required);
    }

    private Object getArg(int pos, Class clss, String classname, Object def) {
        PyObject value = null;
        if (def == required) {
            value = getRequiredArg(pos);
        } else {
            value = getOptionalArg(pos);
            if (value == null) {
                return def;
            }
        }

        Object ret = value.__tojava__(clss);
        if (ret == Py.NoConversion) {
            throw Py.TypeError("argument " + (pos + 1) + ": expected "
                    + classname + ", " + value.getType().fastGetName() + " found");
        }
        return ret;
    }

    private static String ordinal(int n) {
        switch (n + 1) {
        case 1:
            return "1st";
        case 2:
            return "2nd";
        case 3:
            return "3rd";
        default:
            return Integer.toString(n + 1) + "th";
        }
    }
}
