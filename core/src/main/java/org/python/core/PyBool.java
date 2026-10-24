// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.types.TypeSpec;
import org.python.types.WithClass;
import org.python.types.Exposed.Default;
import org.python.types.Exposed.DocString;
import org.python.types.Exposed.PythonNewMethod;

/**
 * The Python {@code bool} object. The only instances of {@code bool} in
 * Python are {@code False} and {@code True}, represented by Java
 * {@code Boolean.FALSE} and {@code Boolean.TRUE}, and there are no
 * sub-classes. (Rogue instances of Java {@code Boolean} will generally
 * behave as {@code False} or {@code True} but may fail identity tests.)
 */
public final class PyBool implements WithClass {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /**
         * The {@code get()} of {@code PyBool.Spec} has a special form that
         * allows us to communicate its Python base {@code int} before that
         * object can be reliably referenced as {@link PyLong#TYPE}.
         *
         * @param base type object ({@code int}) to cite as a base
         * @return the type specification.
         */
        static TypeSpec get(PyType base) {
            return new TypeSystem.BootstrapSpec("bool", MethodHandles.lookup(), Boolean.class)
                    .methodImpls(PyBool.class) //
                    .base(base);
        }
    }

    /** The Python type {@code bool}. */
    public static final PyType TYPE = TypeSystem.TYPE_bool;

    private PyBool() {}  // enforces the doubleton :)

    @Override
    public PyType getType() { return TYPE; }

    // Constructor from Python ----------------------------------------

    /**
     * @param cls ignored since {@code bool} cannot be sub-classed.
     * @param x to convert to its truth value.
     * @return {@code False} or {@code True}
     * @throws Throwable on argument type or other errors
     */
    @PythonNewMethod
    @DocString("""
            bool(x) -> bool

            Returns True when the argument x is true, False otherwise.
            The builtins True and False are the only two instances of the class bool.
            The class bool is a subclass of the class int, and cannot be subclassed.
            """)
    static Object __new__(PyType cls, @Default("False") Object x) throws Throwable {
        assert cls == TYPE;  // Guaranteed by construction (maybe)
        return Abstract.isTrue(x);
    }

    // Special methods ------------------------------------------------

    @SuppressWarnings("unused")
    private static Object __repr__(Boolean self) { return self ? "True" : "False"; }

    @SuppressWarnings("unused")
    private static Object __and__(Boolean v, Object w) {
        if (w instanceof Boolean)
            return v ? w : v;
        else
            // w is not a bool, go arithmetic.
            return PyLongMethods.__and__(v, w);
    }

    @SuppressWarnings("unused")
    private static Object __rand__(Boolean w, Object v) {
        if (v instanceof Boolean)
            return w ? v : w;
        else
            // v is not a bool, go arithmetic.
            return PyLongMethods.__rand__(w, v);
    }

    @SuppressWarnings("unused")
    private static Object __or__(Boolean v, Object w) {
        if (w instanceof Boolean)
            return v ? v : w;
        else
            // w is not a bool, go arithmetic.
            return PyLongMethods.__or__(v, w);
    }

    @SuppressWarnings("unused")
    private static Object __ror__(Boolean w, Object v) {
        if (v instanceof Boolean)
            return w ? w : v;
        else
            // v is not a bool, go arithmetic.
            return PyLongMethods.__ror__(w, v);
    }

    @SuppressWarnings("unused")
    private static Object __xor__(Boolean v, Object w) {
        if (w instanceof Boolean wb)
            return v ^ wb;
        else
            // w is not a bool, go arithmetic.
            return PyLongMethods.__xor__(v, w);
    }

    @SuppressWarnings("unused")
    private static Object __rxor__(Boolean w, Object v) {
        if (v instanceof Boolean vb)
            return vb ^ w;
        else
            // v is not a bool, go arithmetic.
            return PyLongMethods.__rxor__(w, v);
    }
}
