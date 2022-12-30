// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.python.base.InterpreterError;
import org.python.base.MethodKind;

/**
 * This class provides a parser for the positional and keyword
 * arguments supplied during a call to a built-in function or
 * method. The purpose of an argument parser is to provide the body
 * of a function, or perhaps a {@code MethodHandle} with an array of
 * values, corresponding in order and number to its parameters
 * (formal arguments).
 * <p>
 * This parser transforms several argument presentations that occur
 * in a Python implementation, and arranges them into an array. This
 * array is either created by the parser, or designated by the
 * caller. The parser may therefore be used to prepare arguments for
 * a pure a Java method (or {@code MethodHandle}) that accepts an
 * array, or to insert arguments as initial values of local
 * variables in an an optimised interpreter frame ({@link PyFrame}).
 * <p>
 * The fields of the parser that determine the acceptable numbers of
 * positional arguments and their names are essentially those of a
 * {@code code} object ({@link PyCode}). Defaults are provided
 * values that mirror the defaults built into a {@code function}
 * object ({@link PyFunction}).
 * <p>
 * Consider for example a function that in Python would have the
 * function definition:<pre>
 * def func(a, b, c=3, d=4, /, e=5, f=6, *aa, g=7, h, i=9, **kk):
 *     pass
 * </pre> This could be described by a constructor call and
 * modifiers: <pre>
 * String[] names = {"a", "b", "c", "d",  "e", "f",  "g", "h", "i",
 *         "aa", "kk"};
 * ArgParser ap = new ArgParser("func", names,
 *         names.length - 2, 4, 3, true, true) //
 *                 .defaults(3, 4, 5, 6) //
 *                 .kwdefaults(7, null, 9);
 * </pre> Note that "aa" and "kk" are at the end of the parameter
 * names. (This is how a CPython frame is laid out.)
 * <p>
 * Defaults are provided, after the parser has been constructed, as
 * values corresponding to parameter names, when right-justified in
 * the space to which they apply. (See diagram below.) Both the
 * positional and keyword defaults are given by position in this
 * formulation. The {@link #kwdefaults(Object...)} call is allowed
 * to supply {@code null} values at positions it does not define.
 * <p>
 * When parsed to an array, the layout of the argument values, in
 * relation to fields of the parser will be as follows.
 * <table class="lined">
 * <caption>A Python {@code frame}</caption>
 * <tr>
 * <td class="row-label">names</td>
 * <td>a</td>
 * <td>b</td>
 * <td>c</td>
 * <td>d</td>
 * <td>e</td>
 * <td>f</td>
 * <td>g</td>
 * <td>h</td>
 * <td>i</td>
 * <td>aa</td>
 * <td>kk</td>
 * </tr>
 * <tr>
 * <td class="row-label" rowspan=3>layout</td>
 * <td colspan=4>posOnly</td>
 * <td colspan=2></td>
 * <td colspan=3>kwOnly</td>
 * </tr>
 * <tr>
 * <td colspan=2></td>
 * <td colspan=4>defaults</td>
 * <td colspan=3 style="border-style: dashed;">kwdefaults</td>
 * </tr>
 * </table>
 * <p>
 * The most readable way of specifying a parser (although one that
 * is a little costly to construct) is to list the parameters as
 * they would be declared in Python, including the furniture that
 * marks up the positional-only, keyword-only, positional varargs,
 * and keyword varargs. This is the API offered by
 * {@link #fromSignature(String, String...)}. In practice we only
 * use this in unit tests. For serious applications we construct the
 * {@code ArgParser} with a complex of arguments derived by
 * inspection of the Java or Python signature.
 */
class ArgParser {

    // Compare code object (and CPython _Arg_Parser in modsupport.h)

    /** Empty names array. */
    private static final String[] NO_STRINGS = new String[0];

    /** Empty object array. */
    private static final Object[] NO_OBJECTS = new Object[0];

    /** The name of the function, mainly for error messages. */
    final String name;

    /**
     * The kind of object (type or module) in which the method is found.
     * This makes a difference to the signature reported for an instance
     * method.
     */
    final ScopeKind scopeKind;

    /**
     * The kind of method (instance, static or class) that this parser
     * works for.
     */
    final MethodKind methodKind;

    /**
     * Names of parameters that could be satisfied by position or
     * keyword, including the collector parameters. Elements are
     * guaranteed to be interned, and not {@code null} or empty. The
     * array must name all the parameters, of which there are:
     * {@code argcount + kwonlyargcount
                + (hasVarArgs() ? 1 : 0) + (hasVarKeywords() ? 1 : 0)}
     * <p>
     * It is often is longer since it suits us to re-use an array that
     * names all the local variables of a frame.
     */
    /*
     * Here and elsewhere we use the same field names as the CPython
     * code, even though it tends to say "argument" when it could mean
     * that or "parameter". In comments and documentation
     * "positional parameter" means a parameter eligible to be satisfied
     * by an argument given by position.
     */
    final String[] argnames;

    /**
     * The number of positional or keyword parameters, excluding the
     * "collector" ({@code *args} and {@code **kwargs}) parameters, and
     * any data that may follow the legitimate parameter names. Equal to
     * {@code argcount + kwonlyargcount}.
     */
    final int regargcount;

    /** The number of <b>positional</b> parameters. */
    final int argcount;

    /**
     * The number of parameters that can only be satisfied by arguments
     * given by position. This differs from {@link #argcount} by
     * excluding parameters that may be given by keyword or position.
     */
    final int posonlyargcount;

    /** The number of keyword-only parameters. */
    final int kwonlyargcount;

    /**
     * The documentation string of the method.
     */
    String doc;

