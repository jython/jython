// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A helper class for faster implementations of commonly called methods.
 * <p>
 * Subclasses of PyBuiltinFunctionSet will implement some or all of the __call__
 * method with a switch on the index number.
 *
 */
public class PyBuiltinFunctionSet extends PyBuiltinFunctionNarrow {

    // used as an index into a big switch statement in the various derived
    // class's __call__() methods.
    protected final int index;

    /**
     * Creates a PyBuiltinFunctionSet that expects 1 argument.
     */
    public PyBuiltinFunctionSet(String name, int index){
        this(name, index, 1);
    }

    public PyBuiltinFunctionSet(String name, int index, int numargs){
        this(name, index, numargs, numargs);
    }

    public PyBuiltinFunctionSet(String name, int index, int minargs, int maxargs){
        this(name, index, minargs, maxargs, null);
    }

    // full-blown constructor, specifying everything
    public PyBuiltinFunctionSet(String name,
                                int index,
                                int minargs,
                                int maxargs,
                                String doc) {
        super(name, minargs, maxargs, doc);
        this.index = index;
    }

}
