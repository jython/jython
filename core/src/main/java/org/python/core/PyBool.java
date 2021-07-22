package org.python.core;

import java.lang.invoke.MethodHandles;

/**
 * The Python {@code bool} object. The only instances of
 * {@code bool} in Python are {@code False} and {@code True},
 * represented by Java {@code Boolean.FALSE} and
 * {@code Boolean.TRUE}, and there are no sub-classes. (Rogue
 * instances of Java {@code Boolean} will generally behave as
 * {@code False} or {@code True} but may fail identity tests.)
 */
final class PyBool {

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("bool", MethodHandles.lookup()) //
                    .canonical(Boolean.class) //
                    .base(PyLong.TYPE) //
                    .flagNot(PyType.Flag.BASETYPE));

    private PyBool() {}  // enforces the doubleton :)

    // special methods ------------------------------------------------

    static Object __repr__(Boolean self) { return self ? "True" : "False"; }

    static Object __and__(Boolean v, Object w) {
        if (w instanceof Boolean)
            return v ? w : v;
        else
            // w is not a bool, go arithmetic.
            return PyLong.__and__(v, w);
    }

    static Object __rand__(Boolean w, Object v) {
        if (v instanceof Boolean)
            return w ? v : w;
        else
            // v is not a bool, go arithmetic.
            return PyLong.__rand__(w, v);
    }

    static Object __or__(Boolean v, Object w) {
        if (w instanceof Boolean)
            return v ? v : w;
        else
            // w is not a bool, go arithmetic.
            return PyLong.__or__(v, w);
    }

    static Object __ror__(Boolean w, Object v) {
        if (v instanceof Boolean)
            return w ? w : v;
        else
            // v is not a bool, go arithmetic.
            return PyLong.__ror__(w, v);
    }

    static Object __xor__(Boolean v, Object w) {
        if (w instanceof Boolean)
            return v ^ ((Boolean)w);
        else
            // w is not a bool, go arithmetic.
            return PyLong.__xor__(v, w);
    }

    static Object __rxor__(Boolean w, Object v) {
        if (v instanceof Boolean)
            return ((Boolean)v) ^ w;
        else
            // v is not a bool, go arithmetic.
            return PyLong.__rxor__(w, v);
    }
}