    /**
     * The (positional) default parameters or {@code null} if there are
     * none.
     */
    // Compare CPython PyFunctionObject::func_defaults
    private Object[] defaults;

    /**
     * The keyword defaults, may be a {@code dict} or {@code null} if
     * there are none.
     */
    // Compare CPython PyFunctionObject::func_kwdefaults
    private Map<Object, Object> kwdefaults;

    /**
     * The frame has a collector ({@code tuple}) for excess positional
     * arguments at this index, if it is {@code >=0}.
     */
    final int varArgsIndex;

    /**
     * The frame has a collector ({@code dict}) for excess keyword
     * arguments at this index, if it is {@code >=0}.
     */
    final int varKeywordsIndex;

    /**
     * Construct a parser for a named function, with defined numbers of
     * positional-only and keyword-only parameters, and parameter names
     * in an array prepared by client code.
     * <p>
     * The array of names is used in-place (not copied). The client code
     * must therefore ensure that it cannot be modified after the parser
     * has been constructed.
     * <p>
     * The array of names may be longer than is necessary: the caller
     * specifies how much of the array should be treated as regular
     * parameter names, and whether zero, one or two further elements
     * will name collectors for excess positional or keyword arguments.
     * The rest of the elements will not be examined by the parser. The
     * motivation for this design is to permit efficient construction
     * when the the array of names is the local variable names in a
     * Python {@code code} object.
     *
     * @param name of the function
     * @param names of the parameters including any collectors (varargs)
     * @param regargcount number of regular (non-collector) parameters
     * @param posOnly number of positional-only parameters
     * @param kwOnly number of keyword-only parameters
     * @param varargs whether there is positional collector
     * @param varkw whether there is a keywords collector
     */
    ArgParser(String name, String[] names, int regargcount, int posOnly, int kwOnly,
            boolean varargs, boolean varkw) {
        this(name, ScopeKind.TYPE, MethodKind.STATIC, names, regargcount, posOnly, kwOnly, varargs,
                varkw);
    }

    /**
     * Construct a parser from descriptive parameters that may be
     * derived from a the annotated declarations ({@link Exposed}
     * methods) that appear in type and module definitions written in
     * Java. For;<pre>
     * def func(a, b, c=3, d=4, /, e=5, f=6, *aa, g=7, h, i=9, **kk):
     *     pass
     * </pre>The constructor arguments should specify this layout:
     * <table class="lined">
     * <caption>A Python {@code frame}</caption>
     * <tr>
     * <td class="row-label">names</td>
     * <td>a</td>
     * <td>b</td>
     * <td>c</td>
     * <td>d</td>
     * <td>e</td>
     * <td>f</td>
     * <td>g</td>
     * <td>h</td>
     * <td>i</td>
     * <td>aa</td>
     * <td>kk</td>
     * </tr>
     * <tr>
     * <td class="row-label" rowspan=3>layout</td>
     * <td colspan=4>posOnly</td>
     * <td colspan=2></td>
     * <td colspan=3>kwOnly</td>
     * <td>varargs</td>
     * <td>varkw</td>
     * </tr>
     * <tr>
     * <td colspan=2></td>
     * <td colspan=4>defaults</td>
     * <td colspan=3 style="border-style: dashed;">kwdefaults</td>
     * </tr>
     * </table>
     *
     * @param name of the function
     * @param scopeKind whether module, etc.
     * @param methodKind whether static, etc.
     * @param names of the parameters including any collectors (varargs)
     * @param regargcount number of regular (non-collector) parameters
     * @param posOnly number of positional-only parameters
     * @param kwOnly number of keyword-only parameters
     * @param varargs whether there is positional collector
     * @param varkw whether there is a keywords collector
     */
    ArgParser(String name, ScopeKind scopeKind, MethodKind methodKind, String[] names,
            int regargcount, int posOnly, int kwOnly, boolean varargs, boolean varkw) {

        // Name of function
        this.name = name;
        this.methodKind = methodKind;
        this.scopeKind = scopeKind;
        this.argnames = names;

        // Total parameter count *except* possible varargs, varkwargs
        int N = Math.min(regargcount, names.length);
        this.regargcount = N;
        this.posonlyargcount = posOnly;
        this.kwonlyargcount = kwOnly;
        this.argcount = N - kwOnly;

        // There may be positional and/or keyword collectors
        this.varArgsIndex = varargs ? N++ : -1;
        this.varKeywordsIndex = varkw ? N++ : -1;

        assert argnames.length >= argcount + kwonlyargcount + (hasVarArgs() ? 1 : 0)
                + (hasVarKeywords() ? 1 : 0);
    }

