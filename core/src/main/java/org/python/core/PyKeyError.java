// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.types.Feature;
import org.python.types.TypeSpec;

/** The Python {@code KeyError} exception. */
public class PyKeyError extends PyBaseException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code KeyError} exceptions. */
    public static final PyType TYPE =
            PyType.fromSpec(new TypeSpec("KeyError", MethodHandles.lookup()) //
                    .base(LookupError) //
                    .add(Feature.REPLACEABLE, Feature.IMMUTABLE) //
                    .doc("Mapping key not found."));

    /**
     * Constructor specifying Python type and the argument tuple as the
     * associated value. In Python, {@code KeyError} inherits
     * {@code __new__} and {@code __init__} from {@code BaseException},
     * which surprisingly is enough. Note that
     * {@link PyBaseException#__new__(PyType, PyTuple, PyDict)} will find
     * and call this constructor by reflection. *
     *
     * @param type Python type of the exception
     * @param args positional arguments
     */
    public PyKeyError(PyType type, PyTuple args) { super(type, args); }

    /**
     * If the argument tuple contains just one object, that element is
     * assumed to be the problematic key, and is returned.
     *
     * @return the problematic key or {@code None}
     */
    final Object key() { return args.size() == 1 ? args.get(0) : Py.None; }

    // Special methods -----------------------------------------------

    /**
     * {@code str()} of this {@code KeyError} object which is either the
     * {@code repr()} of the argument tuple, or of its sole element.
     * Frequently, the sole element is some key that was not found during
     * lookup in a mapping, and if it is a string, will be represented in
     * quotes.
     * <p>
     * This is done so that e.g. the exception raised by {@code {}['']}
     * prints {@code KeyError: ''} rather than the confusing
     * {@code KeyError} alone. If {@code KeyError} is raised with an
     * explanatory string, that string will be displayed in quotes.
     *
     * @return {@code str()} of this object.
     * @throws Throwable from formatting {@code args}
     */
    // Compare CPython KeyError_* in exceptions.c
    @Override
    protected Object __str__() throws Throwable {
        if (args.size() == 1) {
            return Abstract.repr(args.get(0));
        } else {
            return super.__str__();
        }
    }
}
