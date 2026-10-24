// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;

import org.python.types.Exposed.Default;
import org.python.types.Exposed.DocString;
import org.python.types.Exposed.KeywordOnly;
import org.python.types.Exposed.Name;
import org.python.types.Exposed.PositionalCollector;
import org.python.types.Exposed.PositionalOnly;
import org.python.types.Exposed.PythonStaticMethod;

/**
 * The {@code builtins} module is definitely called "builtins".
 * <p>
 * Although it is fully a module, the {@link BuiltinsModule} lives in
 * the {@code core} package because it needs privileged access to the
 * core implementation that extension modules do not.
 */
class BuiltinsModule extends JavaModule {

    private static final ModuleDef DEFINITION = new ModuleDef("builtins", MethodHandles.lookup());

    /** Construct an instance of the {@code builtins} module. */
    BuiltinsModule() {
        super(DEFINITION);
    }

    @Override
    public void exec() {
        super.exec();

        /*
         * We expose these objects by simply adding them to the module
         * dictionary. Methods exposed by the module are exposed by annotating
         * them. The ModuleExposer will enter them in the same dictionary but by
         * a substantially more complex process.
         */
        // This list is taken from CPython bltinmodule.c
        add("None", Py.None);
        // add("Ellipsis", Py.Ellipsis);
        add("NotImplemented", Py.NotImplemented);
        add("False", Py.False);
        add("True", Py.True);
        add("bool", PyBool.TYPE);
        // add("memoryview", PyMemoryView.TYPE);
        // add("bytearray", PyByteArray.TYPE);
        add("bytes", PyBytes.TYPE);
        // add("classmethod", PyClassMethod.TYPE);
        // add("complex", PyComplex.TYPE);
        add("dict", PyDict.TYPE);
        // add("enumerate", PyEnum.TYPE);
        // add("filter", PyFilter.TYPE);
        add("float", PyFloat.TYPE);
        // add("frozenset", PyFrozenSet.TYPE);
        // add("property", PyProperty.TYPE);
        add("int", PyLong.TYPE);
        add("list", PyList.TYPE);
        // add("map", PyMap.TYPE);
        add("object", PyObject.TYPE);
        // add("range", PyRange.TYPE);
        // add("reversed", PyReversed.TYPE);
        // add("set", PySet.TYPE);
        add("slice", PySlice.TYPE);
        // add("staticmethod", PyStaticMethod.TYPE);
        add("str", PyUnicode.TYPE);
        // add("super", PySuper.TYPE);
        add("tuple", PyTuple.TYPE);
        add("type", PyType.TYPE());
        // add("zip", PyZip.TYPE);
    }

    /**
     * Return the absolute value of the argument.
     *
     * @param x argument
     * @return the absolute value of the argument.
     * @throws Throwable from implementation of {@code __abs__}
     */
    @PythonStaticMethod
    @DocString("Return the absolute value of the argument.")
    static Object abs(Object x) throws Throwable { return PyNumber.absolute(x); }

