// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm.util;

import org.python.core.PyObject;
import org.python.newcompiler.pyasm.ConstantStore;

public class SimpleConstantStore implements ConstantStore {

    private PyObject[] constants;
    private String[] names;
    private String[] varnames;
    private String[] freevars;
    private String[] cellvars;

    public SimpleConstantStore(PyObject[] constants, String[] names, String[] varnames, String[] freevars, String[] cellvars) {
        this.constants = constants;
        this.names = names;
        this.varnames = varnames;
        this.freevars = freevars;
        this.cellvars = cellvars;
    }

    public PyObject getConstant(int index) {
        return constants[index];
    }

    public String getName(int index) {
        return names[index];
    }

    public String getOuterName(int index) {
        if( index < cellvars.length) {
            return cellvars[index];
        } else {
            return freevars[index - cellvars.length];
        }
    }

    public String getVariableName(int index) {
        return varnames[index];
    }

}
