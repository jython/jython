package org.python.core;

/**
 * A utility class for handling mixed positional and keyword arguments.
 *
 * A typical usage:
 * <pre>
 *  public MatchObject search(PyObject[] args, String[] kws) {
 *      ArgParser ap = new ArgParser("search", args, kws,
 *                                   "pattern", "pos", "endpos");
 *      String string = ap.getString(0);
 *      int start     = ap.getInt(1, 0);
 *      int end       = ap.getInt(2, string.length());
 *      ...
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

    private ArgParser(String funcname, PyObject[] args, String[] kws) {
        this.funcname = funcname;
        this.args = args;
        if (kws == null)
            kws = emptyKws;
        this.kws = kws;
    }

    /**
     * Create an ArgParser with one method argument
     * @param  funcname   Name of the method. Used in error messages.
     * @param  args       The actual call arguments supplied in the call.
     * @param  args       The actual keyword names supplied in the call.
     * @param  p0         The expected argument in the method definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws,
                     String p0) {
        this(funcname, args, kws);
        this.params = new String[] { p0 };
        check();
    }

    /**
     * Create an ArgParser with two method argument
     * @param  funcname   Name of the method. Used in error messages.
     * @param  args       The actual call arguments supplied in the call.
     * @param  args       The actual keyword names supplied in the call.
     * @param  p0         The first expected argument in the method
                          definition.
     * @param  p1         The second expected argument in the method
                          definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws,
                     String p0, String p1) {
        this(funcname, args, kws);
        this.params = new String[] { p0, p1 };
        check();
    }

    /**
     * Create an ArgParser with three method argument
     * @param  funcname   Name of the method. Used in error messages.
     * @param  args       The actual call arguments supplied in the call.
     * @param  args       The actual keyword names supplied in the call.
     * @param  p0         The first expected argument in the method
                          definition.
     * @param  p1         The second expected argument in the method
                          definition.
     * @param  p2         The third expected argument in the method
                          definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws,
                     String p0, String p1, String p2) {
        this(funcname, args, kws);
        this.params = new String[] { p0, p1, p2 };
        check();
    }

    /**
     * Create an ArgParser with three method argument
     * @param funcname   Name of the method. Used in error messages.
     * @param args       The actual call arguments supplied in the call.
     * @param args       The actual keyword names supplied in the call.
     * @param paramnames The list of expected argument in the method
                          definition.
     */
    public ArgParser(String funcname, PyObject[] args, String[] kws,
                     String[] paramnames) {
        this(funcname, args, kws);
        this.params = paramnames;
        check();
    }


    /**
     * Return a required argument as a String.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public String getString(int pos) {
         return (String) getArg(pos, String.class, "string");
    }

    /**
     * Return an optional argument as a String.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public String getString(int pos, String def) {
         return (String) getArg(pos, String.class, "string", def);
    }


    /**
     * Return a required argument as an int.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public int getInt(int pos) {
         return getRequiredArg(pos).__int__().getValue();
    }

    /**
     * Return an optional argument as an int.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public int getInt(int pos, int def) {
         PyObject value = getOptionalArg(pos);
         if (value == null)
             return def;
         return value.__int__().getValue();
    }


    /**
     * Return a required argument as a PyObject.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public PyObject getPyObject(int pos) {
         return getRequiredArg(pos);
    }

    /**
     * Return an optiona argument as a PyObject.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public PyObject getPyObject(int pos, PyObject def) {
         PyObject value = getOptionalArg(pos);
         if (value == null)
             value = def;
         return value;
    }

    /**
     * Return the remaining arguments as a tuple.
     * @param pos    The position of the argument. First argument is
     *               numbered 0.
     */
    public PyObject getList(int pos) {
        int kws_start = args.length - kws.length;
        if (pos < kws_start) {
            PyObject[] ret = new PyObject[kws_start - pos];
            System.arraycopy(args, pos, ret, 0, kws_start - pos);
            return new PyTuple(ret);
        }
        return Py.EmptyTuple;
    }


    private void check() {
        l1:
        for (int i = 0; i < kws.length; i++) {
            for (int j = 0; j < params.length; j++) {
                if (kws[i].equals(params[j]))
                    continue l1;
            }
            throw Py.TypeError(kws[i] + " is an invalid keyword argument " +
                               "for this function");
        }
    }

    private PyObject getRequiredArg(int pos) {
        PyObject ret = getOptionalArg(pos);
        if (ret == null)
            throw Py.TypeError(funcname + ": The " + ordinal(pos) +
                               " argument is required");
        return ret;
    }

    private PyObject getOptionalArg(int pos) {
        int kws_start = args.length - kws.length;
        if (pos < kws_start)
            return args[pos];
        for (int i = 0; i < kws.length; i++) {
            if (kws[i].equals(params[pos]))
                return args[kws_start + i];
        }
        return null;
    }


    private Object getArg(int pos, Class clss, String classname) {
        return getArg(pos, clss, classname, required);
    }

    private Object getArg(int pos, Class clss, String classname, Object def) {
        PyObject value = null;
        if (def == required)
            value = getRequiredArg(pos);
        else {
            value = getOptionalArg(pos);
            if (value == null)
                return def;
        }

        Object ret = value.__tojava__(clss);
        if (ret == Py.NoConversion)
            throw Py.TypeError("argument " + (pos+1) + ": expected " +
                               classname + ", " + Py.safeRepr(value) +
                               " found");
        return ret;
    }


    private static String ordinal(int n) {
        switch(n+1) {
        case 1:
            return "1st";
        case 2:
            return "2nd";
        case 3:
            return "3rd";
        default:
            return Integer.toString(n+1)+"th";
        }
    }
}