    /**
     * Execute Python code supplied dynamically (at run time).
     * <p>
     * We support the closure optional argument seen in Python 3.11.
     *
     * @param source string or code object
     * @param globals global namespace (a {@code dict} or {@code None})
     * @param locals local namespace (a {@code mapping}or {@code None})
     * @param closure free variables expected, only when source is a
     *     {@code code} object)
     */
    @PythonStaticMethod
    @DocString("""
            Execute the given source in the context of globals and locals.

            The source may be a string representing one or more Python statements
            or a code object as returned by compile().
            The globals must be a dictionary and locals can be any mapping,
            defaulting to the current globals and locals.
            If only globals is given, locals defaults to it.
            The closure must be a tuple of cellvars, and can only be used
            when source is a code object requiring exactly that many cellvars.""")
    static void exec(Object source, @Default("None") Object globals,
            @PositionalOnly @Default("None") Object locals,
            @KeywordOnly @Default("None") Object closure) {

        ThreadState tstate = ThreadState.get();
        PyDict globalsDict;
        boolean localsGiven = locals != Py.None && locals != null;

        /*
         * Establish globalsDict and locals object we shall actually use when we
         * run the code.
         */
        if (globals == Py.None || globals == null) {
            /*
             * No globals given, so we're going to have to get it from the stack.
             * (Fails if stack is empty.)
             */
            globalsDict = tstate.getGlobals();
            if (!localsGiven) {
                // No locals given. Get from stack too.
                locals = tstate.getLocals();
            }
        } else {
            // globals argument given: check it is sensible.
            // TODO Avoid the cast by passing class (and deducing type)
            globalsDict = (PyDict)PyUtil.typeChecked(globals, PyDict.TYPE,
                    o -> Abstract.argumentTypeError("exec", "globals", "a dict", o));
            if (!localsGiven) {
                // No locals given. Use globals as locals too.
                locals = globalsDict;
            }
        }

        /*
         * If we didn't choose them ourselves, check that the locals given as an
         * argument are a mapping.
         */
        if (localsGiven && !PyMapping.check(locals)) {
            throw Abstract.argumentTypeError("exec", "locals", "a mapping or None", locals);
        }

        /*
         * Ensure the globalsDict specifies the builtins. PyFunction will look
         * for it here before asking the Interpreter.
         */
        Interpreter interp = tstate.getInterpreter();
        globalsDict.putIfAbsent("__builtins__", tstate.getBuiltins());

        // Get the closure tuple (if given) as an array of cells.
        final PyCell[] free;
        if (closure == Py.None) {
            free = null;
        } else if (PyTuple.TYPE.checkExact(closure)) {
            free = ((PyTuple)closure).toArray(PyCell.class, o -> closureError("tuple of cells", o));
        } else {
            throw closureError("a tuple", closure);
        }

        if (PyCode.TYPE.check(source)) {
            // The source has been supplied compiled.
            PyCode code = (PyCode)source;

            // Check the closure matches code expectations
            int nfree = code.co_freevars().size();
            if (nfree == 0) {
                if (free != null) { throw PyErr.format(PyExc.TypeError, CANNOT_USE_CLOSURE); }
            } else {
                if (free == null || free.length != nfree) {
                    throw PyErr.format(PyExc.TypeError, CLOSURE_LENGTH, nfree);
                }
            }

            // CPython: auditable event
            // if (audit("exec", "O", source) < 0) { return null; }

            PyFunction<?> func = code.createFunction(interp, globalsDict, null, null, null, free);
            PyFrame<?> frame = func.createFrame(locals);
            frame.eval();

        } else {
            /*
             * The source is s string so we have to compile it. A closure is not
             * allowed in this case.
             */
            if (closure != null) { throw PyErr.format(PyExc.TypeError, CLOSURE_ONLY_WHEN_CODE); }

            // Unported CPython: not ready to reproduce compilation
            // PyBytes source_copy;
            // ByteBuffer str; // Or other suitable bytes!
            // Normalise source to UTF8 bytes
            // CompilerFlags cf = CompilerFlags.INIT;
            // cf.cf_flags = PyCF_SOURCE_IS_UTF8;
            // str = _Py_SourceAsString(source, "exec",
            // "string, bytes or code", &cf,
            // &source_copy);
            // if (str == null) {
            // return null;}
            // if (PyEval_MergeCompilerFlags(&cf))
            // v = PyRun_StringFlags(str, Py_file_input, globals,
            // locals, &cf);
            // else
            // v = PyRun_String(str, Py_file_input, globals, locals);
        }
    }

    /**
     * Create a {@link PyExc#TypeError TypeError} with a message along the
     * lines "exec() closure argument must be T, not X", involving an
     * expected type description T and the type X of {@code o}.
     *
     * @param description of the expected kind of argument
     * @param o actual argument (not its type)
     * @return exception to throw
     */
    private static PyBaseException closureError(String description, Object o) {
        return Abstract.argumentTypeError("exec", "closure", description, o);

    }

    /**
     * Return the dictionary containing the current scope's global
     * variables.
     *
     * @return the current scope's local variables.
     * @throws PyBaseException (SystemError) if there is no current scope.
     */
    @PythonStaticMethod
    @DocString("Return the dictionary containing the current scope's global variables.")
    static Object globals() throws PyBaseException { return ThreadState.get().getGlobals(); }

    /**
     * Return the number of items in a container or string.
     *
     * @param v container or string
     * @return number of items
     * @throws Throwable from {@code __len__} implementation
     */
    @PythonStaticMethod
    @DocString("Return the number of items in a container.")
    static Object len(Object v) throws Throwable { return PySequence.size(v); }

    /**
     * Return a dictionary containing the current scope's local variables.
     *
     * @implNote Whether or not updates to this dictionary will affect name
     *     lookups in the local scope and vice-versa is implementation
     *     dependent and not covered by any backwards compatibility
     *     guarantees.
     *
     * @return the current scope's local variables.
     * @throws PyBaseException (SystemError) if there is no current scope.
     */
    @PythonStaticMethod
    @DocString("Return a dictionary containing the current scope's local variables.")
    static Object locals() throws PyBaseException { return ThreadState.get().getLocals(); }