    /**
     * Create a parser, for a named function, with defined numbers of
     * positional-only and keyword-only parameters, and naming the
     * parameters. Parameters that may only be given by position need
     * not be named. ("" is acceptable in the names array.)
     * <p>
     * This is a convenient way to construct a reference result in unit
     * tests.
     *
     * @param name of function
     * @param decl names of parameters and indicators "/", "*", "**"
     * @return the constructed parser
     */
    static ArgParser fromSignature(String name, String... decl) {

        // Collect the names of the parameters here
        ArrayList<String> args = new ArrayList<>();
        String varargs = null, varkw = null;

        int posOnly = 0, posCount = 0;

        /*
         * Scan names, looking out for /, * and ** markers. Nameless
         * parameters are tolerated in the positional-only section.
         */
        for (String param : decl) {
            int paramLen = param.length();
            if (paramLen > 0) {
                if (param.charAt(0) == '/') {
                    // We found a positional-only end marker /
                    posOnly = args.size();
                } else if (param.charAt(0) == '*') {
                    if (paramLen > 1) {
                        if (param.charAt(1) == '*') {
                            // Looks like a keywords collector
                            if (paramLen > 2) {
                                // ... and it has a name.
                                varkw = param.substring(2);
                            }
                        } else {
                            // Looks like a positional collector
                            varargs = param.substring(1);
                            posCount = args.size();
                        }
                    } else {
                        // We found a keyword-only start marker *
                        posCount = args.size();
                    }
                } else {
                    // We found a proper name for the parameter.
                    args.add(param);
                }
            } else {
                // We found a "": tolerate for now.
                args.add("");
            }
        }

        // Total parameter count *except* possible varargs, varkwargs
        int N = args.size();

        /*
         * If there was no "/" or "*", all are positional arguments. This is
         * consistent with the output of inspect.signature, where e.g.
         * inspect.signature(exec) is (source, globals=None, locals=None,
         * /).
         */
        if (posCount == 0) { posCount = N; }

        // Number of regular arguments (not *, **)
        int regArgCount = N;
        int kwOnly = N - posCount;

        // Add any *args to the names
        if (varargs != null) {
            args.add(varargs);
            N++;
        }

        // Add any **kwargs to the names
        if (varkw != null) {
            args.add(varkw);
            N++;
        }

        String[] names = N == 0 ? NO_STRINGS : args.toArray(new String[N]);

        return new ArgParser(name, ScopeKind.TYPE, MethodKind.STATIC, names, regArgCount, posOnly,
                kwOnly, varargs != null, varkw != null);
    }

    /**
     * @return true if default positional arguments are available.
     */
    boolean hasDefaults() { return defaults != null; }

    /**
     * @return a copy of default positional arguments (or empty array).
     */
    Object[] getDefaults() {
        if (defaults == null) {
            return NO_OBJECTS;
        } else {
            return Arrays.copyOf(defaults, defaults.length);
        }
    }

    /**
     * @return true if there is an excess positional argument collector.
     */
    boolean hasVarArgs() { return varArgsIndex >= 0; }

    /**
     * @return true if default keyword-only arguments are available.
     */
    boolean hasKwdefaults() { return kwdefaults != null; }

    /**
     * @return true if there is an excess keyword argument collector.
     */
    boolean hasVarKeywords() { return varKeywordsIndex >= 0; }

    /**
     * The representation of an {@code ArgParser} is based on the
     * {@code __text_signature__} attribute of built-in methods (see
     * {@link #textSignature()}) and the specifications found in CPython
     * Argument Clinic.
     */
    @Override
    public String toString() { return name + textSignature(); }

    /**
     * Return a string representing the argument list of the method. The
     * string is like that found in the {@code __text_signature__}
     * attribute of built-in methods.
     *
     * @return the signature of the arguments
     */
    String textSignature() {
        StringJoiner sj = new StringJoiner(", ", "(", ")");
        int empty = sj.length();

        // Keyword only parameters start at k
        int k = regargcount - kwonlyargcount;
        // The positional defaults start at d
        int d = k - (defaults == null ? 0 : defaults.length);
        // We work through the parameters with index i
        int i = 0;

        // Possible leading argument
        switch (methodKind) {
            case INSTANCE:
                // $module, $self
                sj.add(scopeKind.selfName);
                break;
            case CLASS:
                sj.add("$type");
                break;
            default: // STATIC = no mention
                break;
        }

        // Positional-only parameters
        while (i < posonlyargcount) { sj.add(parameterToString(i++, d)); }

        // If there were any positional-only parameters ...
        if (sj.length() > empty) {
            // ... mark the end of them.
            sj.add("/");
        }

        // Positional (but not positional-only) parameters
        while (i < k) { sj.add(parameterToString(i++, d)); }

        // Reached the end of the positional section
        if (hasVarArgs()) {
            // Excess from the positional section goes to a *args
            sj.add("*" + argnames[varArgsIndex]);
        } else if (i < regargcount) {
            // Mark the end but no *args to catch the excess
            sj.add("*");
        }

        // Keyword only parameters begin properly
        while (i < regargcount) { sj.add(parameterToString(i++)); }

        if (hasVarKeywords()) {
            // Excess from the keyword section does to a **kwargs
            sj.add("**" + argnames[varKeywordsIndex]);
        }

        return sj.toString();
    }

    /**
     * Return <i>i</i>th positional parameter name and default value if
     * available. Helper to {@link #sigString()}.
     */
    private String parameterToString(int i, int d) {
        if (i < d)
            return argnames[i];
        else {
            // A default value is available
            Object value = defaults[i - d];
            return argnames[i] + "=" + repr(value);
        }
    }

    /**
     * Return <i>i</i>th parameter name and keyword default value if
     * available. Helper to {@link #sigString()}.
     */
    private String parameterToString(int i) {
        String name = argnames[i];
        if (kwdefaults != null) {
            Object value = kwdefaults.get(name);
            if (value != null) {
                // A default value is available
                return argnames[i] + "=" + repr(value);
            }
        }
        return name;
    }

    /**
     * Weak substitute for {@code repr()} that will do for common types
     * of default argument.
     *
     * @param o object to reproduce
     * @return poorman's {@code repr(o)}
     */
    private static String repr(Object o) {
        if (o instanceof String) {
            String s = (String)o;
            if (!s.contains("'"))
                return "'" + s + "'";
            else
                return "\"" + s + "\"";
        } else {
            return o.toString();
        }
    }

