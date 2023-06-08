// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Miscellaneous static helpers commonly needed to implement Python
 * objects in Java.
 */
public class PyObjectUtil {

    private PyObjectUtil() {} // no instances

    /**
     * An implementation of {@code dict.__repr__} that may be applied to
     * any Java {@code Map} between {@code Object}s, in which keys and
     * values are represented as with {@code repr()}.
     *
     * @param map to be reproduced
     * @return a string like <code>{'a': 2, 'b': 3}</code>
     * @throws Throwable from the {@code repr()} implementation
     */
    static String mapRepr(Map<? extends Object, ?> map) throws Throwable {
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        for (Map.Entry<? extends Object, ?> e : map.entrySet()) {
            String key = Abstract.repr(e.getKey()).toString();
            String value = Abstract.repr(e.getValue()).toString();
            sj.add(key + ": " + value);
        }
        return sj.toString();
    }

    /**
     * A string along the lines "T object at 0xhhh", where T is the type
     * of {@code o}. This is for creating default {@code __repr__}
     * implementations seen around the code base and containing this
     * form. By implementing it here, we encapsulate the problem of
     * qualified type name and what "address" or "identity" should mean.
     *
     * @param o the object (not its type)
     * @return string denoting {@code o}
     */
    static String toAt(Object o) {
        // For the time being identity means:
        int id = System.identityHashCode(o);
        // For the time being type name means:
        String typeName = PyType.of(o).name;
        return String.format("%s object at %#x", typeName, id);
    }

    /**
     * Produce a {@code String} name for a function-like object or its
     * {@code str()} if it doesn't even have a
     * {@code __qualname__}.<pre>
     *     def functionStr(func):
     *         try:
     *             qualname = func.__qualname__
     *         except AttributeError:
     *             return str(func)
     *         try:
     *             module = func.__module__
     *             if module is not None and mod != 'builtins':
     *                 return ".".join(module, qualname)
     *         except AttributeError:
     *             pass
     *         return qualname
     * </pre> This differs from its CPython counterpart
     * {@code _PyObject_FunctionStr} by decisively not adding
     * parentheses.
     *
     * @param func the function
     * @return a name for {@code func}
     */
    // Compare CPython _PyObject_FunctionStr in object.c
    static String functionStr(Object func) {
        Object name;
        try {
            Object qualname = Abstract.lookupAttr(func, "__qualname__");
            if (qualname != null) {
                Object module = Abstract.lookupAttr(func, "__module__");
                if (module != null && module != Py.None
                        && Abstract.richCompareBool("builtins", module, Comparison.NE)) {
                    name = Callables.callMethod(".", "join", module, qualname);
                }
                name = qualname;
            } else {
                name = Abstract.str(func);
            }
            return PyUnicode.asString(name);
        } catch (Throwable e) {
            // Unlike CPython fall back on a generic answer
            return "function";
        }
    }

    /**
     * The type of exception thrown when an attempt to convert an object
     * to a common data type fails. This type of exception carries no
     * stack context, since it is used only as a sort of "alternative
     * return value".
     */
    public static class NoConversion extends Exception {
        private static final long serialVersionUID = 1L;

        private NoConversion() { super(null, null, false, false); }
    }

    /**
     * A statically allocated {@link NoConversion} used in conversion
     * methods to signal "cannot convert". No stack context is preserved
     * in the exception.
     */
    public static final NoConversion NO_CONVERSION = new NoConversion();

    /**
     * A statically allocated {@link StopIteration} used to signal
     * exhaustion of an iterator, but providing no useful stack context.
     */
    public static final StopIteration STOP_ITERATION = new StopIteration();
}
