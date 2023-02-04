// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;

import org.python.core.Exposed.Default;
import org.python.core.Exposed.DocString;
import org.python.core.Exposed.KeywordOnly;
import org.python.core.Exposed.Name;
import org.python.core.Exposed.PositionalCollector;
import org.python.core.Exposed.PythonStaticMethod;

/**
 * The {@code builtins} module is definitely called "builtins".
 * <p>
 * Although it is fully a module, the {@link BuiltinsModule} lives
 * in the {@code core} package because it needs privileged access to
 * the core implementation that extension modules do not.
 */
class BuiltinsModule extends JavaModule {

    private static final ModuleDef DEFINITION = new ModuleDef("builtins", MethodHandles.lookup());

    /** Construct an instance of the {@code builtins} module. */
    BuiltinsModule() {
        super(DEFINITION);

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
        add("object", PyBaseObject.TYPE);
        // add("range", PyRange.TYPE);
        // add("reversed", PyReversed.TYPE);
        // add("set", PySet.TYPE);
        add("slice", PySlice.TYPE);
        // add("staticmethod", PyStaticMethod.TYPE);
        add("str", PyUnicode.TYPE);
        // add("super", PySuper.TYPE);
        add("tuple", PyTuple.TYPE);
        add("type", PyType.TYPE);
        // add("zip", PyZip.TYPE);
    }

    @PythonStaticMethod
    @DocString("Return the absolute value of the argument.")
    static Object abs(Object x) throws Throwable { return PyNumber.absolute(x); }

    @PythonStaticMethod
    @DocString("Return the number of items in a container.")
    static Object len(Object v) throws Throwable { return PySequence.size(v); }

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
     * Implementation of both
     * {@link #min(Object, Object, Object, PyTuple) min()} and
     * {@link #max(Object, Object, Object, PyTuple) max()}.
     *
     * @param arg1 a first argument or iterable of arguments
     * @param args contains other positional arguments
     *
     * @param key function
     * @param dflt to return when iterable is empty
     * @param op {@code LT} for {@code min} and {@code GT} for
     *     {@code max}.
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
            result = key == Py.None ? arg1 : Callables.callFunction(key, arg1);
            others = args.iterator();
            if (dflt != Py.None) {
                String name = op == Comparison.LT ? "min" : "max";
                throw new TypeError(DEFAULT_WITHOUT_ITERABLE, name);
            }

        } else {
            // Single iterable argument of the values to compare
            result = null;
            // XXX define PySequence.iterable like PyMapping.map?
            others = PySequence.fastList(arg1, null).iterator();
        }

        // Now we can get on with the comparison
        while (others.hasNext()) {
            Object item = others.next();
            if (key != Py.None) { item = Callables.callFunction(key, item); }
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
            throw new ValueError("%s() arg is an empty sequence", name);
        }
    }

    private static final String DEFAULT_WITHOUT_ITERABLE =
            "Cannot specify a default for %s() with multiple positional arguments";

    @PythonStaticMethod
    @DocString("Return the canonical string representation of the object.\n"
            + "For many object types, including most builtins, eval(repr(obj)) == obj.")
    static Object repr(Object obj) throws Throwable { return Abstract.repr(obj); }
}