    /**
     * Parse {@code __call__} arguments and create an array, using the
     * arguments supplied and the defaults held in the parser.
     *
     * @param args all arguments, positional then keyword
     * @param names of keyword arguments
     * @return array of parsed arguments
     */
    Object[] parse(Object[] args, String[] names) {
        Object[] a = new Object[argnames.length];
        FrameWrapper w = new ArrayFrameWrapper(a);
        parseToFrame(w, args, 0, args.length, names);
        return a;
    }

    /**
     * Parse CPython-style vector call arguments and create an array,
     * using the arguments supplied and the defaults held in the parser.
     *
     * @param s positional and keyword arguments
     * @param p position of arguments in the array
     * @param n number of positional <b>and keyword</b> arguments
     * @param names of keyword arguments or {@code null}
     * @return array of parsed arguments
     */
    Object[] parse(Object[] s, int p, int n, String[] names) {
        Object[] a = new Object[argnames.length];
        FrameWrapper w = new ArrayFrameWrapper(a);
        parseToFrame(w, s, p, n, names);
        return a;
    }

    /**
     * Parse classic arguments and create an array, using the arguments
     * supplied and the defaults held in the parser.
     *
     * @param args positional arguments
     * @param kwargs keyword arguments
     * @return array of parsed arguments
     */
    Object[] parse(PyTuple args, PyDict kwargs) {
        Object[] a = new Object[argnames.length];
        FrameWrapper w = new ArrayFrameWrapper(a);
        parseToFrame(w, args, kwargs);
        return a;
    }

    /**
     * Provide the positional defaults. * The {@code ArgParser} keeps a
     * reference to this array, so that subsequent changes to it will
     * affect argument parsing. (Concurrent access to the array and
     * parser is a client issue.)
     * <p>
     * If L values are provided, they correspond to
     * {@code arg[max-L] ... arg[max-1]}, where {@code max} is the index
     * of the first keyword-only parameter, or the number of parameters
     * if there are no keyword-only parameters. The minimum number of
     * positional arguments will then be {@code max-L}.
     *
     * @param values replacement positional defaults (or {@code null})
     * @return {@code this}
     */
    ArgParser defaults(Object... values) {
        defaults = values;
        checkShape();
        return this;
    }

    /**
     * Provide the keyword-only defaults as values. If K values are
     * provided, they correspond to {@code arg[N-K] ... arg[N-1]}, where
     * {@code N} is the number of regular parameters
     * ({@link #regargcount}). If the argument is empty, it is converted
     * to {@code null} internally. The number of keyword-only parameters
     * and positional-only parameters must not together exceed the
     * number of regular parameters named in the constructor.
     *
     * @param values keyword values aligned to the parameter names
     * @return {@code this}
     */
    ArgParser kwdefaults(Object... values) {
        PyDict d = new PyDict();
        int K = values.length;
        for (int i = 0, p = regargcount - K; i < K; i++, p++) {
            Object v = values[i];
            if (v != null) { d.put(argnames[p], v); }
        }
        kwdefaults = d;
        checkShape();
        return this;
    }

    /**
     * Provide the keyword-only defaults, perhaps as a {@code dict}. The
     * {@code ArgParser} keeps a reference to this map, so that
     * subsequent changes to it will affect argument parsing, as
     * required for a Python {@link PyFunction function}. (Concurrent
     * access to the mapping and parser is a client issue.)
     *
     * @param kwd replacement keyword defaults (or {@code null})
     * @return {@code this}
     */
    ArgParser kwdefaults(Map<Object, Object> kwd) {
        kwdefaults = kwd;
        checkShape();
        return this;
    }

    /**
     * The number of keyword-only parameters and positional-only
     * parameters must not together exceed the number of parameters
     * named in the constructor. (The last two are defined in the
     * constructor.) Nor must there be excess default values for the
     * number of parameters.
     */
    private void checkShape() {
        // XXX This may be too fussy, given that Python function is not
        final int N = argcount;
        final int L = defaults == null ? 0 : defaults.length;
        final int K = kwonlyargcount;
        final int W = kwdefaults == null ? 0 : kwdefaults.size();

        int min = N - L;
        int kwmax = N + K - posonlyargcount;

        if (min < 0) {
            throw new InterpreterError(TOO_MANY_DEFAULTS, L, N, name);
        } else if (W > kwmax) { throw new InterpreterError(TOO_MANY_KWDEFAULTS, W, kwmax, name); }
    }

    private static final String TOO_MANY_DEFAULTS = "More defaults (%d given) than "
            + "positional parameters (%d allowed) when specifying '%s'";

    private static final String TOO_MANY_KWDEFAULTS =
            "More keyword defaults (%d given) than remain after "
                    + "positional-only parameters (%d left) when specifying '%s'";

    /** Get the name of arg i or make one up. */
    private String nameArg(int i) {
        String arg = argnames[i].toString();
        if (arg.length() == 0) { arg = String.format("arg %d", i + 1); }
        return arg;
    }

    /**
     * Abstract wrapper for storage that the enclosing argument parser
     * should be able to fill from the arguments to a Python call.
     * Typically this wrapper is a window onto the local variables of a
     * function invocation (a {@link PyFrame}) that the run-time is in
     * the process of initialising during a call.
     */
    abstract class FrameWrapper {

        /**
         * Get the local variable named by {@code argnames[i]}
         *
         * @param i index of variable name in {@code argnames}
         * @return value of variable named {@code argnames[i]}
         */
        abstract Object getLocal(int i);

        /**
         * Set the local variable named by {@code argnames[i]}
         *
         * @param i index of variable name in {@code argnames}
         * @param v to assign to variable named {@code argnames[i]}
         */
        abstract void setLocal(int i, Object v);