    /**
     * Implementation of {@code max()}.
     *
     * @param arg1 a first argument or iterable of arguments
     * @param args contains other positional arguments
     * @param key function
     * @param dflt to return when iterable is empty
     * @return {@code max} result or {@code dflt}
     * @throws Throwable from calling {@code key} or comparison
     */
    @PythonStaticMethod(positionalOnly = false)
    @DocString("Return the largest item in an iterable"
            + " or the largest of two or more arguments.")
    // Simplified version of max()
    static Object max(Object arg1, @KeywordOnly @Default("None") Object key,
            @Name("default") @Default("None") Object dflt, @PositionalCollector PyTuple args)
            throws Throwable {
        // @PositionalCollector has to be last.
        return minmax(arg1, args, key, dflt, Comparison.GT);
    }

    /**
     * Implementation of {@code min()}.
     *
     * @param arg1 a first argument or iterable of arguments
     * @param args contains other positional arguments
     * @param key function
     * @param dflt to return when iterable is empty
     * @return {@code min} result or {@code dflt}
     * @throws Throwable from calling {@code key} or comparison
     */
    @PythonStaticMethod(positionalOnly = false)
    @DocString("Return the smallest item in an iterable"
            + " or the smallest of two or more arguments.")
    // Simplified version of min()
    static Object min(Object arg1, @KeywordOnly @Default("None") Object key,
            @Name("default") @Default("None") Object dflt, @PositionalCollector PyTuple args)
            throws Throwable {
        // @PositionalCollector has to be last.
        return minmax(arg1, args, key, dflt, Comparison.LT);
    }

    /**
     * Implementation of both {@link #min(Object, Object, Object, PyTuple)
     * min()} and {@link #max(Object, Object, Object, PyTuple) max()}.
     *
     * @param arg1 a first argument or iterable of arguments
     * @param args contains other positional arguments
     *
     * @param key function
     * @param dflt to return when iterable is empty
     * @param op {@code LT} for {@code min} and {@code GT} for {@code max}.
     * @return min or max result as appropriate
     * @throws Throwable from calling {@code op} or {@code key}
     */
    // Compare CPython min_max in Python/bltinmodule.c
    private static Object minmax(Object arg1, PyTuple args, Object key, Object dflt, Comparison op)
            throws Throwable {

        int n = args.size();
        Object result;
        Iterator<Object> others;
        assert key != null;

        if (n > 0) {
            /*
             * Positional mode: arg1 is the first value, args contains the other
             * values to compare
             */
            result = key == Py.None ? arg1 : Callables.call(key, arg1);
            others = args.iterator();
            if (dflt != Py.None) {
                String name = op == Comparison.LT ? "min" : "max";
                throw PyErr.format(PyExc.TypeError, DEFAULT_WITHOUT_ITERABLE, name);
            }

        } else {
            // Single iterable argument of the values to compare
            result = null;
            // TODO define PySequence.iterable like PyMapping.map?
            others = PySequence.fastList(arg1, null).iterator();
        }

        // Now we can get on with the comparison
        while (others.hasNext()) {
            Object item = others.next();
            if (key != Py.None) { item = Callables.call(key, item); }
            if (result == null) {
                result = item;
            } else if (Abstract.richCompareBool(item, result, op)) { result = item; }
        }

        // result may be null if the single iterable argument is empty
        if (result != null) {
            return result;
        } else if (dflt != Py.None) {
            assert dflt != null;
            return dflt;
        } else {
            String name = op == Comparison.LT ? "min" : "max";
            throw PyErr.format(PyExc.ValueError, "%s() arg is an empty sequence", name);
        }
    }

    private static final String DEFAULT_WITHOUT_ITERABLE =
            "Cannot specify a default for %s() with multiple positional arguments";

    /**
     * Return the canonical string representation of the object.
     *
     * @param obj to represent
     * @return as a string
     * @throws Throwable from the implementation
     */
    @PythonStaticMethod
    @DocString("""
            Return the canonical string representation of the object.

            For many object types, including most builtins,
            eval(repr(obj)) == obj.""")
    static Object repr(Object obj) throws Throwable { return Abstract.repr(obj); }

    private static final String CLOSURE_LENGTH =
            "code object requires a closure of length exactly %d";
    private static final String CANNOT_USE_CLOSURE = "cannot use a closure with this code object";
    private static final String CLOSURE_ONLY_WHEN_CODE =
            "closure can only be used when source is a code object";
}
