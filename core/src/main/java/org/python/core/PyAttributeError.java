// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.types.Exposed;
import org.python.types.Feature;
import org.python.types.TypeSpec;

/**
 * The Python {@code AttributeError} exception.
 */
public class PyAttributeError extends PyBaseException {

    /** The type object of Python {@code AttributeError} exceptions. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("AttributeError", MethodHandles.lookup()) //
                    .base(Exception) //
                    .add(Feature.REPLACEABLE, Feature.IMMUTABLE) //
                    .doc("Attribute not found."));

    private static final long serialVersionUID = 1L;

    /** The problematic attribute name. */
    @Exposed.Member
    @Exposed.DocString("missing attribute name")
    private String name;

    /** The object that didn't have {@code name} as an attribute. */
    @Exposed.Member
    @Exposed.DocString("object")
    private Object obj;

    /**
     * Constructor resembling {@code __new__}, specifying Python positional
     * arguments (only).
     *
     * @param type Python type of the exception
     * @param args arguments to fossilise in the exception instance
     */
    public PyAttributeError(PyType type, PyTuple args) { super(type, args); }

    // Special methods -----------------------------------------------

    private static final ArgParser INIT_PARSER = ArgParser
            .fromSignature("__init__", "*args", "name", "obj").kwdefaults(Py.None, Py.None);

    @Override
    void __init__(Object[] args, String[] kwds) {
        Object[] frame = INIT_PARSER.parse(args, kwds);
        // frame = [name, obj, *args]
        if (frame[2] instanceof PyTuple argsTuple) { // always is
            this.args = argsTuple;
        }
        // keywords: can't default directly to null in the parser
        Object name = frame[0], obj = frame[1];
        this.name = name == Py.None ? null : PyUnicode.asString(name);
        this.obj = obj == Py.None ? null : obj;
    }
}