        /**
         * Copy positional arguments into local variables, making sure we
         * don't copy more than have been allowed for in the frame.
         * Providing too many or too few is not an error at this stage, as
         * there may be a collector to catch the excess arguments or
         * positional or keyword defaults to make up the shortfall.
         *
         * @param args positional arguments
         */
        void setPositionalArguments(PyTuple args) {
            int n = Math.min(args.value.length, argcount);
            for (int i = 0; i < n; i++)
                setLocal(i, args.value[i]);
        }

        /**
         * Copy positional arguments into local variables, making sure we
         * don't copy more than have been allowed for in the frame.
         * Providing too many or too few is not an error at this stage, as
         * there may be a collector to catch the excess arguments or
         * positional or keyword defaults to make up the shortfall.
         *
         * @param stack positional and keyword arguments
         * @param pos position of arguments in the array
         * @param nargs number of positional arguments
         */
        void setPositionalArguments(Object[] stack, int pos, int nargs) {
            int n = Math.min(nargs, argcount);
            for (int i = 0, j = pos; i < n; i++)
                setLocal(i, stack[j++]);
        }

        /**
         * For each of the names used as keywords in the call, match it with
         * an allowable parameter name, and assign that frame-local variable
         * the keyword argument given in the call. If the variable is not
         * null, this is an error.
         * <p>
         * "Allowable parameter name" here means the names in
         * {@code argnames[p:q]} where {@code p=posonlyargcount} and
         * {@code q=argcount + kwonlyargcount}. If the name used in the call
         * is not an allowable keyword, then if this parser allows for
         * excess keywords, add it to the frame's keyword dictionary,
         * otherwise throw an informative error.
         * <p>
         * In this version, accept the keyword arguments passed as a
         * dictionary, as in the "classic" {@code (*args, **kwargs)} call.
         *
         * @param kwargs keyword arguments given in call
         */
        void setKeywordArguments(PyDict kwargs) {
            /*
             * Create a dictionary for the excess keyword parameters, and insert
             * it in the local variables at the proper position.
             */
            PyDict kwdict = null;
            if (hasVarKeywords()) {
                kwdict = Py.dict();
                setLocal(varKeywordsIndex, kwdict);
            }

            /*
             * For each entry in kwargs, search argnames for a match, and either
             * assign the local variable or add the name-value pair to kwdict.
             */
            for (Map.Entry<Object, Object> entry : kwargs.entrySet()) {
                Object name = entry.getKey();
                Object value = entry.getValue();
                int index = argnamesIndexOf(name);

                if (index < 0) {
                    // Not found in (allowed slice of) argnames
                    if (kwdict != null)
                        kwdict.put(name, value);
                    else
                        // No kwdict: non-match is an error.
                        throw unexpectedKeyword(name, kwargs.keySet());
                } else {
                    // Keyword found to name allowable variable at index
                    if (getLocal(index) == null)
                        setLocal(index, value);
                    else
                        // Unfortunately, that seat is already taken
                        throw new TypeError(MULTIPLE_VALUES, name, name);
                }
            }
        }

        /**
         * For each of the names used as keywords in the call, match it with
         * an allowable parameter name, and assign that frame-local variable
         * the keyword argument given in the call. If the variable is not
         * null, this is an error.
         * <p>
         * "Allowable parameter name" here means the names in
         * {@code argnames[p:q]} where {@code p=posonlyargcount} and
         * {@code q=argcount + kwonlyargcount}. If the name used in the call
         * is not an allowable keyword, then if this parser allows for
         * excess keywords, add it to the frame's keyword dictionary,
         * otherwise throw an informative error.
         * <p>
         * In this version, accept the keyword arguments passed as a
         * dictionary, as in the "classic" {@code (*args, **kwargs)} call.
         *
         * @param stack {@code [kwstart:kwstart+len(kwnames)]} values
         *     corresponding to {@code kwnames} in order
         * @param kwstart start position in {@code kwvalues}
         * @param kwnames keywords used in the call (or {@code **kwargs})
         */
        void setKeywordArguments(Object[] stack, int kwstart, String[] kwnames) {

            PyDict kwdict = null;
            if (varKeywordsIndex >= 0) {
                /*
                 * Create a dictionary for the excess keyword parameters, and insert
                 * it in the local variables at the proper position.
                 */
                kwdict = Py.dict();
                setLocal(varKeywordsIndex, kwdict);
            }

            /*
             * For each of the names in kwnames, search argnames for a match,
             * and either assign the local variable or add the name-value pair
             * to kwdict.
             */
            int kwcount = kwnames == null ? 0 : kwnames.length;
            for (int i = 0, j = kwstart; i < kwcount; i++) {
                String key = kwnames[i];
                Object value = stack[j++];
                int index = argnamesIndexOf(key);

                if (index < 0) {
                    // Not found in (allowed slice of) argnames
                    if (kwdict != null)
                        // Put unmatched (name, value) in dict.
                        kwdict.put(key, value);
                    else
                        // No kwdict: non-match is an error.
                        throw unexpectedKeyword(key, Arrays.asList(kwnames));
                } else {
                    // Keyword found to name allowable variable at index
                    if (getLocal(index) == null)
                        setLocal(index, value);
                    else
                        // Unfortunately, that seat is already taken
                        throw new TypeError(MULTIPLE_VALUES, name, key);
                }
            }
        }

