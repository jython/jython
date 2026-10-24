package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.python.types.TypeFlag;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/** Holder for objects appearing in the closure of a function. */
public class PyCell implements Supplier<Object>, WithClass {

    /** The Python type {@code cell}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("cell", MethodHandles.lookup())
                    // Type admits no Python subclasses.
                    .flagNot(TypeFlag.BASETYPE));

    /** The object currently held. */
    Object obj;

    /**
     * Construct a cell to hold the object.
     *
     * @param obj to hold
     */
    PyCell(Object obj) { this.obj = obj; }

    /** Handy constant where no cells are neeed in a frame. */
    static final PyCell[] EMPTY_ARRAY = new PyCell[0];

    // Java API -------------------------------------------------------

    @Override
    public PyType getType() { return TYPE; }

    @Override
    public Object get() { return obj; }

    @Override
    public String toString() { return Py.defaultToString(this); }

    // slot functions -------------------------------------------------

    @SuppressWarnings("unused")
    private Object __repr__() { return String.format("<cell [%.80s]>", obj); }
}
