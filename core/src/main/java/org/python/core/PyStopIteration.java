// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.types.Feature;
import org.python.types.TypeSpec;

/** The Python {@code StopIteration} exception. */
public class PyStopIteration extends PyBaseException {

    /** The type object of Python {@code StopIteration} exceptions. */
    public static final PyType TYPE =
            PyType.fromSpec(new TypeSpec("StopIteration", MethodHandles.lookup()) //
                    .base(Exception) //
                    .add(Feature.REPLACEABLE, Feature.IMMUTABLE) //
                    .doc("Signal the end from iterator.__next__()."));

    private static final long serialVersionUID = 1L;

    /** The generator return value. */
    private Object value;

    /**
     * Constructor specifying Python type.
     *
     * @param type Python type of the exception
     * @param args positional arguments
     */
    public PyStopIteration(PyType type, PyTuple args) { super(type, args); }

    /** Constructor with default arguments. */
    public PyStopIteration() {
        super(TYPE, PyTuple.EMPTY);
    }

    // Special methods ------------------------------------------------

    // Compare CPython StopIteration_* in exceptions.c

    private static final ArgParser INIT_PARSER =
            ArgParser.fromSignature("__init__", "*args").kwdefaults((Object)null);

    @Override
    void __init__(Object[] args, String[] kwds) {
        Object[] frame = INIT_PARSER.parse(args, kwds);
        // frame = [*args]
        if (frame[0] instanceof PyTuple argsTuple) { // always is
            this.args = argsTuple;
        }
        PyTuple a = this.args;
        this.value = (a.size() == 0) ? Py.None : a.get(0);
    }

}