        /**
         * Find the given name in {@code argnames}, and if it is not found,
         * return -1. Only the "allowable parameter names", those acceptable
         * as keyword arguments, are searched. It is an error if the name is
         * not a Python {@code str}.
         *
         * @param name parameter name given as keyword
         * @return index of {@code name} in {@code argnames} or -1
         */
        private int argnamesIndexOf(Object name) {

            int end = regargcount;

            if (name == null || !(PyUnicode.TYPE.check(name))) {
                throw new TypeError(KEYWORD_NOT_STRING, name);
            }

            /*
             * For speed, try raw pointer comparison. As names are normally
             * interned Strings this should almost always hit.
             */
            for (int i = posonlyargcount; i < end; i++) {
                if (argnames[i] == name)
                    return i;
            }

            /*
             * It's not definitive until we have repeated the search using
             * proper object comparison.
             */
            for (int i = posonlyargcount; i < end; i++) {
                if (Abstract.richCompareBool(name, argnames[i], Comparison.EQ, null))
                    return i;
            }

            return -1;
        }

        /**
         * Fill in missing positional parameters from a from {@code defs}.
         * If any positional parameters are cannot be filled, this is an
         * error. The number of positional arguments {@code nargs} is
         * provided so we know where to start only for their number.
         * <p>
         * It is harmless (but a waste) to call this when
         * {@code nargs >= argcount}.
         *
         * @param nargs number of positional arguments given in call
         * @param defs default values by position or {@code null}
         * @throws TypeError if there are still missing arguments.
         */
        void applyDefaults(int nargs, Object[] defs) throws TypeError {

            int ndefs = defs == null ? 0 : defs.length;
            /*
             * At this stage, the first nargs parameter slots have been filled
             * and some (or all) of the remaining argcount-nargs positional
             * parameters may have been assigned using keyword arguments.
             * Meanwhile, defs is available to provide values for (only) the
             * last defs.length positional parameters.
             */
            // locals[nargs:m] have no default values, where:
            int m = argcount - ndefs;
            int missing = 0;
            for (int i = nargs; i < m; i++) { if (getLocal(i) == null) { missing++; } }
            if (missing > 0) { throw missingArguments(missing, ndefs); }

            /*
             * Variables in locals[m:argcount] may take defaults from defs, but
             * perhaps nargs > m. Begin at index nargs, but not necessarily at
             * the start of defs.
             */
            for (int i = nargs, j = Math.max(nargs - m, 0); j < ndefs; i++, j++) {
                if (getLocal(i) == null) { setLocal(i, defs[j]); }
            }
        }

        /**
         * Deal with missing keyword arguments, attempting to fill them from
         * {@code kwdefs}. If any parameters are unfilled after that, this
         * is an error.
         *
         * It is harmless (but a waste) to call this when
         * {@code kwonlyargcount == 0}.
         *
         * @param kwdefs default values by keyword or {@code null}
         * @throws TypeError if there are too many or missing arguments.
         */
        void applyKWDefaults(Map<Object, Object> kwdefs) throws TypeError {
            /*
             * Variables in locals[argcount:end] are keyword-only parameters. If
             * they have not been assigned yet, they take values from dict
             * kwdefs.
             */
            int end = regargcount;
            int missing = 0;
            for (int i = argcount; i < end; i++) {
                Object value = getLocal(i);
                if (value == null && kwdefs != null)
                    setLocal(i, value = kwdefs.get(argnames[i]));
                if (value == null) { missing++; }
            }
            if (missing > 0) { throw missingArguments(missing, -1); }
        }

        static final String KEYWORD_NOT_STRING = "%.200s(): keywords must be strings";
        static final String KEYWORD_NOT_COMPARABLE = "Keyword names %s not comparable.";
        static final String MULTIPLE_VALUES = "%.200s(): multiple values for parameter '%s'";
        static final String POSITIONAL_ONLY =
                "%.200s(): positional-only argument%s passed by keyword: %s";
        static final String UNEXPECTED_KEYWORD = "%.200s(): unexpected keyword argument '%s'";

        /*
         * Compare CPython ceval.c::too_many_positional(). Unlike that
         * function, on diagnosing a problem, we do not have to set a
         * message and return status. Also, when called there is *always* a
         * problem, and therefore an exception.
         */
        // XXX Do not report kw arguments given: unnatural constraint.
        /*
         * The caller must defer the test until after kw processing, just so
         * the actual kw-args given can be reported accurately. Otherwise,
         * the test could be after (or part of) positional argument
         * processing.
         */
        protected TypeError tooManyPositional(int posGiven) {
            boolean posPlural = false;
            int kwGiven = 0;
            String posText, givenText;
            int defcount = defaults == null ? 0 : defaults.length;
            int end = regargcount;

            assert (!hasVarArgs());

            // Count keyword-only args given
            for (int i = argcount; i < end; i++) { if (getLocal(i) != null) { kwGiven++; } }

            if (defcount != 0) {
                posPlural = true;
                posText = String.format("from %d to %d", argcount - defcount, argcount);
            } else {
                posPlural = (argcount != 1);
                if (argcount == 0) {
                    posText = "no";
                } else {
                    posText = String.format("%d", argcount);
                }
            }

            if (kwGiven > 0) {
                String format = " positional argument%s (and %d keyword-only argument%s)";
                givenText = String.format(format, posGiven != 1 ? "s" : "", kwGiven,
                        kwGiven != 1 ? "s" : "");
            } else {
                givenText = "";
            }

            return new TypeError("%s() takes %s positional argument%s but %d%s %s given", name,
                    posText, posPlural ? "s" : "", posGiven, givenText,
                    (posGiven == 1 && kwGiven == 0) ? "was" : "were");
        }

