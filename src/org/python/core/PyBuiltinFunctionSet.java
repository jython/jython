// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A helper class for faster implementations of commonly called methods.
 * <p>
 * Subclasses of PyBuiltinFunctionSet will implement some or all of the __call__
 * method with a switch on the index number.
 * 
 */
public class PyBuiltinFunctionSet extends PyBuiltinFunction {

    public static final Class exposed_as = PyBuiltinFunction.class;

    // used as an index into a big switch statement in the various derived
    // class's __call__() methods.
    protected int index;

    private PyObject doc = Py.None;
    
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
        super(new DefaultInfo(name, minargs, maxargs));
        this.index = index;
        if(doc != null) {
            this.doc = Py.newString(doc);
        }
    }

    public PyObject fastGetDoc() {
        return doc;
    }

    public boolean isMappingType() {
        return false;
    }

    public boolean isNumberType() {
        return false;
    }

    public boolean isSequenceType() {
        return false;
    }

    public PyObject fancyCall(PyObject[] args) {
        throw info.unexpectedCall(args.length, false);
    }

    public PyObject __call__(PyObject[] args) {
        int nargs = args.length;
        switch(nargs){
            case 0:
                return __call__();
            case 1:
                return __call__(args[0]);
            case 2:
                return __call__(args[0], args[1]);
            case 3:
                return __call__(args[0], args[1], args[2]);
            case 4:
                return __call__(args[0], args[1], args[2], args[3]);
            default:
                return fancyCall(args);
        }
    }

    public PyObject __call__(PyObject[] args, String[] kws) {
        if(kws.length != 0) {
            throw Py.TypeError(safeRepr()
                    + "(): this function takes no keyword arguments");
        }
        return __call__(args);
    }

    public PyObject __call__() {
        throw info.unexpectedCall(0, false);
    }

    public PyObject __call__(PyObject arg1) {
        throw info.unexpectedCall(1, false);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        throw info.unexpectedCall(2, false);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        throw info.unexpectedCall(3, false);
    }

    public PyObject __call__(PyObject arg1,
                             PyObject arg2,
                             PyObject arg3,
                             PyObject arg4) {
        throw info.unexpectedCall(4, false);
    }

    public PyBuiltinFunction bind(PyObject self) {
        throw Py.TypeError("Can't bind a builtin function");
    }
    
    public String toString(){ 
        return "<built-in function "+info.getName()+">";
    }
    
}
