// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.types.Exposed;
import org.python.types.Feature;
import org.python.types.TypeSpec;

/**
 * The Python {@code NameError} exception, and the same class also
 * represents {@code UnboundLocalError}.
 */
public class PyNameError extends PyBaseException {

    /** The type object of Python {@code NameError} exceptions. */
    public static final PyType TYPE =
            PyType.fromSpec(new TypeSpec("NameError", MethodHandles.lookup()) //
                    .base(Exception) //
                    .add(Feature.REPLACEABLE, Feature.IMMUTABLE) //
                    .doc("Name not found globally."));

    /** {@code UnboundLocalError} extends {@code NameError}. */
    static PyType UnboundLocalError = extendsException(TYPE, "UnboundLocalError",
            "Local name referenced but not bound to a value.");

    private static final long serialVersionUID = 1L;

    /** The problematic name (or {@code null}). */
    @Exposed.Member
    @Exposed.DocString("missing name")
    private String name;

    /**
     * Constructor specifying Python type.
     *
     * @param type Python type of the exception
     * @param args positional arguments
     */
    public PyNameError(PyType type, PyTuple args) { super(type, args); }

    // Special methods -----------------------------------------------

    private static final ArgParser INIT_PARSER =
            ArgParser.fromSignature("__init__", "*args", "name").kwdefaults(Py.None);

    @Override
    void __init__(Object[] args, String[] kwds) {
        Object[] frame = INIT_PARSER.parse(args, kwds);
        // frame = [name, *args]
        if (frame[1] instanceof PyTuple argsTuple) { // always is
            this.args = argsTuple;
        }
        // name keyword: can't default directly to null in the parser
        Object name = frame[0];
        this.name = name == Py.None ? null : PyUnicode.asString(name);
    }
}