        /**
         * Diagnose an unexpected keyword occurring in a call and represent
         * the problem as an exception. The particular keyword may
         * incorrectly name a positional parameter, or it may be entirely
         * unexpected (not be a parameter at all). In any case, since this
         * error is going to be fatal to the call, this method looks at
         * <i>all</i> the keywords to see if any are positional-only
         * parameters, and if that's not the problem, reports just the
         * originally-offending keyword as unexpected.
         * <p>
         * We call this method when any keyword has been encountered that
         * does not match a legitimate parameter, and there is no
         * {@code **kwargs} dictionary to catch it. Because Python makes it
         * possible to supply keyword arguments from a {@code map} with
         * {@code object} keys, we accept any object as a keyword name.
         *
         * @param <K> type of element in keyword collection
         * @param kw the unexpected keyword encountered in the call
         * @param kwnames all the keywords used in the call
         * @return TypeError diagnosing the problem
         */
        /*
         * Compare CPython ceval.c::positional_only_passed_as_keyword(), and
         * the code around its call. Unlike that function, on diagnosing a
         * problem, we do not have to set a message and return status. Also,
         * when called there is *always* a problem, and therefore an
         * exception.
         */
        protected <K> TypeError unexpectedKeyword(Object kw, Collection<? extends K> kwnames) {
            /*
             * Compare each of the positional only parameter names with each of
             * the keyword names given in the call. Collect the matches in a
             * list.
             */
            List<String> names = new ArrayList<>();
            for (int k = 0; k < posonlyargcount; k++) {
                String varname = argnames[k];
                for (K keyword : kwnames) {
                    if (Abstract.richCompareBool(varname, keyword, Comparison.EQ, null))
                        names.add(keyword.toString());
                }
            }

            if (!names.isEmpty()) {
                // We caught one or more matches: throw
                return new TypeError(POSITIONAL_ONLY, name, names.size() == 1 ? "" : "s",
                        String.join(", ", names));
            } else {
                // No match, so it is just unexpected altogether
                return new TypeError(UNEXPECTED_KEYWORD, name, kw);
            }
        }

        /**
         * Diagnose which positional or keywords arguments are missing, and
         * throw {@link TypeError} listing them. We call this when we have
         * already detected a problem, and the process is one of going over
         * the data again to create an accurate message.
         *
         * @param missing number of missing arguments
         * @param defcount number of positional defaults available (or -1)
         * @return TypeError listing names of the missing arguments
         */
        /*
         * Compare CPython ceval.c::missing_arguments(). Unlike that
         * function, on diagnosing a problem, we do not have to set a
         * message and return status so the caller can "goto fail" and clean
         * up. We can just throw directly.
         */
        protected TypeError missingArguments(int missing, int defcount) {
            String kind;
            int start, end;

            // Choose the range in which to look for null arguments
            if (defcount >= 0) {
                kind = "positional";
                start = 0;
                end = argcount - defcount;
            } else {
                kind = "keyword-only";
                start = argcount;
                end = start + kwonlyargcount;
            }

            // Make a list of names from that range where value is null
            ArrayList<String> names = new ArrayList<>(missing);
            for (int i = start, j = 0; i < end; i++) {
                if (getLocal(i) == null) { names.add(j++, nameArg(i)); }
            }

            // Formulate an error from the list
            return missingNamesTypeError(kind, names);
        }

        /**
         * Compose a {@link TypeError} from the missing argument names.
         */
        /*
         * Compare CPython ceval.c::format_missing(). Unlike that function,
         * on diagnosing a problem, we do not have to set a message and
         * return status so the caller can "goto fail" and clean up. We can
         * just throw directly.
         */
        private TypeError missingNamesTypeError(String kind, ArrayList<String> names) {
            int len = names.size();
            String joinedNames;

            switch (len) {
                case 0:
                    // Shouldn't happen but let's avoid trouble
                    joinedNames = "";
                    break;
                case 1:
                    joinedNames = names.get(0);
                    break;
                case 2:
                    joinedNames =
                            String.format("%s and %s", names.get(len - 2), names.get(len - 1));
                    break;
                default:
                    String tail =
                            String.format(", %s and %s", names.get(len - 2), names.get(len - 1));
                    // Chop off the last two objects in the list.
                    names.remove(len - 1);
                    names.remove(len - 2);
                    // Stitch into a nice comma-separated list.
                    joinedNames = String.join(", ", names) + tail;
            }

            return new TypeError("%s() missing %d required %s argument%s: %s", name, len, kind,
                    len == 1 ? "" : "s", joinedNames);
        }
    }

    /**
     * Wrap an array provided by a client so that the enclosing argument
     * parser may fill it from the arguments to a Python call. This
     * array could be the local variables in the frame of a function
     * being called, or an argument in the call of a method handle that
     * accepts its arguments as an array. See:
     * {@link ArgParser#parseToFrame(FrameWrapper, PyTuple, PyDict)}.
     */
    class ArrayFrameWrapper extends FrameWrapper {

        private final Object[] vars;
        final int start;

        /**
         * Wrap a slice of an existing array. The elements to fill are a
         * slice of the destination array with specified starting index. The
         * intended use is that {@code start = 1} allows space for a
         * {@code self} reference not in the argument list. The capacity of
         * the array, between the start index and the end, must be
         * sufficient to hold the parse result may be larger, e.g. to
         * accommodate other local variables.
         *
         * @param vars destination array
         * @param start at which to place first parsed argument
         */
        ArrayFrameWrapper(Object[] vars, int start) {
            super();
            this.vars = vars;
            this.start = start;
            assert start + argcount <= vars.length;
        }

        /**
         * Wrap an existing array. The capacity of the array must be
         * sufficient to hold the parse result.
         *
         * @param vars destination array
         */
        ArrayFrameWrapper(Object[] vars) { this(vars, 0); }

        @Override
        Object getLocal(int i) { return vars[start + i]; }

        @Override
        void setLocal(int i, Object v) { vars[start + i] = v; }

