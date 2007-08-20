// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

import org.python.core.PyObject;

public interface ConstantStore {

    /* Name handling methods */

    /**
     * Get the name of a something the scope, given its index.
     * 
     * Used for everything but vaiables, for variables
     * {@link #getVariableName(int)} is used.
     * 
     * @param index The index of the name.
     * @return The name of the item.
     */
    public String getName(int index);

    /**
     * Get the name of a variable in the scope, given its index.
     * 
     * @param index The index of the name.
     * @return The name of the variable.
     */
    public String getVariableName(int index);

    /**
     * Get the name of a variable in an enclosing scope, given its index.
     * 
     * @param index The index of the name.
     * @return The name of the variable.
     */
    public String getOuterName(int index);

    /* Constants */
    
    /**
     * Get a constant from the constant pool of this code object.
     * 
     * @param index The index of the constant in the constant pool.
     * @return The {@link PyObject} that is the constant.
     */
    public PyObject getConstant(int index);

}