        @Override
        void setPositionalArguments(PyTuple argsTuple) {
            int n = Math.min(argsTuple.value.length, argcount);
            System.arraycopy(argsTuple.value, 0, vars, start, n);
        }

        @Override
        void setPositionalArguments(Object[] stack, int pos, int nargs) {
            int n = Math.min(nargs, argcount);
            System.arraycopy(stack, pos, vars, start, n);
        }
    }

    /**
     * Parse when an args tuple and keyword dictionary are supplied,
     * that is, for a classic call.
     *
     * @param frame to populate with argument values
     * @param args positional arguments given
     * @param kwargs keyword arguments given
     */
    void parseToFrame(FrameWrapper frame, PyTuple args, PyDict kwargs) {

        final int nargs = args.value.length;

        // Set parameters from the positional arguments in the call.
        frame.setPositionalArguments(args);

        // Set parameters from the keyword arguments in the call.
        if (kwargs != null && !kwargs.isEmpty())
            frame.setKeywordArguments(kwargs);

        if (nargs > argcount) {

            if (hasVarArgs()) {
                // Locate the * parameter in the frame
                // Put the excess positional arguments there
                frame.setLocal(varArgsIndex, new PyTuple(args.value, argcount, nargs - argcount));
            } else {
                // Excess positional arguments but no *args for them.
                throw frame.tooManyPositional(nargs);
            }

        } else { // nargs <= argcount

            if (hasVarArgs()) {
                // No excess: set the * parameter in the frame to empty
                frame.setLocal(varArgsIndex, PyTuple.EMPTY);
            }

            if (nargs < argcount) {
                // Set remaining positional parameters from default
                frame.applyDefaults(nargs, defaults);
            }
        }

        if (kwonlyargcount > 0)
            // Set keyword parameters from default values
            frame.applyKWDefaults(kwdefaults);
    }

    /**
     * Parse when an args array and keyword array are supplied, that is,
     * for a vector call on a stack slice.
     *
     * @param frame to populate with argument values
     * @param stack array containing all arguments
     * @param start of the slice in the stack
     * @param nargs number of arguments in the slice, whether position
     *     or keyword
     * @param kwnames (implying number) of keyword arguments
     */
    void parseToFrame(FrameWrapper frame, Object[] stack, int start, int nargs, String[] kwnames) {

        // Number of arguments given by keyword
        int nkwargs = kwnames == null ? 0 : kwnames.length;
        // From here on, number of arguments given by position
        nargs = nargs - nkwargs;

        /*
         * Here, CPython applies certain criteria for calling a fast path
         * that (in our terms) calls only setPositionalArguments(). Those
         * that depend only on code or defaults we make when those
         * attributes are defined.
         */

        // Set parameters from the positional arguments in the call.
        if (nargs > 0) { frame.setPositionalArguments(stack, start, nargs); }

        // Set parameters from the keyword arguments in the call.
        if (varKeywordsIndex >= 0 || nkwargs > 0) {
            frame.setKeywordArguments(stack, start + nargs, kwnames);
        }

        if (nargs > argcount) {

            if (varArgsIndex >= 0) {
                // Put the excess positional arguments in the *args
                frame.setLocal(varArgsIndex,
                        new PyTuple(stack, start + argcount, nargs - argcount));
            } else {
                // Excess positional arguments but nowhere for them.
                throw frame.tooManyPositional(nargs);
            }

        } else { // nargs <= argcount

            if (varArgsIndex >= 0) {
                // No excess: set the * parameter in the frame to empty
                frame.setLocal(varArgsIndex, PyTuple.EMPTY);
            }

            if (nargs < argcount) {
                // Set remaining positional parameters from default
                frame.applyDefaults(nargs, defaults);
            }
        }

        if (kwonlyargcount > 0)
            // Set keyword parameters from default values
            frame.applyKWDefaults(kwdefaults);
    }

    /**
     * Parse when an args array and keyword array are supplied, that is,
     * for a standard {@code __call__}.
     *
     * @param frame to populate with argument values
     * @param args all arguments, positional then keyword
     * @param kwnames of keyword arguments (or {@code null})
     */
    void parseToFrame(FrameWrapper frame, Object[] args, String[] kwnames) {

        // Number of arguments given by keyword
        int nkwargs = kwnames == null ? 0 : kwnames.length;
        // Number of arguments given by position
        int nargs = args.length - nkwargs;

        /*
         * Here, CPython applies certain criteria for calling a fast path
         * that (in our terms) calls only setPositionalArguments(). Those
         * that depend only on code or defaults we make when those
         * attributes are defined.
         */

        // Set parameters from the positional arguments in the call.
        if (nargs > 0) { frame.setPositionalArguments(args, 0, nargs); }

        // Set parameters from the keyword arguments in the call.
        if (varKeywordsIndex >= 0 || nkwargs > 0) {
            frame.setKeywordArguments(args, nargs, kwnames);
        }

        if (nargs > argcount) {

            if (varArgsIndex >= 0) {
                // Put the excess positional arguments in the *args
                frame.setLocal(varArgsIndex, new PyTuple(args, argcount, nargs - argcount));
            } else {
                // Excess positional arguments but nowhere for them.
                throw frame.tooManyPositional(nargs);
            }

        } else { // nargs <= argcount

            if (varArgsIndex >= 0) {
                // No excess: set the * parameter in the frame to empty
                frame.setLocal(varArgsIndex, PyTuple.EMPTY);
            }

            if (nargs < argcount) {
                // Set remaining positional parameters from default
                frame.applyDefaults(nargs, defaults);
            }
        }

        if (kwonlyargcount > 0)
            // Set keyword parameters from default values
            frame.applyKWDefaults(kwdefaults);
    }
}
